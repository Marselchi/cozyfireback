"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Card, CardContent } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { UserMinus, Plus, X, Edit2, Check, AlertCircle } from "lucide-react";
import {
  removeUserRole,
  assignUserRole,
  replaceUserRole,
} from "@/server/user/roles";
import { useParams } from "next/navigation";
import { RoomAccountResponse } from "@/types/manage";
import { IdName } from "@/types/springTypes";
import { useRoomId } from "@/lib/room-utils";

interface UserItemProps {
  user: RoomAccountResponse;
  availableRoles: IdName[];
}

export default function UserItem({
  user,
  availableRoles,
}: Readonly<UserItemProps>) {
  const [isAssigningRole, setIsAssigningRole] = useState(false);
  const [selectedNewRoleId, setSelectedNewRoleId] = useState<number | null>(
    null,
  );
  const [editingRole, setEditingRole] = useState<IdName | null>(null);
  const [selectedEditRoleId, setSelectedEditRoleId] = useState<number | null>(
    null,
  );
  const roomName = useRoomId();

  // Assigned role names for filtering
  const assignedRoleNames = new Set(user.roles.map((r) => r.name));
  const unassignedRoles = availableRoles.filter(
    (role) => !assignedRoleNames.has(role.name),
  );

  const getAvailableRolesForEdit = (currentRoleName: string) => {
    const otherAssignedNames = new Set(
      user.roles.map((r) => r.name).filter((name) => name !== currentRoleName),
    );
    return availableRoles.filter((role) => !otherAssignedNames.has(role.name));
  };

  return (
    <Card className="bg-additional hover:shadow-md transition-shadow">
      <CardContent className="p-6">
        <div className="flex items-start justify-between">
          <div className="flex items-start gap-4 flex-1">
            <Avatar className="h-12 w-12">
              <AvatarFallback className="text-lg">
                {user.username
                  .split(" ")
                  .map((n) => n[0])
                  .join("")
                  .toUpperCase()}
              </AvatarFallback>
            </Avatar>

            <div className="flex-1 space-y-3">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <h3 className="font-semibold text-lg">{user.username}</h3>
                  <span className="text-sm text-muted-foreground">
                    @{user.profileName}
                  </span>
                </div>
              </div>

              {/* Roles Section */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <h4 className="text-sm font-medium text-muted-foreground">
                    Роли ({user.roles.length}/10)
                  </h4>
                  {user.roles.length < 10 &&
                    unassignedRoles.length > 0 &&
                    !isAssigningRole && (
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setIsAssigningRole(true)}
                        className="h-6 px-2 text-xs"
                      >
                        <Plus className="h-3 w-3 mr-1" />
                        Добавить роль
                      </Button>
                    )}
                </div>

                <div className="flex flex-wrap gap-2">
                  {user.roles.map((role) => (
                    <div key={role.id} className="flex items-center">
                      {editingRole?.id === role.id ? (
                        <div className="flex items-center gap-2">
                          <Select
                            value={
                              selectedEditRoleId
                                ? String(selectedEditRoleId)
                                : ""
                            }
                            onValueChange={(value) =>
                              setSelectedEditRoleId(Number(value))
                            }
                          >
                            <SelectTrigger className="h-7 w-32 text-xs">
                              <SelectValue placeholder="Выбрать роль" />
                            </SelectTrigger>
                            <SelectContent>
                              {getAvailableRolesForEdit(role.name).map(
                                (availableRole) => (
                                  <SelectItem
                                    key={availableRole.id}
                                    value={String(availableRole.id)}
                                    className="text-xs"
                                  >
                                    {availableRole.name}
                                  </SelectItem>
                                ),
                              )}
                            </SelectContent>
                          </Select>
                          <form action={replaceUserRole}>
                            <input
                              type="hidden"
                              name="userId"
                              value={user.id.toString()}
                            />
                            <input
                              type="hidden"
                              name="oldRoleId"
                              value={role.id.toString()}
                            />
                            <input
                              type="hidden"
                              name="newRoleId"
                              value={selectedEditRoleId?.toString() || ""}
                            />
                            <input
                              type="hidden"
                              name="roomName"
                              value={roomName}
                            />
                            <Button
                              type="submit"
                              variant="ghost"
                              size="sm"
                              className="h-6 w-6 p-0"
                              disabled={
                                !selectedEditRoleId ||
                                selectedEditRoleId === role.id
                              }
                            >
                              <Check className="h-3 w-3" />
                            </Button>
                          </form>
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-6 w-6 p-0"
                            onClick={() => {
                              setEditingRole(null);
                              setSelectedEditRoleId(null);
                            }}
                          >
                            <X className="h-3 w-3" />
                          </Button>
                        </div>
                      ) : (
                        <Badge
                          variant="secondary"
                          className="flex items-center gap-1 pr-1 group hover:bg-muted"
                        >
                          <span>{role.name}</span>
                          <div className="flex items-center gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity">
                            <Button
                              variant="ghost"
                              size="sm"
                              className="h-4 w-4 p-0 hover:bg-transparent"
                              onClick={() => {
                                setEditingRole(role);
                                setSelectedEditRoleId(role.id);
                              }}
                            >
                              <Edit2 className="h-2.5 w-2.5" />
                            </Button>
                            <form action={removeUserRole}>
                              <input
                                type="hidden"
                                name="userId"
                                value={user.id.toString()}
                              />
                              <input
                                type="hidden"
                                name="roleId"
                                value={role.id.toString()}
                              />
                              <input
                                type="hidden"
                                name="roomName"
                                value={roomName}
                              />
                              <Button
                                type="submit"
                                variant="ghost"
                                size="sm"
                                className="h-4 w-4 p-0 hover:bg-transparent text-destructive"
                              >
                                <X className="h-2.5 w-2.5" />
                              </Button>
                            </form>
                          </div>
                        </Badge>
                      )}
                    </div>
                  ))}

                  {/* Assign Role Select */}
                  {isAssigningRole && (
                    <div className="flex items-center gap-2">
                      <Select
                        value={
                          selectedNewRoleId ? String(selectedNewRoleId) : ""
                        }
                        onValueChange={(value) =>
                          setSelectedNewRoleId(Number(value))
                        }
                      >
                        <SelectTrigger className="h-7 w-32 text-xs">
                          <SelectValue placeholder="Выбрать роль" />
                        </SelectTrigger>
                        <SelectContent>
                          {unassignedRoles.map((role) => (
                            <SelectItem
                              key={role.id}
                              value={String(role.id)}
                              className="text-xs"
                            >
                              {role.name}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <form action={assignUserRole}>
                        <input
                          type="hidden"
                          name="userId"
                          value={user.id.toString()}
                        />
                        <input
                          type="hidden"
                          name="roleId"
                          value={selectedNewRoleId?.toString() || ""}
                        />
                        <input type="hidden" name="roomName" value={roomName} />
                        <Button
                          type="submit"
                          variant="ghost"
                          size="sm"
                          className="h-6 w-6 p-0"
                          disabled={!selectedNewRoleId}
                        >
                          <Check className="h-3 w-3" />
                        </Button>
                      </form>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-6 w-6 p-0"
                        onClick={() => {
                          setIsAssigningRole(false);
                          setSelectedNewRoleId(null);
                        }}
                      >
                        <X className="h-3 w-3" />
                      </Button>
                    </div>
                  )}
                </div>

                {/* Status Messages */}
                {user.roles.length === 10 && (
                  <div className="flex items-center gap-1 text-xs text-amber-600">
                    <AlertCircle className="h-3 w-3" />
                    <span>Достигнут максимум ролей</span>
                  </div>
                )}

                {unassignedRoles.length === 0 && user.roles.length < 10 && (
                  <div className="flex items-center gap-1 text-xs text-muted-foreground">
                    <AlertCircle className="h-3 w-3" />
                    <span>Все доступные роли выделены</span>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Kick Button (placeholder) */}
          <form>
            <Button
              type="submit"
              variant="outline"
              size="sm"
              className="bg-background text-destructive hover:text-destructive ml-4"
            >
              <UserMinus className="h-4 w-4 mr-2" />
              Кикнуть
            </Button>
          </form>
        </div>
      </CardContent>
    </Card>
  );
}
