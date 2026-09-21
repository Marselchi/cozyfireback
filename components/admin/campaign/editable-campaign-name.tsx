'use client'

import { useState, useActionState, startTransition } from 'react'
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Edit2, Save, X } from 'lucide-react'
import { updateCampaignName } from '@/server/room/actions'
import { ConfirmDialog } from '@/components/utils/confirm-dialog'

interface EditableCampaignNameProps {
  initialName: string
  roomName: string
}

export function EditableCampaignName({ initialName, roomName }: Readonly<EditableCampaignNameProps>) {
  const [isEditing, setIsEditing] = useState(false)
  const [tempName, setTempName] = useState(initialName)
  const [showConfirmation, setShowConfirmation] = useState(false)
  const [state, action, isPending] = useActionState(updateCampaignName.bind(null,roomName), {
      error: "",
      success: false,
    })

  const handleEdit = () => {
    setIsEditing(true)
    setTempName(initialName)
  }

  const handleCancel = () => {
    setIsEditing(false)
    setTempName(initialName)
  }

  const handleSave = () => {
    if (tempName.trim() !== initialName) {
      setShowConfirmation(true)
    } else {
      setIsEditing(false)
    }
  }

  const handleConfirm = () => {
    const formData = new FormData()
    formData.append('name', tempName)
    action(formData)
    setShowConfirmation(false)
    setIsEditing(false)
  }

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center justify-between">
            Название партии
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
                onChange={(e) => setTempName(e.target.value)}
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
            <p className="text-lg font-medium">{initialName}</p>
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
        title="Подтвердите изменение названия"
        description={`Точно хочешь изменить на "${tempName}"?`}
        confirmButtonType='update'
      />
    </>
  )
}
