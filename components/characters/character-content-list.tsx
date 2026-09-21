import CharacterList from "@/components/characters/character-list";
import { getPaginatedCharacters } from "@/server/characters/characters";

export default async function CharacterContent({
  roomName,
  searchParams,
}: Readonly<{
  roomName: string;
  searchParams: { search?: string; admin?: string; page?: string };
}>) {
  const pageFromUrl = Number(searchParams.page ?? "1");
  const page = pageFromUrl - 1;
  const size = 6;
  const sort = ['character_id,desc'];

  const createdByRoomCreator =
    searchParams.admin === "admin"
      ? true
      : searchParams.admin === "non-admin"
        ? false
        : null;

  const filter = {
    search: searchParams.search ?? null,
    createdByRoomCreator,
  };

  const { content, page: pageMeta } = await getPaginatedCharacters(
    roomName,
    filter,
    { page, size, sort }
  );

  return <CharacterList initialCharacters={content} page={pageMeta} />;
}
