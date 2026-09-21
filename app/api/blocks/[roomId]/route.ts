import { connection, type NextRequest, NextResponse } from "next/server";
import { getBlocks } from "@/server/blocks/actions";
type RouteContext = { params: Promise<{ roomId: string }> };
export async function GET(request: NextRequest, { params }: RouteContext) {
  await connection();
  const { searchParams } = new URL(request.url);
  const { roomId } = await params;
  const [error, blocks] = await getBlocks({
    roomId,
    page: Number(searchParams.get("page") ?? 0),
    size: Number(searchParams.get("size") ?? 10),
    type: searchParams.get("type") as never,
    search: searchParams.get("search") ?? undefined,
    id: searchParams.get("id") ?? undefined,
  });
  if (error)
    return NextResponse.json(
      { error: error.message },
      { status: error.status },
    );
  return NextResponse.json(blocks);
}
