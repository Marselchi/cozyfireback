"use client";

import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Users } from "lucide-react";
import CustomLink from "../no-prefetch-link";

interface RoomCardProps {
  room: {
    id: string;
    name: string;
    display_name: string;
    description: string;
    player_count: number;
    author: string;
  };
}

export default function RoomCard({ room }: Readonly<RoomCardProps>) {
  return (
    <CustomLink
      href={`/rooms/${room.id}-${room.name}`}
      className="block transition-transform hover:scale-[1.02]"
    >
      <Card className="h-full hover:shadow-md transition-shadow">
        <CardHeader>
          <CardTitle>{room.display_name}</CardTitle>
          <CardDescription className="flex items-center justify-between">
            <span>от {room.author}</span>
            <Badge variant="outline" className="flex items-center gap-1">
              <Users className="h-3 w-3" />
              <span>{room.player_count}</span>
            </Badge>
          </CardDescription>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground line-clamp-3">
            {room.description}
          </p>
        </CardContent>
        <CardFooter className="text-xs text-muted-foreground">
          Нажмите чтобы перейти к комнате
        </CardFooter>
      </Card>
    </CustomLink>
  );
}
