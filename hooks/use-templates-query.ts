"use client"

import { useInfiniteQuery, useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { searchTemplates, getTemplateContent, renameTemplate, deleteTemplate, type PaginatedTemplateResult } from "@/server/templates/templates"

// ---------------------------------------------------------------------------
// Types for infinite query
// ---------------------------------------------------------------------------

export type TemplatesInfiniteData = {
  pages: PaginatedTemplateResult[]
  pageParams: number[]
}

// ---------------------------------------------------------------------------
// Hooks
// ---------------------------------------------------------------------------

export function useTemplatesList(searchQuery: string, enabled: boolean = true) {
  return useInfiniteQuery({
    queryKey: ["templates", searchQuery],
    queryFn: ({ pageParam = 0 }) => searchTemplates(searchQuery, pageParam, 10),
    getNextPageParam: (lastPage) => {
      if (!lastPage.page) return undefined
      const nextPage = lastPage.page.number + 1
      if (nextPage >= lastPage.page.totalPages) return undefined
      return nextPage
    },
    initialPageParam: 0,
    staleTime: 60_000,
    refetchOnWindowFocus: false,
    enabled,
  })
}

export function useTemplateContent(templateId: number | null) {
  return useQuery({
    queryKey: ["template-content", templateId],
    queryFn: () => getTemplateContent(templateId!),
    enabled: templateId !== null,
    staleTime: 60_000,
    refetchOnWindowFocus: false,
  })
}

export function useRenameTemplate() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ templateId, newName }: { templateId: number; newName: string }) =>
      renameTemplate(templateId, newName),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["templates"] })
    },
  })
}

export function useDeleteTemplate() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (templateId: number) => deleteTemplate(templateId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["templates"] })
    },
  })
}
