/**
 * Извлекает ID из параметра URL формата "id-slug"
 * @param param - строка вида "123-my-room"
 * @returns ID как строку
 */
export function extractIdFromSlug(param: string | undefined): string {
  if (!param) {
    throw new Error("Parameter is undefined");
  }

  const id = param.split("-")[0];

  if (!id) {
    throw new Error("Invalid slug format: missing ID");
  }

  return id;
}
