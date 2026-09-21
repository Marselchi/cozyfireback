import { Player, Session, User } from "@/types/sessionPrototype";

export const mockPlayers: Player[] = [
  { id: "p1", name: "Алекс Чен", avatar: "/placeholder.svg?height=40&width=40", email: "alex@example.com" },
  { id: "p2", name: "Сара Миллер", avatar: "/placeholder.svg?height=40&width=40", email: "sarah@example.com" },
  { id: "p3", name: "Маркус Джонсон", avatar: "/placeholder.svg?height=40&width=40", email: "marcus@example.com" },
  { id: "p4", name: "Эмма Уилсон", avatar: "/placeholder.svg?height=40&width=40", email: "emma@example.com" },
  { id: "p5", name: "Дэвид Ким", avatar: "/placeholder.svg?height=40&width=40", email: "david@example.com" },
  { id: "p6", name: "Лиза Томпсон", avatar: "/placeholder.svg?height=40&width=40", email: "lisa@example.com" },
];

export const mockSessions: Session[] = [
  {
    id: "s1",
    date: "2026-03-19",
    time: "19:00",
    description: "Еженедельная рейдовая ночь — покажите всё, на что способны!",
    invites: [
      { playerId: "p1", status: "accepted" },
      { playerId: "p2", status: "pending" },
      { playerId: "p3", status: "declined" },
    ],
    createdBy: "admin1",
  },
  {
    id: "s2",
    date: "2026-03-20",
    time: "20:30",
    description: "Тренировка к турниру",
    invites: [
      { playerId: "p1", status: "accepted" },
      { playerId: "p4", status: "accepted" },
      { playerId: "p5", status: "accepted" },
    ],
    createdBy: "admin1",
  },
  {
    id: "s3",
    date: "2026-03-21",
    time: "18:00",
    description: "Неформальная игровая сессия — новички приветствуются",
    invites: [],
    createdBy: "admin1",
  },
  {
    id: "s4",
    date: "2026-03-22",
    time: "21:00",
    description: "Игровая сессия — все на борт!",
    invites: [
      { playerId: "p2", status: "accepted" },
      { playerId: "p3", status: "accepted" },
      { playerId: "p6", status: "pending" },
    ],
    createdBy: "admin1",
  },
  {
    id: "s5",
    date: "2026-03-23",
    time: "17:30",
    description: "Сессия планирования стратегии",
    invites: [
      { playerId: "p1", status: "declined" },
      { playerId: "p2", status: "accepted" },
      { playerId: "p4", status: "declined" },
    ],
    createdBy: "admin1",
  },
  {
    id: "s6",
    date: "2026-03-24",
    time: "19:00",
    description: "Еженедельная рейдовая ночь — покажите всё, на что способны!",
    invites: [
      { playerId: "p1", status: "accepted" },
      { playerId: "p2", status: "pending" },
      { playerId: "p3", status: "declined" },
    ],
    createdBy: "admin1",
  },
];

export const mockUsers: Record<string, User> = {
  admin: {
    id: "admin1",
    name: "ДМ",
    role: "admin",
    avatar: "/placeholder.svg?height=40&width=40",
    email: "admin@example.com",
  },
  player: {
    id: "p1",
    name: "Алекс Чен",
    role: "player",
    avatar: "/placeholder.svg?height=40&width=40",
    email: "alex@example.com",
  },
};