export const characterKeys = {
  self: (roomId: string) => ["character-self", roomId] as const,
  byId: (characterId: string) => ["character", characterId] as const,
};
