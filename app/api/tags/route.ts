import { connection, NextRequest, NextResponse } from "next/server";
import { getAllTags } from "@/server/tags/tags";

export async function GET(req: NextRequest) {
  await connection();
  try {
    const { searchParams } = new URL(req.url);
    const roomName = searchParams.get("roomName");

    if (!roomName) {
      return NextResponse.json(
        { error: "roomName is required" },
        { status: 400 },
      );
    }

    const tags = await getAllTags(roomName);
    return NextResponse.json(tags);
  } catch (e) {
    console.error("API ERROR:", e);
    return NextResponse.json([], { status: 500 });
  }
}
