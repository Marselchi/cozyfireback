import { connection, NextResponse } from "next/server";
import { getBlockWrapped } from "@/server/blocks/actions";

type RouteContext = { params: Promise<{ id: string; roomId: string }> };

export async function GET(_request: Request, { params }: RouteContext) {
  await connection();
  const { roomId, id } = await params;
  console.log("ROUTE HANDLER");
  console.log(roomId);
  console.log(id);
  const [error, block] = await getBlockWrapped(roomId, id);

  if (error) {
    return NextResponse.json(
      { error: error.message },
      { status: error.status },
    );
  }

  return NextResponse.json(block);
}
