import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"

interface CampaignDateProps {
  date?: string
}

export default function CampaignDate({ date }: CampaignDateProps) {
  // Default value if no date is provided
  const displayDate = date || "День, когда дм не заполнил это"

  return (
    <Card>
      <CardHeader>
        <CardTitle>Текущая дата мира</CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-xl font-bold">{displayDate}</p>
      </CardContent>
    </Card>
  )
}
