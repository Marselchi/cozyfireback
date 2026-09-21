import { Skeleton } from "@/components/ui/skeleton"

export function RoomNavigationSkeleton() {
  return (
    <div className="flex h-16 items-center justify-between border-b border-border bg-card px-4 lg:px-6">
      <div className="flex items-center gap-2">
        <Skeleton className="h-6 w-32" />
      </div>

      <div className="hidden items-center gap-2 md:flex">
        {Array.from({ length: 5 }).map((_, i) => (
          <Skeleton key={i} className="h-9 w-20" />
        ))}
      </div>

      <div className="flex items-center gap-2">
        <Skeleton className="h-9 w-9 rounded-full" />
        <Skeleton className="hidden h-6 w-24 md:block" />
      </div>
    </div>
  )
}

export function RoomContentSkeleton() {
  return (
    <div className="container mx-auto max-w-4xl space-y-6 p-6">
      <Skeleton className="h-10 w-3/4" />
      <div className="space-y-4">
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-4 w-5/6" />
        <Skeleton className="h-4 w-4/5" />
      </div>
    </div>
  )
}
