import { connection, NextResponse } from "next/server";
import { getSkills } from "@/server/blocks/actions";

export async function GET(request: Request) {
  await connection();
  const roomId = new URL(request.url).searchParams.get("roomId");
  if (!roomId) {
    return NextResponse.json({ error: "roomId is required" }, { status: 400 });
  }
  const [error, skills] = await getSkills(roomId);
  if (error)
    return NextResponse.json(
      { error: error.message },
      { status: error.status },
    );
  return NextResponse.json(skills ?? []);
}
