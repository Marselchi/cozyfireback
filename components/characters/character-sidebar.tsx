import CharacterSearchFilter from "@/components/characters/character-search-filter";

export default async function CharacterSidebar({ roomName }: Readonly<{ roomName: string }>) {
  return <CharacterSearchFilter />;
}
