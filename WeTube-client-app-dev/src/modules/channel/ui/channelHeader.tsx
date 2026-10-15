import Image from 'next/image';

// modules/channel/ui/components/channel-header.tsx
export default function ChannelHeader() {
  return (
    <div className="relative p-4 pt-0">
      <div className="relative w-full h-40 rounded-lg overflow-hidden">
        {" "}
        <Image
          src="https://yt3.ggpht.com/ld2s6PuqoMgr1J8TIi3ziWrhndP0XRhFwyZOY7fM81muHcwifQIv-xjugYkmcTZds58sIEur=w2276-fcrop64=1,00005a57ffffa5a8-k-c0xffffffff-no-nd-rj"
          alt="Banner"
          fill
          sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
          className="object-cover"
        />
      </div>
      <div className="flex items-center gap-4 -mt-10 px-4">
        <Image
          src="https://yt3.ggpht.com/ytc/AIdro_mVeqnCcWOqoVrLHhOT6Uy2vxSaU9379p4QvKain4nUCkw=s160-c-k-c0x00ffffff-no-rj"
          alt="Avatar"
          width={160}
          height={160}
          className="rounded-full shadow-xl z-10"
        />
        <div className="flex flex-col">
          <h1 className="text-2xl font-bold">Đức Anh Trader</h1>
          <p className="text-sm text-gray-500">
            @ducanhtrader • 100K subscribers
          </p>
        </div>
        <button className="ml-auto px-4 py-2 bg-black text-white rounded-md">
          Subscribe
        </button>
      </div>
    </div>
  );
}
