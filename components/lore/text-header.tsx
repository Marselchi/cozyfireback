import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Edit, EyeOff, Shield, Trash, Lock, User, Eye } from "lucide-react"
import type { TextData } from "@/types/text"
import { deleteLore } from "@/server/lore/lore"
import CustomLink from "../no-prefetch-link"

interface TextHeaderProps {
  textData: TextData
}

export function TextHeader({ textData }: Readonly<TextHeaderProps>) {
  
  return (
    <header className="relative bg-card border border-border rounded-xl p-5 sm:p-6 mb-8 shadow-sm hover:shadow-md transition-shadow duration-300">
      {/* Header content */}
      <div className="flex flex-col gap-4">
        <div className="flex flex-col gap-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div className="flex flex-wrap items-center gap-2">
              {textData.nonPublic ? (
                <div className="flex items-center gap-1.5 text-xs font-medium px-2.5 py-1 rounded-full bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/30">
                  <Lock className="w-3 h-3" />
                  <span>Доступ по роли</span>
                </div>
              ) : (
                <div className="flex items-center gap-1.5 text-xs font-medium px-2.5 py-1 rounded-full bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30">
                  <User className="w-3 h-3" />
                  <span>Публичный</span>
                </div>
              )}
              
              <div className="flex items-center gap-1.5 text-xs font-medium px-2.5 py-1 rounded-full bg-slate-500/10 text-slate-600 dark:text-slate-300 border border-slate-500/30">
                <Eye className="w-3 h-3" />
                <span>{textData.viewCount} просмотров</span>
              </div>
            </div>
            
            {/* Action buttons for author - positioned at the far right */}
            {textData.isAuthor && (
              <div className="flex flex-col sm:flex-row gap-2 ml-auto">
                <CustomLink href={`${textData.id}/edit/author`}>
                  <Button 
                    variant="outline" 
                    size="sm" 
                    className="gap-2 border-border hover:bg-primary/5 text-foreground transition-all duration-200 group whitespace-nowrap"
                  >
                    <Edit className="w-4 h-4 transition-transform group-hover:-translate-x-0.5" />
                    <span className="hidden sm:inline">Редактировать</span>
                    <span className="sm:hidden">Ред.</span>
                  </Button>
                </CustomLink>
                
                <form action={deleteLore.bind(null, textData.id.toString())}>
                  <Button
                    variant="outline"
                    size="sm"
                    className="gap-2 border-border hover:bg-destructive/10 text-destructive transition-all duration-200 group whitespace-nowrap"
                    type="submit"
                  >
                    <Trash className="w-4 h-4 transition-transform group-hover:rotate-12" />
                    <span className="hidden sm:inline">Удалить</span>
                    <span className="sm:hidden">Удл.</span>
                  </Button>
                </form>
              </div>
            )}
          </div>
          
          <div className="flex-1 w-full">
            {/* Title and metadata */}
            <div className="mb-4">
              <h1 className="text-2xl sm:text-3xl font-bold text-foreground mb-3 bg-gradient-to-r from-primary to-primary/70 bg-clip-text">
                {textData.title}
              </h1>
              
              <div className="flex flex-wrap items-center gap-x-3 gap-y-1.5 text-sm text-muted-foreground mb-3">
                <div className="flex items-center gap-1.5">
                  <span>Автор:</span>
                  <span className="font-medium text-foreground">{textData.author}</span>
                </div>
                
                {textData.byAdmin && (
                  <div className="flex items-center gap-1.5 text-[hsl(var(--lore-admin))] ">
                    <Shield className="w-4 h-4" />
                    <span className="text-xs font-medium">DM</span>
                  </div>
                )}
                
                <div className="flex items-center gap-1.5">
                  <span>•</span>
                  <span className="italic">{textData.date}</span>
                </div>
              </div>
              
              <p className="text-muted-foreground/90 text-base leading-relaxed wrap-break-word">
                {textData.description}
              </p>
            </div>
          </div>
        </div>
        
        {/* Tags section */}
        <div className="pt-4 border-t border-border/50 mt-0">
          <div className="flex flex-wrap gap-2">
            {textData.tags?.map((tag) => (
              <CustomLink 
                key={tag.name + textData.author} 
                href={`./lore?tag=${encodeURIComponent(tag.name)}`}
              >
                <Badge 
                  variant="outline" 
                  className="cursor-pointer rounded-full border-primary/30 bg-primary/5 px-3 py-1.5 text-xs font-medium transition-all duration-200 hover:bg-primary/10 hover:border-primary/50"
                >
                  <span className="mr-1.5">#</span>
                  {tag.name}
                </Badge>
              </CustomLink>
            ))}
            
            {!textData.tags || textData.tags.length === 0 ? (
              <span className="text-xs text-muted-foreground italic">Теги отсутствуют</span>
            ) : null}
          </div>
        </div>
      </div>
    </header>
  )
}
