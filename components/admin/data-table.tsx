"use client";

import { useState, useTransition } from "react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Pencil, Trash2, Plus, Loader2 } from "lucide-react";
import { ItemType } from "@/types/manage";
import { Error } from "@/server/roles/items";
import { useParams } from "next/navigation";
import { ConfirmDialog } from "../utils/confirm-dialog";
import { useRoomId } from "@/lib/room-utils";

interface DataItem {
  id: string;
  name: string;
}

interface DataTableProps {
  data: DataItem[];
  createAction: (
    type: ItemType,
    previousState: Error,
    formData: FormData,
  ) => Promise<{ error?: string; success?: boolean }>;
  editAction: (
    type: ItemType,
    previousState: Error,
    formData: FormData,
  ) => Promise<{ error?: string; success?: boolean }>;
  deleteAction: (
    type: ItemType,
    formData: FormData,
  ) => Promise<{ error?: string; success?: boolean }>;
  itemType: ItemType;
}

export function DataTable({
  data,
  createAction,
  editAction,
  deleteAction,
  itemType,
}: Readonly<DataTableProps>) {
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<DataItem | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [confirmDeleteOpen, setConfirmDeleteOpen] = useState(false);
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);
  const roomName = useRoomId();

  const handleAdd = async (formData: FormData) => {
    setError(null);
    formData.append("roomName", roomName);
    startTransition(async () => {
      const result = await createAction(
        itemType,
        {
          error: "",
          success: false,
        },
        formData,
      );
      if (result.error) {
        setError(result.error);
      } else {
        setIsAddOpen(false);
      }
    });
  };

  const handleEdit = async (formData: FormData) => {
    setError(null);
    startTransition(async () => {
      const result = await editAction(
        itemType,
        {
          error: "",
          success: false,
        },
        formData,
      );
      if (result.error) {
        setError(result.error);
      } else {
        setEditingItem(null);
      }
    });
  };

  const handleDelete = async (id: string) => {
    setError(null);
    setDeletingId(id);
    startTransition(async () => {
      const formData = new FormData();
      formData.append("id", id);
      formData.append("roomName", roomName);
      const result = await deleteAction(itemType, formData);
      if (result.error) {
        setError(result.error);
      }
      setDeletingId(null);
      setConfirmDeleteOpen(false);
    });
  };

  return (
    <div className="space-y-4 p-6">
      {/* Add button */}
      <div className="flex justify-between items-center">
        <p className="text-sm text-muted-foreground">{data.length} Всего</p>
        <Dialog open={isAddOpen} onOpenChange={setIsAddOpen}>
          <DialogTrigger asChild>
            <Button className="gap-2">
              <Plus className="h-4 w-4" />
              Добавить {itemType === "tag" ? "тег" : "роль"}
            </Button>
          </DialogTrigger>
          <DialogContent className="bg-card border-border">
            <DialogHeader>
              <DialogTitle className="text-foreground">
                Добавить новый {itemType === "tag" ? "тег" : "роль"}
              </DialogTitle>
              <DialogDescription className="text-muted-foreground">
                Введите название.
              </DialogDescription>
            </DialogHeader>
            <form action={handleAdd}>
              <div className="space-y-4 py-4">
                <div className="space-y-2">
                  <label
                    htmlFor="add-name"
                    className="text-sm font-medium text-foreground"
                  >
                    Название
                  </label>
                  <Input
                    id="add-name"
                    name="name"
                    placeholder={`Введите название`}
                    className="bg-secondary border-border"
                    autoFocus
                  />
                </div>
                {error && <p className="text-sm text-destructive">{error}</p>}
              </div>
              <DialogFooter>
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setIsAddOpen(false)}
                >
                  Отмена
                </Button>
                <Button type="submit" disabled={isPending}>
                  {isPending && (
                    <Loader2 className="h-4 w-4 animate-spin mr-2" />
                  )}
                  Создание
                </Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      {/* Table */}
      <div className="rounded-lg border border-border overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow className="bg-secondary/50 hover:bg-secondary/50 border-border">
              <TableHead className="w-32 text-foreground font-semibold">
                ID
              </TableHead>
              <TableHead className="text-foreground font-semibold">
                Название
              </TableHead>
              <TableHead className="w-32 text-right text-foreground font-semibold">
                Действия
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={3}
                  className="text-center py-8 text-muted-foreground"
                >
                  Не найдено элементов.
                </TableCell>
              </TableRow>
            ) : (
              data.map((item) => (
                <TableRow key={item.id} className="border-border">
                  <TableCell className="font-mono text-sm text-muted-foreground">
                    {item.id}
                  </TableCell>
                  <TableCell className="font-medium text-foreground">
                    {item.name}
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-1">
                      {/* Edit Dialog */}
                      <Dialog
                        open={editingItem?.id === item.id}
                        onOpenChange={(open) => !open && setEditingItem(null)}
                      >
                        <DialogTrigger asChild>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setEditingItem(item)}
                            className="hover:bg-secondary"
                          >
                            <Pencil className="h-4 w-4" />
                            <span className="sr-only">Редактировать</span>
                          </Button>
                        </DialogTrigger>
                        <DialogContent className="bg-card border-border">
                          <DialogHeader>
                            <DialogTitle className="text-foreground">
                              Изменить {itemType === "tag" ? "тег" : "роль"}
                            </DialogTitle>
                          </DialogHeader>
                          <form action={handleEdit}>
                            <input type="hidden" name="id" value={item.id} />
                            <input
                              type="hidden"
                              name="roomName"
                              value={roomName}
                            />
                            <div className="space-y-4 py-4">
                              <div className="space-y-2">
                                <label
                                  htmlFor={`edit-name-${item.id}`}
                                  className="text-sm font-medium text-foreground"
                                >
                                  Название
                                </label>
                                <Input
                                  id={`edit-name-${item.id}`}
                                  name="name"
                                  defaultValue={item.name}
                                  className="bg-secondary border-border"
                                  autoFocus
                                />
                              </div>
                              {error && (
                                <p className="text-sm text-destructive">
                                  {error}
                                </p>
                              )}
                            </div>
                            <DialogFooter>
                              <Button
                                type="button"
                                variant="outline"
                                onClick={() => setEditingItem(null)}
                              >
                                Отмена
                              </Button>
                              <Button type="submit" disabled={isPending}>
                                {isPending && (
                                  <Loader2 className="h-4 w-4 animate-spin mr-2" />
                                )}
                                Сохранить изменения
                              </Button>
                            </DialogFooter>
                          </form>
                        </DialogContent>
                      </Dialog>

                      {/* Delete Confirm Dialog */}
                      <ConfirmDialog
                        open={confirmDeleteOpen && deletingId === item.id}
                        onOpenChange={(open) => {
                          if (!open) {
                            setConfirmDeleteOpen(false);
                            setDeletingId(null);
                          }
                        }}
                        title="Удалить элемент?"
                        description={`Вы уверены, что хотите удалить "${item.name}"? Это действие нельзя отменить.`}
                        doubleCheck={true}
                        confirmButtonType="delete"
                        onAccept={() => handleDelete(item.id)}
                        onCancel={() => {
                          setConfirmDeleteOpen(false);
                          setDeletingId(null);
                        }}
                      />

                      {/* Delete Button */}
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => {
                          setDeletingId(item.id);
                          setConfirmDeleteOpen(true);
                        }}
                        disabled={deletingId === item.id}
                        className="hover:bg-destructive/10 hover:text-destructive"
                      >
                        {deletingId === item.id ? (
                          <Loader2 className="h-4 w-4 animate-spin" />
                        ) : (
                          <Trash2 className="h-4 w-4" />
                        )}
                        <span className="sr-only">Удалить</span>
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
