"use client"

import { RoomUser } from "@/server/user/user"
import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from "react"

interface UserContextType {
  user: RoomUser
  updateUserName: (newName: string) => void
}

const UserContext = createContext<UserContextType | undefined>(undefined)

interface UserProviderProps {
  children: ReactNode
  initialUser: RoomUser
}

export function UserProvider({ children, initialUser }: UserProviderProps) {
  const [user, setUser] = useState<RoomUser>(initialUser)

  const updateUserName = useCallback((newName: string) => {
    //бд смена имени
    setUser((prev) => ({ ...prev, name: newName }))
  }, [])

  const contextValue = useMemo(() => ({
    user,
    updateUserName
  }), [user, updateUserName])

  return <UserContext.Provider value={contextValue}>{children}</UserContext.Provider>
}

export function useUser() {
  const context = useContext(UserContext)
  if (context === undefined) {
    throw new Error("useUser must be used within a UserProvider")
  }
  return context
}
