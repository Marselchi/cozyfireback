import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function parseSupabaseDate(dateInput: string | Date): Date {
  if (dateInput instanceof Date) {
    return dateInput
  }

  if (typeof dateInput === "string") {
    // Replace space with 'T' to make it ISO format compatible
    const isoString = dateInput.replace(" ", "T")
    const parsedDate = new Date(isoString)

    if (Number.isNaN(parsedDate.getTime())) {
      console.error("Failed to parse date:", dateInput)
      return new Date() // Return current date as fallback
    }

    return parsedDate
  }

  return new Date() // Fallback to current date
}

export function getRelativeTime(dateInput: string | Date): string {
  const date = parseSupabaseDate(dateInput)
  const now = new Date()
  const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000)

  if (diffInSeconds < 60) return "только что"
  if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)} мин назад`
  if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)} ч назад`
  if (diffInSeconds < 2592000) return `${Math.floor(diffInSeconds / 86400)} дн назад`

  return date.toLocaleDateString("ru-RU")
}
