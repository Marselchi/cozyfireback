'use client'

import { useState, useActionState, startTransition } from 'react'
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Edit2, Save, X, Calendar } from 'lucide-react'
import { updateCampaignDate } from '@/server/room/actions'
import { ConfirmDialog } from '@/components/utils/confirm-dialog'

interface EditableCampaignDateProps {
  initialDate: string
  roomName: string
}

export function EditableCampaignDate({ initialDate, roomName }: Readonly<EditableCampaignDateProps>) {
  const [isEditing, setIsEditing] = useState(false)
  const [tempDate, setTempDate] = useState(initialDate)
  const [showConfirmation, setShowConfirmation] = useState(false)
  const [state, action, isPending] = useActionState(updateCampaignDate.bind(null,roomName),{
      error: "",
      success: false,
    })
  const handleEdit = () => {
    setIsEditing(true)
    setTempDate(initialDate)
  }

  const handleCancel = () => {
    setIsEditing(false)
    setTempDate(initialDate)
  }

  const handleSave = () => {
    if (tempDate !== initialDate) {
      setShowConfirmation(true)
    } else {
      setIsEditing(false)
    }
  }

  const handleConfirm = () => {
    const formData = new FormData()
    formData.append('currentDate', tempDate)
    action(formData)
    setShowConfirmation(false)
    setIsEditing(false)
  }

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Calendar className="h-5 w-5" />
              Текущая дата мира
            </div>
            {!isEditing && (
              <Button variant="ghost" size="sm" onClick={handleEdit}>
                <Edit2 className="h-4 w-4" />
              </Button>
            )}
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isEditing ? (
            <div className="space-y-4">
              <Input
                value={tempDate}
                onChange={(e) => setTempDate(e.target.value)}
                placeholder="Введите название партии"
                className="text-lg"
              />
              <div className="flex gap-2">
                <Button onClick={handleSave} size="sm">
                  <Save className="h-4 w-4 mr-2" />
                  Сохранить
                </Button>
                <Button variant="outline" onClick={handleCancel} size="sm">
                  <X className="h-4 w-4 mr-2" />
                  Отменить
                </Button>
              </div>
            </div>
          ) : (
            <p className="text-lg">{initialDate}</p>
          )}
          {state && (
            <div className={`mt-2 text-sm ${state.success ? 'text-green-600' : 'text-red-600'}`}>
              {state.error}
            </div>
          )}
        </CardContent>
      </Card>

      <ConfirmDialog
        open={showConfirmation}
        onOpenChange={() => setShowConfirmation(false)}
        onAccept={() => startTransition(handleConfirm)}
        title="Подтвердите изменение даты"
        description={`Точно хочешь изменить на ${tempDate}?`}
        confirmButtonType='update'
      />
    </>
  )
}
