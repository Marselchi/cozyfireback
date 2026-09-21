import { Card, CardHeader, CardTitle, CardContent } from "../ui/card"

interface LastSessionProps {
  session?: string
}

export default function LastSession({ session }: Readonly<LastSessionProps>) {
  // Default value if no situation is provided
  const displaySession =
    session ||
    "Видимо ничего не было"

  return (
    <Card>
      <CardHeader>
        <CardTitle>Последняя сессия</CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-muted-foreground">{displaySession}</p>
      </CardContent>
    </Card>
  )
}