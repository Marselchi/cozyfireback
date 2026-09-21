"use client";

import { CharacterHeader } from "./character-header";
import { StatsGrid } from "./stats-grid";
import { SkillsList } from "./skills-list";
import { OriginWarningBanner } from "./origin-warning-banner";
import { Separator } from "@/components/ui/separator";
import { Character, StatBlock, Skill } from "@/types/account-char";
import { skillModifier } from "@/lib/account-char/derive/dnd5e";
import { useCharacter } from "@/hooks/account-char/use-character";
import { useUpdateCharacter } from "@/hooks/account-char/use-update-character";
import { useCharacterSelf } from "@/hooks/account-char/use-character-self";
import CustomLink from "../no-prefetch-link";
import { Button } from "../ui/button";
import { ImportDialog } from "./import-character-flow/import-dialog";
import { DropdownMenuItem } from "../ui/dropdown-menu";

interface Props {
  roomId: string;
  characterId?: string;
}

function recomputeSkillModifiers(
  skills: Skill[],
  stats: StatBlock[],
  level: number,
  system: string,
): Skill[] {
  if (system !== "DND_5E") return skills;
  const scoreByKey: Record<string, number> = {};
  for (const s of stats) scoreByKey[s.key] = s.score;
  return skills.map((sk) => ({
    ...sk,
    modifier: skillModifier(
      scoreByKey[sk.statKey] ?? 10,
      sk.proficiency,
      level,
    ),
  }));
}

function useResolvedCharacter(roomId: string, characterId?: string) {
  const bySelf = useCharacterSelf(roomId, { enabled: !characterId });
  const byId = useCharacter(roomId, characterId as string, {
    enabled: !!characterId,
  });

  return characterId ? byId : bySelf;
}

export function CharacterSheet({ roomId, characterId }: Readonly<Props>) {
  const {
    data: character,
    error,
    isPending,
  } = useResolvedCharacter(roomId, characterId);

  const resolvedId = characterId ?? character?.id;

  const {
    mutate: update,
    isPending: isSaving,
    error: saveError,
  } = useUpdateCharacter(roomId, resolvedId, !characterId);

  if (isPending) {
    return (
      <div className="flex items-center justify-center py-24 text-muted-foreground text-sm">
        Грузим персонажа...
      </div>
    );
  }

  if (error || !character) {
    console.log(error.status, error.statusText, error.message);
    return (
      <>
        <div
          role="alert"
          className="rounded-lg border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive"
        >
          {error?.status === 404
            ? "Персонажа не нашли. Возможно его еще не создали?"
            : error.message}
        </div>
        <div className="items-center justify-center flex mt-2">
          <Button asChild>
            <CustomLink href="./create">Создать персонажа</CustomLink>
          </Button>
        </div>
      </>
    );
  }

  function patch(partial: Partial<Character>) {
    update({ patch: { ...partial, version: character!.version } });
  }

  function handleHeaderSave(
    fields: Partial<Pick<Character, "name" | "level" | "class" | "race">>,
  ) {
    // If level changed and dnd5e, recompute skill modifiers
    const newLevel = fields.level ?? character!.level;
    const updatedSkills = recomputeSkillModifiers(
      character!.skills,
      character!.stats,
      newLevel,
      character!.system,
    );
    patch({ ...fields, skills: updatedSkills });
  }

  function handleStatsSave(stats: StatBlock[]) {
    const updatedSkills = recomputeSkillModifiers(
      character!.skills,
      stats,
      character!.level,
      character!.system,
    );
    patch({ stats, skills: updatedSkills });
  }

  function handleSkillsSave(skills: Skill[]) {
    patch({ skills });
  }

  return (
    <article
      className="space-y-6"
      aria-label={`Character sheet for ${character.name}`}
    >
      <OriginWarningBanner origin={character.origin.toString()} />

      <CharacterHeader
        character={character}
        onSave={handleHeaderSave}
        isSaving={isSaving}
      />

      <Separator />

      <StatsGrid
        stats={character.stats}
        system={character.system}
        onSave={handleStatsSave}
        isSaving={isSaving}
      />

      <Separator />

      <SkillsList
        skills={character.skills}
        stats={character.stats}
        system={character.system}
        onSave={handleSkillsSave}
        isSaving={isSaving}
      />

      <ImportDialog
        roomId={roomId}
        characterId={character.id}
        isSelf={true}
        trigger={
          <Button variant="outline" size="sm">
            Перезаписать из файла
          </Button>
        }
      />

      {saveError && (
        <p role="alert" className="text-sm text-destructive mt-2">
          {saveError.message.startsWith("CONFLICT")
            ? "Видимо вы обновили персонажа на другом устройстве, обновите страницу чтобы получить обновленного персонажа."
            : saveError.message}
        </p>
      )}
    </article>
  );
}
