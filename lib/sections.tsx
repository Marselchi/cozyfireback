import type { RoomSection } from "@/types/room"
import { Home, BookOpen, HelpCircle, Users, Calendar, Shield } from "lucide-react"

export const roomSections: RoomSection[] = [
  {
    id: "main",
    label: "Главная",
    href: (roomSlug) => `/rooms/${roomSlug}`,
    icon: <Home className="h-5 w-5" />,
  },
  {
    id: "lore",
    label: "Лор",
    shortLabel: "Лор",
    href: (roomSlug) => `/rooms/${roomSlug}/lore`,
    icon: <BookOpen className="h-5 w-5" />,
  },
  {
    id: "questions",
    label: "Вопросы? Ответы!",
    shortLabel: "Вопросы",
    href: (roomSlug) => `/rooms/${roomSlug}/questions`,
    icon: <HelpCircle className="h-5 w-5" />,
  },
  {
    id: "characters",
    label: "Персонажи",
    shortLabel: "Персонажи",
    href: (roomSlug) => `/rooms/${roomSlug}/characters`,
    icon: <Users className="h-5 w-5" />,
  },
  {
    id: "sessions",
    label: "Сборы на партии",
    shortLabel: "Сборы",
    href: (roomSlug) => `/rooms/${roomSlug}/sessions`,
    icon: <Calendar className="h-5 w-5" />,
  },
  {
    id: "admin",
    label: "Админ",
    href: (roomSlug) => `/rooms/${roomSlug}/admin/main`,
    icon: <Shield className="h-5 w-5" />,
    adminOnly: true,
  },
]
