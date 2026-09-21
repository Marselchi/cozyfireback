import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import UserItem from "./user-item"
import { RoomAccountResponse } from "@/types/manage"
import { IdName } from "@/types/springTypes"


interface UserListProps {
  users: RoomAccountResponse[]
  availableRoles: IdName[]
}

export default function UserList({ users, availableRoles }: Readonly<UserListProps>) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-xl font-semibold">Пользователи ({users.length})</CardTitle>
      </CardHeader>
      <CardContent>
        {users.length === 0 ? (
          <p className="text-muted-foreground text-center py-8">Пользователи не найдены.</p>
        ) : (
          <div className="space-y-4">
            {users.map((user) => (
              <UserItem key={user.id} user={user} availableRoles={availableRoles} />
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}
