import { CharacterDisplay } from "./character-display"
import { Character, DEFAULT_STATUS_FIELDS } from "@/types/characters"
import type { Role } from "@/types/editor-layout"
import { getCharacterByIdUser } from "@/server/characters/characters"
import { notFound } from "next/navigation"
import type { Excerpt } from "@/types/lore"

interface CharacterContentProps {
  characterId: number
  roomName: string
  roles: Role[]
}

interface CharacterFetchResult {
  character: Character
  isAuthor: boolean
  excerpts: Excerpt[]
  questionCount: number
}

// Server component that fetches data and passes to client component
export async function CharacterContent({ characterId, roomName, roles }: CharacterContentProps) {
  const { character, isAuthor, excerpts, questionCount } = await fetchCharacter(characterId, roomName)

  return <CharacterDisplay initialData={character} characterId={characterId} roomName={roomName} roles={roles} isAuthor={isAuthor} excerpts={excerpts} questionCount={questionCount} />
}

async function fetchCharacter(characterId: number, roomName: string): Promise<CharacterFetchResult> {
  // In create mode (id=0), return empty character
  if (characterId === 0) {
    return { character: createEmptyCharacter(characterId), isAuthor: true, excerpts: [], questionCount: 0 }
  }

  const result = await getCharacterByIdUser(characterId, roomName)

  if (!result) {
    notFound()
  }

  const character: Character = {
    id: result.id,
    name: result.name,
    description: result.description,
    imageUrl: null,
    statusFields: parseStatusString(result.status),
    textBlock: {
      id: "content",
      type: "text",
      label: "",
      content: result.content,
    },
    optionalStatusBlocks: [],
    enabledOptionalStatusBlocks: [],
    layoutMode: "top",
    selectedRolesId: result.roles.map(r => r.id),
  }

  return { 
    character, 
    isAuthor: result.isAuthor,
    excerpts: result.excerpts || [],
    questionCount: result.questionCount || 0,
  }
}

function parseStatusString(statusString: string) {
  if (!statusString) {
    return DEFAULT_STATUS_FIELDS.map(f => ({ ...f, value: "" }))
  }

  const statusMap = new Map<string, string>()
  
  statusString.split(';').forEach(block => {
    const colonIndex = block.indexOf(':')
    if (colonIndex !== -1) {
      const name = block.substring(0, colonIndex).trim()
      const value = block.substring(colonIndex + 1).trim()
      statusMap.set(name, value)
    }
  })

  const statusFields = DEFAULT_STATUS_FIELDS.map(f => ({
    ...f,
    value: statusMap.get(f.label.toLowerCase()) || "",
  }))

  for (const [name, value] of statusMap.entries()) {
    const exists = DEFAULT_STATUS_FIELDS.some(f => f.id === name || f.label.toLowerCase() === name.toLowerCase())
    if (!exists) {
      statusFields.push({
        id: name.toLowerCase(),
        label: name.charAt(0).toUpperCase() + name.slice(1),
        value,
      })
    }
  }

  return statusFields
}

function createEmptyCharacter(characterId: number): Character {
  return {
    id: characterId,
    name: "",
    description: "",
    imageUrl: null,
    statusFields: DEFAULT_STATUS_FIELDS.map(f => ({ ...f, value: "" })),
    textBlock: {
      id: "content",
      type: "text",
      label: "",
      content: "",
    },
    optionalStatusBlocks: [],
    enabledOptionalStatusBlocks: [],
    layoutMode: "top",
    selectedRolesId: [],
  }
}
