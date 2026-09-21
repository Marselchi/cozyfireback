"use client"

import { Card, CardHeader, CardTitle } from "@/components/ui/card"
import { LoreItem } from "../../types/text"
import { useParams, useRouter } from "next/navigation"
import LoreItemCard from "../lore/lore-item"

interface LatestLoreBlockProps {
  loreItems?: LoreItem[]
}

export default function LatestLoreBlock({ loreItems }: LatestLoreBlockProps) {
  const roomName
  // Default lore items if none are provided
  const displayItems = loreItems?.length
    ? loreItems
    : []

  // Handler for tag clicks
  const handleTagClick = (tagName: string) => {
    //disable
  }

  return (
    <Card className="min-h-fit">
      <CardHeader>
        <CardTitle>Последний лор</CardTitle>
      </CardHeader>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 p-6">
        {displayItems.map((item) => (
          <LoreItemCard key={item.id} item={item} onTagClick={handleTagClick} main={roomName}/>
        ))}
      </div>
    </Card>
  )
}
