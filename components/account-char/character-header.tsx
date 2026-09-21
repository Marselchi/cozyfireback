"use client";

import { useState } from "react";
import { PencilIcon, CheckIcon, XIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Character } from "@/types/account-char";

interface Props {
  character: Character;
  onSave: (
    patch: Partial<Pick<Character, "name" | "level" | "class" | "race">>,
  ) => void;
  isSaving?: boolean;
}

export function CharacterHeader({
  character,
  onSave,
  isSaving,
}: Readonly<Props>) {
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState(character.name);
  const [level, setLevel] = useState(String(character.level));
  const [cls, setCls] = useState(character.class);
  const [race, setRace] = useState(character.race);

  function handleSave() {
    const parsed = Number.parseInt(level, 10);
    onSave({
      name: name.trim() || character.name,
      level: Number.isNaN(parsed) ? character.level : Math.max(1, parsed),
      class: cls,
      race,
    });
    setEditing(false);
  }

  function handleCancel() {
    setName(character.name);
    setLevel(String(character.level));
    setCls(character.class);
    setRace(character.race);
    setEditing(false);
  }

  if (!editing) {
    return (
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-1">
          <h1 className="text-3xl font-bold tracking-tight text-foreground text-balance">
            {character.name}
          </h1>
          <div className="flex flex-wrap items-center gap-2 text-muted-foreground text-sm">
            {character.race && <span>{character.race}</span>}
            {character.race && character.class && <span>&middot;</span>}
            {character.class && <span>{character.class}</span>}
            <Badge variant="secondary" className="pointer-events-none">
              Уровень {character.level}
            </Badge>
          </div>
        </div>
        <Button
          variant="outline"
          size="sm"
          onClick={() => setEditing(true)}
          aria-label="Edit character header"
        >
          <PencilIcon className="h-4 w-4" />
          <span className="sr-only">Редактировать</span>
        </Button>
      </div>
    );
  }

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        handleSave();
      }}
      className="space-y-4"
    >
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <div className="space-y-1.5">
          <Label htmlFor="header-name">Имя</Label>
          <Input
            id="header-name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            autoFocus
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="header-level">Уровень</Label>
          <Input
            id="header-level"
            type="number"
            min={1}
            max={20}
            value={level}
            onChange={(e) => setLevel(e.target.value)}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="header-class">Класс</Label>
          <Input
            id="header-class"
            value={cls}
            onChange={(e) => setCls(e.target.value)}
            placeholder="Плут"
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="header-race">Раса</Label>
          <Input
            id="header-race"
            value={race}
            onChange={(e) => setRace(e.target.value)}
            placeholder="Эльф"
          />
        </div>
      </div>

      <div className="flex gap-2">
        <Button type="submit" size="sm" disabled={isSaving}>
          <CheckIcon className="h-4 w-4" />
          Сохранить
        </Button>
        <Button type="button" variant="ghost" size="sm" onClick={handleCancel}>
          <XIcon className="h-4 w-4" />
          Отменить
        </Button>
      </div>
    </form>
  );
}
