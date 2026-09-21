import { connection, NextRequest, NextResponse } from "next/server";
import { getLoreFullSearch } from "@/server/lore/lore";

export async function GET(req: NextRequest) {
  await connection();
  try {
    const { searchParams } = new URL(req.url);
    const roomName = searchParams.get("roomName");
    const query = searchParams.get("q");
    const offset = searchParams.get("offset");
    const size = searchParams.get("limit");
    if (!roomName) {
      return NextResponse.json(
        { error: "roomName is required" },
        { status: 400 },
      );
    }
    const data = await getLoreFullSearch({
      roomName,
      filter: {
        offset,
        query,
        size,
      },
    });

    return NextResponse.json(data);
  } catch (e) {
    console.error("API ERROR:", e);
    return NextResponse.json({ content: [] }, { status: 500 });
  }
}
