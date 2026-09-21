'use client'

import * as React from 'react'
import { cn } from '@/lib/utils'
import { Badge } from '@/components/ui/badge'
import {
  Shield,
  EyeOff,
} from 'lucide-react'
import CustomLink from '../no-prefetch-link'

export interface CharacterStatusBlock {
  label: string
  value: string
}

export interface CharacterItemData {
  id: number
  name: string
  description: string
  date: string
  author: string
  byAdmin: boolean
  statusBlocks: CharacterStatusBlock[]
}

export interface CharacterItemOptions {
  href?: string
}

export interface CharacterItemProps {
  data: CharacterItemData
  options?: CharacterItemOptions
}

export function CharacterItem({ data, options }: CharacterItemProps) {
  const href = options?.href || `./characters/${data.id}`

  const statusIndicators = []

  if (data.byAdmin) {
    statusIndicators.push({
      key: 'byAdmin',
      icon: Shield,
      label: 'Создано DM',
      className: 'text-[hsl(var(--lore-admin))]',
    })
  }

  return (
    <CustomLink href={href} className="block group h-full">
      <article
        className={cn(
          'relative overflow-hidden rounded-(--radius) border bg-card text-card-foreground',
          'h-full flex flex-col',
          'transition-all duration-300 ease-out',
          'hover:shadow-lg hover:shadow-primary/5 hover:border-primary/30',
          'hover:-translate-y-0.5'
        )}
      >
        <div className="flex flex-col flex-1 p-5">
          {/* Flexible content area that grows to fill space */}
          <div className="space-y-4 grow">
            {/* Header section with status indicators */}
            <div className="flex items-start justify-between gap-3">
              <div className="flex-1 min-w-0 space-y-2">
                {/* Title */}
                <div className="flex items-start gap-2">
                  <h3
                    className={cn(
                      'font-semibold text-lg leading-tight text-balance transition-colors'
                    )}
                  >
                    {data.name}
                  </h3>
                </div>

                {/* Description */}
                <p className="text-sm text-muted-foreground leading-relaxed line-clamp-2 wrap-break-word">
                  {data.description}
                </p>
              </div>

              {/* Status indicators column */}
              {statusIndicators.length > 0 && (
                <div className="flex gap-1.5 shrink-0">
                  {statusIndicators.map(({ key, icon: Icon, label, className }) => (
                    <div
                      key={key}
                      className={cn(
                        'p-1.5 rounded-md bg-secondary/50 transition-all duration-200',
                        className
                      )}
                      title={label}
                    >
                      <Icon className="h-3.5 w-3.5" />
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Footer section - stays at the bottom */}
          <div className="pt-2 border-t border-input mt-auto">
            <div className="flex items-center justify-between gap-4">
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                {data.byAdmin && (
                  <Badge
                    variant="outline"
                    className="border-[hsl(var(--lore-admin))] text-[hsl(var(--lore-admin))] px-2 py-0.5"
                  >
                    <Shield className="h-3 w-3 mr-1" />
                    DM
                  </Badge>
                )}
                <span className="font-medium">{data.author}</span>
              </div>

              <div className="flex items-center gap-3 text-xs text-muted-foreground">
                <span>{data.date}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Hover effect overlay */}
        <div
          className={cn(
            'absolute inset-0 bg-linear-to-r from-accent/0 via-accent/0 to-accent/0',
            'opacity-0 group-hover:opacity-[0.03] transition-opacity duration-300',
            'pointer-events-none'
          )}
        />
      </article>
    </CustomLink>
  )
}
