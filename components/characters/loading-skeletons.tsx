import { Skeleton } from "@/components/ui/skeleton"

export function CharacterHeaderSkeleton() {
  return (
    <header className="flex flex-col md:flex-row gap-6 pb-6 border-b">
      <Skeleton className="w-48 h-48 md:w-64 md:h-64 rounded-lg flex-shrink-0" />
      <div className="flex-1 space-y-4">
        <Skeleton className="h-10 w-64" />
        <div className="space-y-2">
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-3/4" />
        </div>
      </div>
    </header>
  )
}

export function StatusBlockSkeleton() {
  return (
    <section className="space-y-4">
      <Skeleton className="h-7 w-24" />
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4 p-4 bg-muted/30 rounded-lg">
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="flex items-center gap-2">
            <Skeleton className="h-4 w-16" />
            <Skeleton className="h-4 w-24" />
          </div>
        ))}
      </div>
    </section>
  )
}

export function TextBlockSkeleton() {
  return (
    <div className="space-y-2">
      <div className="flex items-center gap-2">
        <Skeleton className="h-4 w-4" />
        <Skeleton className="h-6 w-32" />
      </div>
      <div className="pl-6 space-y-2">
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-4 w-3/4" />
      </div>
    </div>
  )
}

export function CharacterPageSkeleton() {
  return (
    <div className="max-w-4xl mx-auto p-6 space-y-8">
      <div className="flex items-center justify-end gap-2 mb-6">
        <Skeleton className="h-8 w-28" />
        <Skeleton className="h-8 w-20" />
      </div>
      <CharacterHeaderSkeleton />
      <StatusBlockSkeleton />
      <TextBlockSkeleton />
    </div>
  )
}
