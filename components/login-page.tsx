"use client";

import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useRouter, useSearchParams } from "next/navigation";

export default function LoginClient() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const callbackUrl = searchParams.get("callbackUrl") || "/";
  const error = searchParams.get("error"); // например invalid_credentials

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-950 p-4">
      <Card className="w-full max-w-md border-gray-800 bg-gray-900 text-gray-100">
        <CardHeader className="space-y-1 pb-0">
          <CardTitle className="text-2xl font-bold">Войти</CardTitle>
        </CardHeader>

        <CardContent>
          {error && (
            <Alert variant="destructive" className="mb-4">
              <AlertDescription>
                {error === "invalid_credentials"
                  ? "Неверный логин или пароль"
                  : "Ошибка авторизации"}
              </AlertDescription>
            </Alert>
          )}

          <form action="/auth/login" method="POST" className="space-y-4">
            <input type="hidden" name="callbackUrl" value={callbackUrl} />

            <div className="space-y-2">
              <Label htmlFor="username">Логин</Label>
              <Input
                id="username"
                name="username"
                type="text"
                autoComplete="username"
                placeholder="Введите логин..."
                required
                className="border-gray-700 bg-gray-800 text-gray-100"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="password">Пароль</Label>
              <Input
                id="password"
                name="password"
                type="password"
                required
                className="border-gray-700 bg-gray-800 text-gray-100"
              />
            </div>

            <Button type="submit" className="w-full bg-primary hover:bg-primary/90">
              Войти
            </Button>
          </form>
        </CardContent>

        <CardFooter className="flex justify-center gap-1 text-sm text-gray-400">
          <span>Нет аккаунта?</span>
          <Button
            variant="link"
            className="p-0 text-primary hover:text-primary/90"
            onClick={() => router.push("/signup")}
          >
            Зарегистрироваться
          </Button>
        </CardFooter>
      </Card>
    </div>
  );
}
