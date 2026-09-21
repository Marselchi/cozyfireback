"use client"
import { useRouter, useSearchParams } from "next/navigation"
import PaginationControls from "./pagination-controls"
import { IdName, PageMeta } from "@/types/springTypes"
import {LoreItem, LoreItemData } from "./lore-item"

interface LoreListProps {
  initialLoreItems: LoreItemData[]
  page?: PageMeta
}
const ITEMS_PER_PAGE = 6; 

export default function LoreList({ initialLoreItems, page }: Readonly<LoreListProps>) {
  if(!page){
    return
  }
  const router = useRouter()
  const searchParams = useSearchParams()
  // Handle tag click
  const handleTagClick = (tag: IdName) => {
    const params = new URLSearchParams(searchParams)
    const currentTag = params.get("tag")

    if (currentTag === tag.name) {
      params.delete("tag")
    } else {
      params.set("tag", tag.name)
    }

    // Reset to first page when changing filters
    params.delete("page")

    router.push(`lore?${params.toString()}`)
  }

  // Handle page change
  const handlePageChange = (page: number) => {
    const params = new URLSearchParams(searchParams)
    params.set("page", page.toString())
    router.push(`lore?${params.toString()}`)
  }

  if (page?.totalElements === 0) {
    return (
      <div className="text-center py-12">
        <h3 className="text-xl font-medium">Не найдено лора</h3>
        <p className="text-muted-foreground mt-2">Попробуйте изменить запрос или фильтры</p>
      </div>
    )
  }

  return (
    <div className="flex flex-col grow">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mt-2 grow">
        {Array.from({ length: ITEMS_PER_PAGE }, (_, index) => {
          const item = initialLoreItems[index];
          return (
            <div key={item?.id ?? `empty-${index}`} className="flex flex-col h-full">
              {item ? (
                <LoreItem data={item} options={{ onTagClick: handleTagClick }} />
              ) : (
                <div className="h-full" />
              )}
            </div>
          );
        })}
      </div>
      <div className="pt-6">
        <PaginationControls currentPage={page.number+1} totalPages={page.totalPages} onPageChange={handlePageChange} />
        <div className="text-center text-sm text-muted-foreground mt-4">
          Показано {initialLoreItems.length + 6*(page.number)} из {page.totalElements}
        </div>
      </div>
    </div>
  )
}
