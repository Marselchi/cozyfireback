import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"

interface CampaignSituationProps {
  situation?: string
}

export default function CampaignSituation({ situation }: CampaignSituationProps) {
  // Default value if no situation is provided
  const displaySituation =
    situation ||
    "Дм не заполнил чо происходит"

  return (
    <Card>
      <CardHeader>
        <CardTitle>Текущая ситуация</CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-muted-foreground">{displaySituation}</p>
      </CardContent>
    </Card>
  )
}
