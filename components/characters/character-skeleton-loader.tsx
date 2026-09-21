import { Skeleton } from "@/components/ui/skeleton"

export default function CharacterSkeletonLoader() {
  // Create an array of 6 items to represent loading state
  const skeletonItems = Array.from({ length: 6 }, (_, i) => i)
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mt-6">
      {skeletonItems.map((item) => (
        <div key={item} className="border rounded-lg p-6 space-y-4">
          <div className="space-y-2">
            <Skeleton className="h-6 w-3/4" />
            <Skeleton className="h-4 w-1/4" />
          </div>
          <div className="space-y-2">
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-2/3" />
          </div>
          <div className="flex gap-2">
            <Skeleton className="h-6 w-20" />
            <Skeleton className="h-6 w-16" />
          </div>
          <div>
            <Skeleton className="h-4 w-1/3" />
          </div>
        </div>
      ))}
    </div>
  )
}
