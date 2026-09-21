"use client"
import { useRouter, useSearchParams } from "next/navigation"
import { PageMeta } from "@/types/springTypes"
import { CharacterItem, CharacterItemData } from "./character-item"
import PaginationControls from "../lore/pagination-controls"

interface CharacterListProps {
  initialCharacters: CharacterItemData[]
  page?: PageMeta
}

export default function CharacterList({ initialCharacters, page }: Readonly<CharacterListProps>) {
  if(!page){
    return
  }
  const router = useRouter()
  const searchParams = useSearchParams()

  // Handle page change
  const handlePageChange = (page: number) => {
    const params = new URLSearchParams(searchParams)
    params.set("page", page.toString())
    router.push(`characters?${params.toString()}`)
  }

  if (page?.totalElements === 0) {
    return (
      <div className="text-center py-12">
        <h3 className="text-xl font-medium">Не найдено персонажей</h3>
        <p className="text-muted-foreground mt-2">Попробуйте изменить запрос или фильтры</p>
      </div>
    )
  }

  return (
    <div className="flex flex-col grow">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mt-2 grow">
        {initialCharacters.map((item) => (
          <div key={item.id} className="flex flex-col h-full">
            <CharacterItem data={item} />
          </div>
        ))}
      </div>
      <div className="pt-6">
        <PaginationControls currentPage={page.number+1} totalPages={page.totalPages} onPageChange={handlePageChange} />
        <div className="text-center text-sm text-muted-foreground mt-4">
          Показано {initialCharacters.length + page.number * page.size} из {page.totalElements}
        </div>
      </div>
    </div>
  )
}
