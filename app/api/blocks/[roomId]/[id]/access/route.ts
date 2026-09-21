import { connection, NextResponse } from "next/server";
import { getAccessList } from "@/server/blocks/actions";

type RouteContext = { params: Promise<{ roomId: string; id: string }> };

export async function GET(_request: Request, { params }: RouteContext) {
  await connection();
  const { roomId, id } = await params;
  const [error, records] = await getAccessList(roomId, id);

  if (error) {
    return NextResponse.json(
      { error: error.message },
      { status: error.status },
    );
  }

  return NextResponse.json(records);
}
