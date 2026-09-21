"use client"

import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Trash2} from "lucide-react"
import { deleteAuthCode } from "@/server/invitation/actions"
import { useState } from "react"

interface AuthCodeItemProps {
  code: {
    id: string
    code: string
  }
}

export default function AuthCodeItem({ code }: Readonly<AuthCodeItemProps>) {
  const [isCopied, setIsCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(code.code);
      setIsCopied(true);
      setTimeout(() => setIsCopied(false), 2000);
    } catch (error) {
      console.error('Ошибка копирования:', error);
    }
  };

  return (
    <Card className="w-full bg-muted">
      <CardContent className="p-4">
        <div className="flex items-center justify-between">
          <div className="flex-1 space-y-2">
            <div className="flex items-center gap-3">
              <code
                className={`text-lg font-mono font-bold px-2 py-1 rounded cursor-pointer transition-colors
                  ${isCopied ? 'bg-green-100 text-green-600' : 'bg-accent'}
                `}
                onClick={handleCopy}
              >
                {code.code}{' '}
                {isCopied && (
                  <span className="text-xs font-normal ml-2 text-green-600">Скопировано!</span>
                )}
              </code>
            </div>
          </div>

          <form action={deleteAuthCode.bind(null, code.id)}>
            <Button type="submit" variant="outline" size="sm" className="text-destructive hover:text-destructive">
              <Trash2 className="h-4 w-4" />
              Удалить
            </Button>
          </form>
        </div>
      </CardContent>
    </Card>
  )
}
