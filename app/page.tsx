import ProfileSection from "@/components/room/profile-section"
import RoomList from "@/components/room/room-list"
import { getMainUser } from "../server/user/user"
import { getUserRooms } from "../server/user/room"
import { Suspense } from "react"
import { Loader2 } from "lucide-react"

export default async function RoomsPage() {
  

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-8">Ваши комнаты</h1>
      <Suspense fallback={<Loader2/>}>
      <ProfileSectionSuspense/>
      </Suspense>
      <div className="mt-8">
        <Suspense fallback={<Loader2/>}>
        <RoomListSuspense/>
        </Suspense>
      </div>
      
    </div>
  )
}

const ProfileSectionSuspense = async () =>{
  const userData = await getMainUser()
  return <ProfileSection user={userData} />
}

const RoomListSuspense = async () => {
  const roomsData = await getUserRooms()
  return <RoomList rooms={roomsData} />
}