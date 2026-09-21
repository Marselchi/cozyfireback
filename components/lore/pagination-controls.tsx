"use client"

import { Button } from "@/components/ui/button"
import { ChevronLeft, ChevronRight } from "lucide-react"
import { useMediaQuery } from "@/hooks/use-media-query"

interface PaginationControlsProps {
  currentPage: number
  totalPages: number
  onPageChange: (page: number) => void
}

export default function PaginationControls({ currentPage, totalPages, onPageChange }: Readonly<PaginationControlsProps>) {
  const isMobile = useMediaQuery("(max-width: 640px)")

  // Calculate which page numbers to show
  const getPageNumbers = () => {
    const pages = []
    const maxPagesToShow = isMobile ? 3 : 5

    let startPage = Math.max(1, currentPage - Math.floor(maxPagesToShow / 2))
    let endPage = startPage + maxPagesToShow - 1

    if (endPage > totalPages) {
      endPage = totalPages
      startPage = Math.max(1, endPage - maxPagesToShow + 1)
    }

    for (let i = startPage; i <= endPage; i++) {
      pages.push(i)
    }

    return pages
  }

  if (totalPages <= 1) return null

  const pageNumbers = getPageNumbers()
  const showFirstPage = !isMobile && pageNumbers[0] > 1
  const showLastPage = !isMobile && pageNumbers[pageNumbers.length - 1] < totalPages

  return (
    <div className="flex items-center justify-center space-x-2 mt-8">
      <Button
        variant="outline"
        size="icon"
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage === 1}
        aria-label="Предыдущая страница"
      >
        <ChevronLeft className="h-4 w-4" />
      </Button>

      {showFirstPage && (
        <>
          <Button variant="outline" size="sm" onClick={() => onPageChange(1)}>
            1
          </Button>
          {pageNumbers[0] > 2 && <span className="px-2">...</span>}
        </>
      )}

      {pageNumbers.map((page) => (
        <Button
          key={page}
          variant={currentPage === page ? "default" : "outline"}
          size="sm"
          onClick={() => onPageChange(page)}
        >
          {page}
        </Button>
      ))}

      {showLastPage && (
        <>
          {pageNumbers[pageNumbers.length - 1] < totalPages - 1 && <span className="px-2">...</span>}
          <Button variant="outline" size="sm" onClick={() => onPageChange(totalPages)}>
            {totalPages}
          </Button>
        </>
      )}

      <Button
        variant="outline"
        size="icon"
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage === totalPages}
        aria-label="Следующая страница"
      >
        <ChevronRight className="h-4 w-4" />
      </Button>
    </div>
  )
}