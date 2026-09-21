"use client";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Loader2, Plus } from "lucide-react";
import AuthCodeItem from "@/components/admin/auth-code-item";
import { useActionState } from "react";
import { useParams } from "next/navigation";
import { createInvitation } from "@/server/invitation/invitation";
import { useRoomId } from "@/lib/room-utils";

interface AuthCode {
  id: string;
  code: string;
}

type AuthCodeListState = {
  error?: string;
  codes: AuthCode[];
};

interface AuthCodeListProps {
  initialCodes: AuthCode[];
}

export default function AuthCodeList({
  initialCodes,
}: Readonly<AuthCodeListProps>) {
  const roomName = useRoomId();
  const [state, formAction, isPending] = useActionState<
    AuthCodeListState,
    FormData
  >((prevState, formData) => createInvitation(prevState, formData, roomName), {
    codes: initialCodes,
  });

  const currentCodes = state?.codes || initialCodes;
  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="text-xl font-semibold">Список кодов</CardTitle>
          <form action={formAction}>
            <Button
              type="submit"
              disabled={isPending}
              className="flex items-center gap-2"
            >
              {isPending ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Создаю...
                </>
              ) : (
                <>
                  <Plus className="h-4 w-4" />
                  Создать код
                </>
              )}
            </Button>
          </form>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {currentCodes.length === 0 ? (
          <p className="text-muted-foreground text-center py-8">
            Нет созданных кодов
          </p>
        ) : (
          currentCodes.map((code) => <AuthCodeItem key={code.id} code={code} />)
        )}
      </CardContent>
    </Card>
  );
}
