import { connection, NextRequest, NextResponse } from "next/server";
import { getLoreByIdUserInline } from "@/server/lore/lore";

type RouteContext = { params: Promise<{ id: string }> };

export async function GET(req: NextRequest, { params }: RouteContext) {
  await connection();
  try {
    const { id } = await params;
    const { searchParams } = new URL(req.url);
    const roomName = searchParams.get("roomName");

    if (!roomName) {
      return NextResponse.json(
        { error: "roomName is required" },
        { status: 400 },
      );
    }

    const idNum = Number(id);
    if (Number.isNaN(idNum)) {
      return NextResponse.json({ error: "Invalid id" }, { status: 400 });
    }

    const data = await getLoreByIdUserInline(idNum, roomName);

    if (!data) {
      return NextResponse.json({ error: "Not found" }, { status: 404 });
    }

    return NextResponse.json(data);
  } catch (e) {
    console.error("API ERROR:", e);
    return NextResponse.json(
      { error: "Failed to fetch lore" },
      { status: 500 },
    );
  }
}
