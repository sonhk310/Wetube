"use client";

import { Camera, CameraOff, Check, ChevronDown, Mic, MicOff } from "lucide-react";
import { useLocalParticipant, useMediaDeviceSelect } from "@livekit/components-react";
import { Button } from "@/components/ui/button";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";

function DeviceMenu({
  disabled,
  devices,
  activeId,
  onPick,
}: {
  disabled: boolean;
  devices: MediaDeviceInfo[];
  activeId: string;
  onPick: (deviceId: string) => void;
}) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="secondary" size="icon" disabled={disabled} aria-label="Chọn thiết bị">
          <ChevronDown className="size-4" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start">
        {devices.map((d) => (
          <DropdownMenuItem key={d.deviceId} onSelect={() => onPick(d.deviceId)}>
            <span className="max-w-[16rem] truncate">{d.label || "Thiết bị không tên"}</span>
            {d.deviceId === activeId ? <Check className="ml-auto size-4" /> : null}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

/** Bật / tắt micro, camera và chọn thiết bị; phải nằm trong LiveKitRoom của chủ kênh. */
export default function LiveDeviceSettings() {
  const { localParticipant, isMicrophoneEnabled, isCameraEnabled } = useLocalParticipant();
  const mic = useMediaDeviceSelect({ kind: "audioinput" });
  const cam = useMediaDeviceSelect({ kind: "videoinput" });

  return (
    <div className="flex flex-wrap items-center gap-3">
      <div className="flex items-center gap-1">
        <Button
          variant={isMicrophoneEnabled ? "secondary" : "outline"}
          onClick={() => void localParticipant.setMicrophoneEnabled(!isMicrophoneEnabled)}
        >
          {isMicrophoneEnabled ? <Mic className="size-4" /> : <MicOff className="size-4" />}
          Micro {isMicrophoneEnabled ? "bật" : "tắt"}
        </Button>
        <DeviceMenu
          disabled={!isMicrophoneEnabled}
          devices={mic.devices}
          activeId={mic.activeDeviceId}
          onPick={(id) => void mic.setActiveMediaDevice(id)}
        />
      </div>
      <div className="flex items-center gap-1">
        <Button
          variant={isCameraEnabled ? "secondary" : "outline"}
          onClick={() => void localParticipant.setCameraEnabled(!isCameraEnabled)}
        >
          {isCameraEnabled ? <Camera className="size-4" /> : <CameraOff className="size-4" />}
          Camera {isCameraEnabled ? "bật" : "tắt"}
        </Button>
        <DeviceMenu
          disabled={!isCameraEnabled}
          devices={cam.devices}
          activeId={cam.activeDeviceId}
          onPick={(id) => void cam.setActiveMediaDevice(id)}
        />
      </div>
    </div>
  );
}
