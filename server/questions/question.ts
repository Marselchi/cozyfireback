'use server'
import 'server-only'

import {
  type Question,
  type QuestionsFilters,
  type CreateQuestionRequest,
  type UpdateQuestionRequest,
  type CreateAnswerRequest,
  type UpdateAnswerRequest,
  type Answer,
  CATEGORIES,
  Author,
  QuestionWithAnswers,
} from "@/types/questions"
import { buildQueryString, getWithAuth, sendWithAuth } from '@/lib/auth/apiClient'
import { PageableParams } from '@/types/springTypes'

export interface PaginatedQuestionsResult {
  content: Question[];
  page?: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

export async function getFilterQuestions({filters, pageable, roomName}: {filters: QuestionsFilters, pageable: PageableParams, roomName: string, loreId?: string, characterId?: string}): Promise<PaginatedQuestionsResult> {
    
    const queryString = buildQueryString(filters, pageable)
    const [error, data] = await getWithAuth(`/questions/${roomName}?${queryString || ''}`)
    if (error) {
      console.error(error)
      return { content: [] }
    }
    const formattedData : Question[] = Array.isArray(data?.content) ? data.content.map((question : any) => ({
      id: question.id,
      title: question.title,
      body: question.body,
      author: question.author.username,
      authorId: question.author.id,
      createdAt: question.createdAt, 
      updatedAt: question.updatedAt,
      isAnswered: question.isAnswered,
      type: question.character 
        ? { id: "2", name: "Персонаж" } 
        : question.lore 
          ? { id: "1", name: "Лор" } 
          : { id: "0", name: "Общие" },
      loreId: question.lore?.id,
      characterId: question.character?.id,
      category: question.category ? CATEGORIES.find(cat => cat.id === question.category.toString()) : undefined
    })) : []


    const page = data?.page
    ? {
        size: data.page.size,
        number: data.page.number,
        totalElements: data.page.totalElements,
        totalPages: data.page.totalPages,
      }
    : undefined
    
    return {
        content: formattedData, page
    }
}

export async function createQuestion({request, roomName} : {request: CreateQuestionRequest, roomName: string}): Promise<number | null> {

  const [error, data] = await sendWithAuth(`/questions/${roomName}`, "POST", {
    title: request.title,
    body: request.body,
    category: request.category?.id,
    loreId: request.loreId,
    characterId: request.characterId
  })
  if (error) {
    console.error(error)
    return null
  }

  return data
}

export async function updateQuestion(request: UpdateQuestionRequest): Promise<number | null> {

  const [error, data] = await sendWithAuth(`/questions/${request.id}`, "PUT", {
    title: request.title,
    body: request.body,
    category: request.category,
  })
  if (error) {
    console.error(error)
    return null
  }

  return data
}

export async function getQuestionById(roomName: string, id: string, answerId: string | null): Promise<QuestionWithAnswers | null> {
  const query = answerId ? `?answerId=${answerId}`: ''
  const [error, data] = await getWithAuth(`/questions/${roomName}/${id}/single${query}`);
  if (error || !data) {
    console.error(error)
    return null
  }

  const answers : Answer[] = data.answers.map((ans : any) => ({
    id: ans.id,
    content: ans.content,
    authorId: ans.author.id,
    author: ans.author.username,
    createdAt: ans.createdAt,
    updatedAt: ans.updatedAt,
    parentId: id,
    repliesCount: ans.replyCount,
    replies: ans.replies ?? {},
    isDM: ans.isAdmin
  }))
  const formattedData : QuestionWithAnswers = {
    question: {
      id: data.question.id,
      title: data.question.title,
      body: data.question.body,
      author: data.question.author.username,
      authorId: data.question.author.id,
      createdAt: data.question.createdAt, 
      updatedAt: data.question.updatedAt,
      isAnswered: data.question.isAnswered,
      type: data.question.character 
        ? { id: "2", name: "Персонаж" } 
        : data.question.lore 
          ? { id: "1", name: "Лор" } 
          : { id: "0", name: "Общие" },
      loreId: data.question.lore?.id,
      characterId: data.question.character?.id,
      category: data.question.category ? CATEGORIES.find(cat => cat.id === data.question.category.toString()) : undefined
    },
    expandedIds: data.pathToAnswer,
    answers: answers
  }



  return formattedData
}

export async function getDaughterAnswers(id: string): Promise<Answer[]> {
  const [error, data] = await getWithAuth(`/answers/${id}/children`);
  if (error || !data) {
    console.error(error)
    return []
  }


  const formattedData : Answer[] = data.map((ans : any) => ({
    id: ans.id,
    content: ans.content,
    authorId: ans.author.id,
    author: ans.author.username,
    createdAt: ans.createdAt,
    updatedAt: ans.updatedAt,
    parentId: id,
    repliesCount: ans.replyCount,
    isDM: ans.isAdmin
  }))


  return formattedData
}

export async function getAnswers(id: string): Promise<Answer[]> {
  const [error, data] = await getWithAuth(`/answers/question/${id}`);
  if (error || !data) {
    console.error(error)
    return []
  }


  const formattedData : Answer[] = data.map((ans : any) => ({
    id: ans.id,
    content: ans.content,
    authorId: ans.author.id,
    author: ans.author.username,
    createdAt: ans.createdAt,
    updatedAt: ans.updatedAt,
    parentId: id,
    repliesCount: ans.replyCount,
    isDM: ans.isAdmin
  }))

  return formattedData
}

export async function createAnswer({request} : {request: CreateAnswerRequest}): Promise<Answer | null> {


  const [error, data] = await sendWithAuth(`/answers/${request.roomName}`, "POST", {
    content: request.content,
    questionId: request.questionId,
    parentId: request.parentId,
  })
  if (error|| !data) {
    console.error(error)
    return null
  }

  return {
    id: data.id,
    content: data.content,
    authorId: data.author.id,
    author: data.author.username,
    createdAt: data.createdAt,
    updatedAt: data.updatedAt,
    parentId: request.parentId,
    repliesCount: 0,
    isDM: data.isAdmin
  }
}


export async function updateAnswer(request: UpdateAnswerRequest): Promise<Answer | null> {

  const [error, data] = await sendWithAuth(`/answers/${request.id}`, "PUT", {
    content: request.content
  })
  if (error|| !data) {
    console.error(error)
    return null
  }


  return {
    id: data.id,
    content: data.content,
    authorId: data.author.id,
    author: data.author.username,
    createdAt: data.createdAt,
    updatedAt: data.updatedAt,
    parentId: data.parentId,
    repliesCount: data.replyCount,
    isDM: data.isAdmin
  }
}

export async function getRoomAuthors(roomName: string): Promise<Author[]> {
  const [error, data] = await getWithAuth(`/questions/${roomName}/authors`);
  if (error || !data) {
    console.error(error)
    return []
  }
  const formattedData : Author[] = data.map((author: any) => ({
    id: author.id,
    name: author.username
  }))

  return formattedData
}


export async function deleteAnswer(id: string): Promise<void> {
    const [error] = await sendWithAuth(`/answers/${id}`, "DELETE", null, {expectsJson: false})
    if (error) {
      console.error(error)
    }
}

export async function deleteQuestion(id: string): Promise<void> {
    const [error] = await sendWithAuth(`/questions/${id}`, "DELETE", null, {expectsJson: false})
    if (error) {
      console.error(error)
    }
}