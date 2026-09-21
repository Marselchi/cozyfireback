'use client'

import * as React from 'react'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { AlertTriangle, Trash2, Plus, RefreshCw } from 'lucide-react'
import { AlertDialogHeader, AlertDialogFooter, AlertDialog, AlertDialogContent, AlertDialogTitle, AlertDialogDescription } from '../ui/alert-dialog'

type ConfirmButtonType = 'delete' | 'update' | 'create' | 'custom'

interface ConfirmDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  description: string
  doubleCheck?: boolean
  onAccept: () => void
  onCancel?: () => void
  confirmButtonType?: ConfirmButtonType
  customAcceptButtonText?: string
  customAcceptButtonVariant?: 'default' | 'destructive' | 'outline' | 'secondary' | 'ghost' | 'link'
  customCancelButtonText?: string
  closable?: boolean
}

export function ConfirmDialog({
  open,
  onOpenChange,
  title,
  description,
  doubleCheck = false,
  onAccept,
  onCancel,
  confirmButtonType = 'delete',
  customAcceptButtonText,
  customAcceptButtonVariant = 'default',
  customCancelButtonText,
  closable = true,
}: Readonly<ConfirmDialogProps>) {
  const [isFirstClick, setIsFirstClick] = React.useState(true)

  React.useEffect(() => {
    if (!open) {
      setIsFirstClick(true)
    }
  }, [open])

  const handleConfirm = () => {
    if (doubleCheck && isFirstClick) {
      setIsFirstClick(false)
      return
    }
    onAccept()
    onOpenChange(false)
  }

  const handleCancel = () => {
    onCancel?.()
    onOpenChange(false)
    setIsFirstClick(true)
  }

  const getAcceptButtonConfig = () => {
    if (confirmButtonType === 'custom') {
      return {
        text: customAcceptButtonText || 'Подтвердить',
        variant: customAcceptButtonVariant,
        icon: null,
      }
    }

    const configs: Record<
      Exclude<ConfirmButtonType, 'custom'>,
      { text: string; variant: 'default' | 'destructive'; icon: React.ReactNode }
    > = {
      delete: {
        text: 'Удалить',
        variant: 'destructive' as const,
        icon: <Trash2 className="size-4" />,
      },
      update: {
        text: 'Обновить',
        variant: 'default' as const,
        icon: <RefreshCw className="size-4" />,
      },
      create: {
        text: 'Создать',
        variant: 'default' as const,
        icon: <Plus className="size-4" />,
      },
    }

    return configs[confirmButtonType]
  }

  const buttonConfig = getAcceptButtonConfig()
  const buttonText = doubleCheck && !isFirstClick ? 'Вы уверены?' : buttonConfig.text

  return (
    <AlertDialog open={open} onOpenChange={onOpenChange} >
      <AlertDialogContent className="sm:max-w-lg">
        <AlertDialogHeader >
          <div className="flex items-center gap-3">
            {confirmButtonType === 'delete' && (
              <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-destructive/10">
                <AlertTriangle className="size-5 text-destructive" />
              </div>
            )}
            <AlertDialogTitle className="text-left">{title}</AlertDialogTitle>
          </div>
          <AlertDialogDescription className="text-left text-pretty">
            {description}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter className="gap-2 sm:gap-2">
          <Button variant="outline" onClick={handleCancel}>
            {customCancelButtonText ?? "Отмена"}
          </Button>
          <Button
            variant={buttonConfig.variant}
            onClick={handleConfirm}
            className="gap-2"
          >
            {buttonConfig.icon}
            {buttonText}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
