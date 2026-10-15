"use client"

import { SidebarTrigger } from "@/components/ui/sidebar"
import {
  Home,
  Compass,
  Library,
  UploadCloud,
  Plus,
  Heart,
  Mic,
  Music2,
} from "lucide-react"
import Image from 'next/image'

const playlists = [
  { name: "Nhạc đã thích", icon: <Heart className="w-4 h-4" /> },
  { name: "nightcore buồn", icon: <Music2 className="w-4 h-4" /> },
  { name: "nightcore sôi động", icon: <Music2 className="w-4 h-4" /> },
  { name: "Tập podcast để thưởng thức ...", icon: <Mic className="w-4 h-4" /> },
]

export default function MusicSidebar() {
  return (
    <aside className="w-64 h-screen bg-white text-gray-800 flex flex-col border-r border-gray-200 z-99">
      {/* Logo */}
      <div className="h-14 px-4 flex items-center gap-3 border-b border-gray-200">
        <SidebarTrigger />
        {/* <img
          src="/image/Logo.png"
          alt="WeTube Premium"
          className="w-8 h-8 object-contain"
        /> */}
        <Image
          src="/image/Logo.png"
          alt="WeTube Premium"
          width={32}
          height={32}
          className="w-8 h-8 object-contain"
        />
        <span className="text-2xl font-bold">Music</span>
      </div>

      {/* Menu items */}
      <nav className="flex-1 px-2 py-4 space-y-1">
        <SidebarItem
          icon={<Home className="w-5 h-5" />}
          label="Trang chủ"
          active
        />
        <SidebarItem
          icon={<Compass className="w-5 h-5" />}
          label="Khám phá"
        />
        <SidebarItem
          icon={<Library className="w-5 h-5" />}
          label="Thư viện"
        />
        <SidebarItem
          icon={<UploadCloud className="w-5 h-5" />}
          label="Nâng cấp"
        />
      </nav>

      {/* New Playlist */}
      <div className="px-2 mb-2">
        <button className="w-full flex items-center gap-2 bg-gray-100 hover:bg-gray-200 px-3 py-2 rounded-md text-sm font-medium text-gray-800">
          <Plus className="w-4 h-4" />
          Danh sách phát mới
        </button>
      </div>

      {/* Playlist section */}
      <div className="px-2 pb-4 flex flex-col gap-2 border-t border-gray-200">
        {playlists.map((pl, i) => (
          <button
            key={i}
            className="w-full flex items-center gap-2 text-sm hover:bg-gray-100 text-gray-700 px-3 py-2 rounded-md text-left"
          >
            {pl.icon}
            <span className="truncate">{pl.name}</span>
          </button>
        ))}
      </div>
    </aside>
  );
}

function SidebarItem({
  icon,
  label,
  active = false,
}: {
  icon: React.ReactNode
  label: string
  active?: boolean
}) {
  return (
    <button
      className={`w-full flex items-center gap-2 px-3 py-2 rounded-md text-sm font-medium transition-colors ${
        active
          ? "bg-gray-100 text-black font-semibold"
          : "hover:bg-gray-100 text-gray-700"
      }`}
    >
      {icon}
      <span>{label}</span>
    </button>
  )
}
