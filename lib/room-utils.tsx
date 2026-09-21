import { useParams } from "next/navigation";
import { extractIdFromSlug } from "./server-room-utils";

/**
 * Хук для получения ID из текущего роута
 */
export function useRoomId(): string {
  const params = useParams<{ roomName: string }>();
  return extractIdFromSlug(params?.roomName);
}
