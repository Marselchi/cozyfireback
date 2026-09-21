"use client";

import { LogOut, Edit, ArrowLeftRight, UserRoundCogIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { ChangeNameModal } from "./change-name-modal";
import { ImportExportDialog } from "./import-export-dialog";

interface UserProfileMenuProps {
  userName: string;
  onNameChange: (newName: string) => void;
}

export function UserProfileMenu({
  userName,
  onNameChange,
}: UserProfileMenuProps) {
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [isImportExportOpen, setIsImportExportOpen] = useState(false);
  const params = useParams<{ roomName: string }>();
  const router = useRouter();

  const initials = userName
    ? userName
        .split(" ")
        .map((n) => n[0])
        .join("")
        .toUpperCase()
        .slice(0, 2)
    : "Пользователь";

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" className="flex items-center gap-2 px-2">
            <Avatar className="h-8 w-8">
              <AvatarFallback className="bg-primary text-primary-foreground text-xs">
                {initials}
              </AvatarFallback>
            </Avatar>
            <span className="hidden text-sm font-medium md:inline-block">
              {userName}
            </span>
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-56">
          <DropdownMenuLabel>Профиль</DropdownMenuLabel>
          <DropdownMenuSeparator />
          <DropdownMenuItem onClick={() => setIsEditDialogOpen(true)}>
            <Edit className="mr-2 h-4 w-4" />
            Сменить имя
          </DropdownMenuItem>
          {/* <DropdownMenuItem onClick={() => setIsImportExportOpen(true)}>
            <ArrowLeftRight className="mr-2 h-4 w-4" />
            Импорт/Экспорт
          </DropdownMenuItem> */}
          <DropdownMenuItem
            onClick={() =>
              router.push(`/rooms/${params.roomName}/account-char/self`)
            }
          >
            <UserRoundCogIcon className="mr-2 h-4 w-4" />
            Ваш аккаунт
          </DropdownMenuItem>
          <DropdownMenuItem onClick={() => router.push("/")}>
            <LogOut className="mr-2 h-4 w-4" />К комнатам
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>

      <ChangeNameModal
        isOpen={isEditDialogOpen}
        onClose={setIsEditDialogOpen}
        currentName={userName}
        onNameUpdate={onNameChange}
      />

      <ImportExportDialog
        open={isImportExportOpen}
        onOpenChange={setIsImportExportOpen}
      />
    </>
  );
}
