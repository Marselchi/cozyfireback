'use client'

import { Loader2 } from 'lucide-react'

interface LoadingPageProps {
  message?: string
  fullScreen?: boolean
}

export function LoadingPage({ message = 'Загрузка...', fullScreen = true }: Readonly<LoadingPageProps>) {
  return (
    <div
      className={`flex flex-col items-center justify-center gap-6 bg-background ${
        fullScreen ? 'fixed inset-0' : 'h-screen'
      }`}
    >
      <div className="relative">
        {/* Outer spinning ring */}
        <div className="absolute inset-0 animate-spin rounded-full border-4 border-border border-t-accent" style={{ width: '80px', height: '80px', animationDuration: '1.5s' }} />
        
        {/* Middle spinning ring */}
        <div className="absolute inset-0 m-2 animate-spin rounded-full border-4 border-border border-t-primary opacity-60" style={{ width: '64px', height: '64px', animationDuration: '2s', animationDirection: 'reverse' }} />
        
        {/* Inner icon */}
        <div className="flex items-center justify-center" style={{ width: '80px', height: '80px' }}>
          <Loader2 className="size-8 animate-spin text-foreground" style={{ animationDuration: '1s' }} />
        </div>
      </div>

      {/* Loading text with fade animation */}
      <div className="flex flex-col items-center gap-2 animate-pulse">
        <p className="text-lg font-semibold text-foreground">{message}</p>
      </div>
    </div>
  )
}
