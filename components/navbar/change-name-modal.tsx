import { useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";
import { updateUserName } from "@/server/user/user";
import { useParams } from "next/navigation";
import { useRoomId } from "@/lib/room-utils";

interface ChangeNameModalProps {
  isOpen: boolean;
  onClose: (open: boolean) => void;
  currentName: string;
  onNameUpdate: (newName: string) => void;
}

export function ChangeNameModal({
  isOpen,
  onClose,
  currentName,
  onNameUpdate,
}: Readonly<ChangeNameModalProps>) {
  const [newName, setNewName] = useState(currentName);
  const [isLoading, setIsLoading] = useState(false);
  const roomName = useRoomId();
  const handleSave = async () => {
    if (!newName.trim()) {
      toast.error("Ошибка", {
        description: "Пустое имя нельзя",
      });
      return;
    }
    if (newName.trim() === currentName) {
      onClose(false);
      return;
    }
    setIsLoading(true);
    try {
      const result = await updateUserName(roomName, newName.trim());
      if (result.success) {
        onNameUpdate(newName.trim());
        toast.success("Успешно", {
          description: result.message,
        });
        onClose(false);
      } else {
        toast.error("Ошибка", {
          description: result.message,
        });
      }
    } catch (error) {
      toast.error("Ошибка", {
        description: "Что-то странное",
      });
    } finally {
      setIsLoading(false);
    }
  };

  const handleCancel = () => {
    setNewName(currentName);
    onClose(false);
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-106.25 z-50">
        <DialogHeader>
          <DialogTitle>Сменить ник</DialogTitle>
          <DialogDescription>
            Это будет отображаемым ником для данной комнаты
          </DialogDescription>
        </DialogHeader>
        <div className="grid gap-4 py-4">
          <div className="grid grid-cols-4 items-center gap-4">
            <Input
              id="name"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              className="col-span-4"
              placeholder="Введите ник"
              disabled={isLoading}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  handleSave();
                }
              }}
            />
          </div>
        </div>
        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            onClick={handleCancel}
            disabled={isLoading}
          >
            Отмена
          </Button>
          <Button type="button" onClick={handleSave} disabled={isLoading}>
            {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Сохранить
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
