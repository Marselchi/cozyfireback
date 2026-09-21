'use client'

import { toast as sonnerToast } from 'sonner'
import { CheckCircle2, XCircle, Info, Loader2 } from 'lucide-react'

type ToastType = 'success' | 'error' | 'info' | 'promise'

interface ToastOptions {
  title: string
  description?: string
  type: ToastType
  promise?: Promise<unknown>
  loading?: string
  success?: string
  error?: string
}

export function showToast({ title, description, type, promise, loading, success, error }: ToastOptions) {
  const content = (
    <div className="flex flex-col gap-1">
      <div className="font-semibold">{title}</div>
      {description && <div className="text-sm text-muted-foreground">{description}</div>}
    </div>
  )

  switch (type) {
    case 'success':
      return sonnerToast.success(content, {
        icon: <CheckCircle2 className="size-5 text-success" />,
        className: '!border-l-4 !border-l-success !bg-card',
        classNames: {
          title: '!text-success-foreground',
          description: '!text-muted-foreground',
        },
      })
    case 'error':
      return sonnerToast.error(content, {
        icon: <XCircle className="size-5 text-destructive" />,
        className: '!border-l-4 !border-l-destructive !bg-card',
        classNames: {
          title: '!text-destructive-foreground',
          description: '!text-muted-foreground',
        },
      })
    case 'info':
      return sonnerToast.info(content, {
        icon: <Info className="size-5 text-accent" />,
        className: '!border-l-4 !border-l-accent !bg-card',
        classNames: {
          title: '!text-accent-foreground',
          description: '!text-muted-foreground',
        },
      })
    case 'promise':
      if (!promise) {
        console.error('Promise type requires a promise to be provided')
        return
      }
      return sonnerToast.promise(promise, {
        loading: (
          <div className="flex items-center gap-3">
            <Loader2 className="size-5 animate-spin text-muted-foreground" />
            <div className="flex flex-col gap-1">
              <div className="font-semibold">{loading || title}</div>
              {description && <div className="text-sm text-muted-foreground">{description}</div>}
            </div>
          </div>
        ),
        success: (
          <div className="flex items-center gap-3">
            <div className="flex flex-col gap-1">
              <div className="font-semibold text-foreground">{success || 'Success'}</div>
              {description && <div className="text-sm text-muted-foreground">{description}</div>}
            </div>
          </div>
        ),
        error: (
          <div className="flex items-center gap-3">
            <XCircle className="size-5 text-destructive" />
            <div className="flex flex-col gap-1">
              <div className="font-semibold text-foreground">{error || 'Error'}</div>
              {description && <div className="text-sm text-muted-foreground">{description}</div>}
            </div>
          </div>
        ),
        className: '!border-l-4 !border-l-border !bg-card',
        classNames: {
          success: '!border-l-success',
          error: '!border-l-destructive',
        },
      })
    default:
      return sonnerToast(content)
  }
}
