import { connection, NextRequest, NextResponse } from "next/server";
import { getPaginatedLore } from "@/server/lore/lore";
import type { ViewStatus } from "@/types/springTypes";

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

    const search = searchParams.get("search");
    const tag = searchParams.get("tag");
    const admin = searchParams.get("admin");
    const viewed = searchParams.get("viewed") as ViewStatus | null;
    const page = Number(searchParams.get("page") ?? "0");
    const size = Number(searchParams.get("size") ?? "6");

    const createdByRoomCreator =
      admin === "admin" ? true : admin === "non-admin" ? false : null;

    const filter = {
      title: search,
      tagNames: tag ? [tag] : null,
      createdByRoomCreator,
      status: viewed ?? null,
    };

    const data = await getPaginatedLore(roomName, filter, {
      page,
      size,
      sort: ["lore_id,desc"],
    });

    return NextResponse.json(data);
  } catch (e) {
    console.error("API ERROR:", e);
    return NextResponse.json({ content: [] }, { status: 500 });
  }
}
