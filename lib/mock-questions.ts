import type {
  Question,
  Answer,
  QuestionsFilters,
  CreateQuestionRequest,
  UpdateQuestionRequest,
  CreateAnswerRequest,
  UpdateAnswerRequest,
} from "@/types/questions"

const CURRENT_USER_ID = "user-1"
const CURRENT_USER_NAME = "Игрок"

// Mutable store for mock data
let mockQuestions: Question[] = [
  {
    id: "q-1",
    title: "Суть проекта «Кластерный Распад»",
    body: "Нашел обрывки записей про >?проект «Кластерный Распад»<?. Кто может объяснить своими словами, что они там пытались сделать с ураном и магией?",
    author: "Любопытный_Плут",
    authorId: "user-1",
    createdAt: new Date("2025-12-01"),
    updatedAt: new Date("2025-12-02"),
    isAnswered: true,
    type: { id: "1", name: "lore" },
    category: undefined,
    loreId: "9",
    answers: [],
  },
  {
    id: "q-2",
    title: "Что пошло не так в лаборатории B-7?",
    body: "Ходят слухи, что эксперимент с >?Прототипом «Феникс-2»<? закончился взрывом. Что именно случилось с кристаллом и почему все уничтожило?",
    author: "Ветеран_Приключений",
    authorId: "user-3",
    createdAt: new Date("2025-12-05"),
    updatedAt: new Date("2025-12-06"),
    isAnswered: true,
    type: { id: "1", name: "lore" },
    category: undefined,
    loreId: "9",
    answers: [],
  },
]

let mockAnswers: Answer[] = [
  {
    id: "a-1",
    content: "Согласно документам, основная гипотеза заключалась в том, что >?канализированная<?. Авторы утверждали, что >?материя не просто расщепляется - она переводится в состояние чистой магической силы под управлением квантовых закономерностей<?.",
    author: "Архивариус_Гильдии",
    authorId: "user-10",
    createdAt: new Date("2025-12-02"),
    questionId: "q-1",
    parentId: undefined,
    repliesCount: 0,
    isDM: false,
  },
  {
    id: "a-2",
    content: "Все рухнуло, потому что >?на отметке 0.27 секунды кварцевый ФК треснул с характерным звуком, напоминающим крик<?. Сразу после этого >?произошел мини-взрыв (эквивалент ~5 кг тротила), полностью уничтоживший Лабораторию B-7<?. Виной всему стала >?недостаточная чистота кварца (микродефекты) и частичная несовместимость эссенции Падшего Дьявола с требуемыми резонансными свойствами для поддержания, а не только инициации реакции<?.",
    author: "Выживший_Лаборант",
    authorId: "user-13",
    createdAt: new Date("2025-12-06"),
    questionId: "q-2",
    parentId: undefined,
    repliesCount: 0,
    isDM: false,
  },
]


function delay(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export function getCurrentUserId() {
  return CURRENT_USER_ID
}

export function getCurrentUserName() {
  return CURRENT_USER_NAME
}

export async function fetchQuestions(
  filters: QuestionsFilters,
  loreId?: string
): Promise<QuestionsResponse> {
  await delay(400)

  let filtered = [...mockQuestions]

  // Filter by loreId if provided (for lore-specific questions)
  if (loreId) {
    filtered = filtered.filter((q) => q.loreId === loreId || q.type.name === "general")
  }

  if (filters.isAnswered !== null) {
    filtered = filtered.filter((q) => q.isAnswered === filters.isAnswered)
  }

  if (filters.search) {
    const search = filters.search.toLowerCase()
    filtered = filtered.filter(
      (q) =>
        q.title.toLowerCase().includes(search) ||
        q.body.toLowerCase().includes(search)
    )
  }

  if (filters.myQuestionsOnly) {
    filtered = filtered.filter((q) => q.authorId === CURRENT_USER_ID)
  }

  if (filters.category) {
    filtered = filtered.filter((q) => q.category?.id === filters.category?.id)
  }

  if (filters.type) {
    filtered = filtered.filter((q) => q.type.id === filters.type?.id)
  }

  const pageSize = 10
  const start = (filters.page - 1) * pageSize
  const end = start + pageSize
  const page = filtered.slice(start, end)

  return {
    questions: page,
    nextPage: end < filtered.length ? filters.page + 1 : undefined,
    hasMore: end < filtered.length,
  }
}

export async function fetchAnswers(
  questionId: string,
  parentId?: string
): Promise<Answer[]> {
  await delay(300)

  const answers = mockAnswers.filter(
    (a) => a.questionId === questionId && a.parentId === parentId
  )

  return answers.map((a) => ({ ...a }))
}

export async function createQuestion(
  req: CreateQuestionRequest & { type: Question["type"]; loreId?: string }
): Promise<Question> {
  await delay(300)

  const newQuestion: Question = {
    id: `q-${Date.now()}`,
    title: req.title,
    body: req.body,
    author: CURRENT_USER_NAME,
    authorId: CURRENT_USER_ID,
    createdAt: new Date(),
    isAnswered: false,
    type: req.type,
    category: req.category,
    loreId: req.loreId,
    answers: [],
  }

  mockQuestions = [newQuestion, ...mockQuestions]
  return newQuestion
}

export async function updateQuestion(
  req: UpdateQuestionRequest
): Promise<Question> {
  await delay(300)

  const idx = mockQuestions.findIndex((q) => q.id === req.id)
  if (idx === -1) throw new Error("Question not found")

  mockQuestions[idx] = {
    ...mockQuestions[idx],
    ...req,
    updatedAt: new Date(),
  }

  return mockQuestions[idx]
}

export async function deleteQuestion(id: string): Promise<void> {
  await delay(200)
  mockQuestions = mockQuestions.filter((q) => q.id !== id)
  mockAnswers = mockAnswers.filter((a) => a.questionId !== id)
}

export async function createAnswer(req: CreateAnswerRequest): Promise<Answer> {
  await delay(300)

  const newAnswer: Answer = {
    id: `a-${Date.now()}`,
    content: req.content,
    author: CURRENT_USER_NAME,
    authorId: CURRENT_USER_ID,
    createdAt: new Date(),
    questionId: req.questionId,
    parentId: req.parentId,
    repliesCount: 0,
  }

  mockAnswers = [...mockAnswers, newAnswer]

  // Increment parent repliesCount
  if (req.parentId) {
    const parentIdx = mockAnswers.findIndex((a) => a.id === req.parentId)
    if (parentIdx !== -1) {
      mockAnswers[parentIdx] = {
        ...mockAnswers[parentIdx],
        repliesCount: (mockAnswers[parentIdx].repliesCount ?? 0) + 1,
      }
    }
  }

  // Mark question as answered
  const qIdx = mockQuestions.findIndex((q) => q.id === req.questionId)
  if (qIdx !== -1) {
    mockQuestions[qIdx] = { ...mockQuestions[qIdx], isAnswered: true }
  }

  return newAnswer
}

export async function updateAnswer(req: UpdateAnswerRequest): Promise<Answer> {
  await delay(300)

  const idx = mockAnswers.findIndex((a) => a.id === req.id)
  if (idx === -1) throw new Error("Answer not found")

  mockAnswers[idx] = {
    ...mockAnswers[idx],
    content: req.content,
    updatedAt: new Date(),
  }

  return mockAnswers[idx]
}

export async function deleteAnswer(id: string): Promise<void> {
  await delay(200)
  const answer = mockAnswers.find((a) => a.id === id)
  if (answer?.parentId) {
    const parentIdx = mockAnswers.findIndex((a) => a.id === answer.parentId)
    if (parentIdx !== -1) {
      mockAnswers[parentIdx] = {
        ...mockAnswers[parentIdx],
        repliesCount: Math.max(
          0,
          (mockAnswers[parentIdx].repliesCount ?? 1) - 1
        ),
      }
    }
  }
  mockAnswers = mockAnswers.filter((a) => a.id !== id)
}
