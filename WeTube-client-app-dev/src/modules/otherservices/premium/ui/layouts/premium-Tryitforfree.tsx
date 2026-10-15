"use client";

import Image from 'next/image';
import PaymentButton from './PaymentButton';

const features = [
  {
    icon: "/icons/no-ads.png",
    title: "Watch YouTube without ads",
    desc: "immerse yourself in the videos you love without interruptions",
  },
  {
    icon: "/icons/download.png",
    title: "Download",
    desc: "to watch when you don’t have an internet connection",
  },
  {
    icon: "/icons/background-play.png",
    title: "Background play",
    desc: "keep videos playing while using other apps or when the screen is locked",
  },
  {
    icon: "/icons/music.png",
    title: "Ad-free music",
    desc: "on the YouTube Music app",
  },
];

export default function PremiumTryitforfree() {

  return (
    <section className="min-h-screen bg-gradient-to-b from-blue-50 to-white flex flex-col items-center text-center px-4 py-20pt-10 pb-20">
      <div className="max-w-3xl w-full flex flex-col items-center">
       
        <div className="flex items-center justify-center gap-x-4 mb-6">
          <Image
            src="/image/Logo.png"
            alt="WeTube Premium"
            width={80}
            height={80}
            className="object-contain"
          />
          <span className="text-2xl font-bold text-gray-800">
            WeTube Premium
          </span>
        </div>

        <p className="text-4xl md:text-5xl font-bold mb-4 leading-tight">
          Enjoy the full experience of{" "}
          <span className="text-blue-600">WeTube Premium</span>
        </p>

        <p className="text-lg text-gray-700 mb-6">
          Experience WeTube and WeTube Music without ads, with background play,
          and offline viewing.
        </p>

        <p className="text-base text-gray-600 mb-2">
          Try 1 month for <strong>free</strong>. After that only{" "}
          <strong>₫49,000/month</strong> • VAT not included • Cancel anytime
        </p>

        <div className="mt-6">
          <PaymentButton/>
        </div>

        <p className="mt-4 text-sm text-blue-700 underline cursor-pointer hover:text-blue-900">
          Plans for family or students
        </p>
      </div>

      <div className="mt-16 grid grid-cols-1 md:grid-cols-2 gap-8 max-w-4xl w-full">
        {features.map((feature, index) => (
          <div
            key={index}
            className="flex items-start gap-4 text-left px-4"
          >
            <Image
              src={feature.icon}
              alt={feature.title}
              width={48}
              height={48} 
              className="object-contain"
            />
            <div>
              <h3 className="text-xl font-semibold text-gray-800">
                {feature.title}
              </h3>
              <p className="text-gray-600">{feature.desc}</p>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
