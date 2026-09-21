
import { CharacterListResponse, Character } from "@/types/character"
import { mockCharacters } from "./mock-characters"

const PAGE_SIZE = 6

const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

const charactersStore = [...mockCharacters]

export async function fetchCharacters(cursor: string | null = null, search = ""): Promise<CharacterListResponse> {
  await delay(800)

  let filteredCharacters = charactersStore
  if (search) {
    const searchLower = search.toLowerCase()
    filteredCharacters = charactersStore.filter(
      (char) =>
        char.name.toLowerCase().includes(searchLower) ||
        char.profession?.toLowerCase().includes(searchLower) ||
        char.description?.toLowerCase().includes(searchLower) ||
        char.affiliation?.toLowerCase().includes(searchLower) ||
        char.race?.toLowerCase().includes(searchLower),
    )
  }

  // Calculate pagination
  const startIndex = cursor ? Number.parseInt(cursor, 10) : 0
  const endIndex = startIndex + PAGE_SIZE
  const paginatedCharacters = filteredCharacters.slice(startIndex, endIndex)
  const hasMore = endIndex < filteredCharacters.length
  const nextCursor = hasMore ? endIndex.toString() : null

  return {
    characters: paginatedCharacters,
    nextCursor,
    hasMore,
  }
}

export async function fetchCharacterById(id: string): Promise<Character | null> {
  await delay(500)
  return charactersStore.find((char) => char.id === id) || null
}

export async function createCharacter(data: Omit<Character, "id">): Promise<Character> {
  await delay(500)

  const newCharacter: Character = {
    id: (charactersStore.length + 1).toString(),
    name: data.name,
    profession: data.profession || null,
    appearDate: data.appearDate || null,
    status: data.status || null,
    affiliation: data.affiliation || null,
    description: data.description || null,
    race: data.race || null,
    age: data.age || null,
    importantNotes: data.importantNotes || null,
    pictureUrl: data.pictureUrl || null,
  }

  charactersStore.push(newCharacter)
  return newCharacter
}

export async function updateCharacter(id: string, data: Partial<Character>): Promise<Character | null> {
  await delay(500)

  const index = charactersStore.findIndex((char) => char.id === id)
  if (index === -1) return null

  charactersStore[index] = {
    ...charactersStore[index],
    ...data,
    id: charactersStore[index].id,
  }

  return charactersStore[index]
}

export async function deleteCharacter(id: string): Promise<boolean> {
  await delay(500)

  const index = charactersStore.findIndex((char) => char.id === id)
  if (index === -1) return false

  charactersStore.splice(index, 1)
  return true
}
