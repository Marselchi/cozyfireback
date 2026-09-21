'use client'

import { useState, useActionState, startTransition } from 'react'
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Edit2, Save, X, FileText } from 'lucide-react'
import { updateCampaignSession } from '@/server/room/actions'
import { ConfirmDialog } from '@/components/utils/confirm-dialog'

interface EditableCampaignSituationProps {
  initialSession: string
  roomName: string
}

export function EditableCampaignSession({ initialSession, roomName }: Readonly<EditableCampaignSituationProps>) {
  const [isEditing, setIsEditing] = useState(false)
  const [tempSession, setTempSession] = useState(initialSession)
  const [showConfirmation, setShowConfirmation] = useState(false)
  const [state, action, isPending] = useActionState(updateCampaignSession.bind(null, roomName), {
      error: "",
      success: false,
    })

  const handleEdit = () => {
    setIsEditing(true)
    setTempSession(initialSession)
  }

  const handleCancel = () => {
    setIsEditing(false)
    setTempSession(initialSession)
  }

  const handleSave = () => {
    if (tempSession.trim() !== initialSession) {
      setShowConfirmation(true)
    } else {
      setIsEditing(false)
    }
  }

  const handleConfirm = () => {
    const formData = new FormData()
    formData.append('currentSession', tempSession)
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
              <FileText className="h-5 w-5" />
              Последняя сессия
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
              <Textarea
                value={tempSession}
                onChange={(e) => setTempSession(e.target.value)}
                placeholder="Опишите последнюю сессию"
                className="min-h-[120px] text-base"
                rows={6}
              />
              <div className="flex gap-2">
                <Button onClick={handleSave} size="sm">
                  <Save className="h-4 w-4 mr-2" />
                  Сохранить
                </Button>
                <Button variant="outline" onClick={handleCancel} size="sm">
                  <X className="h-4 w-4 mr-2" />
                  Изменить
                </Button>
              </div>
            </div>
          ) : (
            <p className="text-base leading-relaxed whitespace-pre-wrap">{initialSession}</p>
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
        title="Подтвердите изменение ситуации"
        description="Точно хочешь изменить?"
        confirmButtonType='update'
      />
    </>
  )
}
