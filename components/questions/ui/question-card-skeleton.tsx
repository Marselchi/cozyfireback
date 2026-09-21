"use client"

export function QuestionCardSkeleton() {
  return (
    <div className="p-4 rounded-lg border border-input bg-card animate-pulse">
      <div className="flex items-start space-x-3">
        {/* Status indicator skeleton */}
        <div className="shrink-0 mt-1">
          <div className="w-5 h-5 bg-input rounded-full"></div>
        </div>

        {/* Content skeleton */}
        <div className="flex-1 min-w-0">
          {/* Title skeleton */}
          <div className="h-4 bg-input rounded mb-2"></div>
          <div className="h-4 bg-input rounded w-3/4 mb-2"></div>

          {/* Body snippet skeleton */}
          <div className="h-3 bg-input rounded w-1/2 mb-2"></div>

          {/* Metadata skeleton */}
          <div className="flex items-center space-x-2">
            <div className="h-3 bg-input rounded w-16"></div>
            <div className="w-1 h-1 bg-input rounded-full"></div>
            <div className="h-3 bg-input rounded w-12"></div>
          </div>
        </div>
      </div>
    </div>
  )
}
