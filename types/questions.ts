export interface QuestionCategory {
  id: string,
  name: QuestionCategoryName
}

export interface QuestionType {
  id: string,
  name: QuestionTypeName
}
export interface Author {
  id: string
  name: string
}

export interface Category {
  id: string
  name: string
}

export type QuestionCategoryName = 
  | "Мир" 
  | "Лор" 
  | "Предметы" 
  | "Правила" 
  | "Общее" 
  | "Персонажи" 
  | "Мета"

export type QuestionTypeName = 
  | "Лор" 
  | "Общие"
  | "Персонаж" 


export const CATEGORIES: QuestionCategory [] = [
  { id: "0", name: "Мир" },
  { id: "1", name: "Лор" },
  { id: "2", name: "Предметы" },
  { id: "3", name: "Правила" },
  { id: "4", name: "Общее" },
  { id: "5", name: "Персонажи" },
  { id: "6", name: "Мета" },
];

export interface LoreEntry {
  id: number
  title: string
  description?: string
}


export interface Question {
  id: string
  title: string
  body: string
  author: string
  authorId?: string
  createdAt: Date
  updatedAt?: Date
  isAnswered: boolean
  answers?: Answer[]
  category?: QuestionCategory
  loreId?: string
  characterId?: string
  type: QuestionType
}

export interface Answer {
  id: string
  content: string
  author: string | { id: string; name: string }
  authorId?: string
  createdAt: Date
  updatedAt?: Date
  questionId?: string
  parentId?: string 
  repliesCount?: number
  isDM?: boolean
  replies?: Answer[]
}


export interface QuestionsFilters {
  search: string | null
  isAnswered: boolean | null
  authorId: number | null
  categoryId: number | null
  loreId: number | null
  characterId: number | null
  hasLore: boolean | null
  hasCharacter: boolean | null
}

export interface CreateQuestionRequest {
  title: string
  body: string
  category?: QuestionCategory
  loreId? : string
  characterId? : string
}

export interface UpdateQuestionRequest {
  id: string
  title?: string
  body?: string
  category?: QuestionCategory
}

export interface CreateAnswerRequest {
  questionId?: string
  content: string
  parentId?: string
  roomName: string
}

export interface UpdateAnswerRequest {
  id: string
  content: string
}

export interface DeleteItemRequest {
  id: string
  type: "question" | "answer"
}

export interface QuestionWithAnswers {
  question: Question
  /** Root-level answers. Ancestor replies are pre-populated when `answerId` was supplied. */
  answers: Answer[]
  /**
   * IDs of answers whose `replies` were pre-populated by the server.
   * Empty array when no `answerId` was requested.
   */
  expandedIds: string[]
}

