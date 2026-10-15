"use client";
import { useState } from "react";
import { ChevronDown } from "lucide-react";

const faqs = [
  {
    question: "What does the YouTube Premium plan include?",
    answer: (
      <>
        <p>
          As a YouTube Premium member, you can watch videos without ads on
          YouTube. In addition, you can download videos to watch offline and
          play videos in the background while using other apps.
        </p>
        <p className="mt-2">
          The YouTube Premium membership also includes YouTube Music Premium.
          Download the YouTube Music app to enjoy over 100 million songs without
          ads, offline, and with your screen locked.
        </p>
        <p className="mt-2">
          You can also watch ad-free videos on the YouTube Kids app.
        </p>
      </>
    ),
  },
  {
    question: "How can I download videos and music?",
    answer: (
      <>
        <p>
          You can download videos/music to your mobile device when using the
          YouTube, YouTube Music, or YouTube Kids apps. On desktop, you can also
          watch and download videos using Chrome, Edge, or Opera browsers.
        </p>
        <p className="mt-2">
          You can view or listen offline for up to 30 days without an Internet
          connection.
        </p>
        <a
          href="https://support.google.com/youtube/answer/6141269"
          target="_blank"
          rel="noopener noreferrer"
          className="block mt-2 text-blue-600 underline"
        >
          Learn more about watching offline
        </a>
      </>
    ),
  },
  {
    question: "How can I add others to my plan?",
    answer: (
      <>
        <p>
          You can add members to your YouTube Premium Family plan to share with
          up to 5 other members. When you purchase the family plan, you can add
          members if you are the family manager.
        </p>
        <p className="mt-2">
          Family members must live in the same household or have a Google
          account within the family group.
        </p>
        <a
          href="https://support.google.com/youtube/answer/6305528"
          target="_blank"
          rel="noopener noreferrer"
          className="block mt-2 text-blue-600 underline"
        >
          Learn more about the Family plan
        </a>
      </>
    ),
  },
  {
    question: "How can I play music and videos in the background?",
    answer: (
      <>
        <p>
          With YouTube Premium, background play is enabled by default on
          YouTube, YouTube Music, and YouTube Kids. This means if you’re
          watching a video on YouTube and switch to another app, the video will
          keep playing in the background until you pause it.
        </p>
        <p className="mt-2">You can turn off background play in settings.</p>
        <a
          href="https://support.google.com/youtube/answer/7548453"
          target="_blank"
          rel="noopener noreferrer"
          className="block mt-2 text-blue-600 underline"
        >
          Learn more about Background Play
        </a>
      </>
    ),
  },
  {
    question: "How is YouTube Premium different from YouTube Music Premium?",
    answer: (
      <>
        <p>
          With YouTube Premium, you can watch videos on the YouTube app without
          ads, offline, and in the background.
        </p>
        <p className="mt-2">
          The YouTube Premium membership also includes YouTube Music Premium.
          Download the YouTube Music app to enjoy over 100 million songs without
          ads, offline, and with your screen locked.
        </p>
        <a
          href="https://support.google.com/youtube/answer/6305537"
          target="_blank"
          rel="noopener noreferrer"
          className="block mt-2 text-blue-600 underline"
        >
          Learn more about YouTube Music
        </a>
      </>
    ),
  },
  {
    question: "How can I cancel my membership?",
    answer: (
      <>
        <p>
          You can cancel your membership by going to{" "}
          <a
            href="https://www.youtube.com/paid_memberships"
            target="_blank"
            rel="noopener noreferrer"
            className="text-blue-600 underline"
          >
            Paid Memberships
          </a>
          . You can rejoin YouTube Premium anytime.
        </p>
      </>
    ),
  },
];

export default function PremiumFaq() {
  const [openIndex, setOpenIndex] = useState<number | null>(0);

  const toggle = (index: number) => {
    setOpenIndex(openIndex === index ? null : index);
  };

  return (
    <section className="w-full max-w-7xl bg-gray-100 rounded-4xl p-6 sm:p-10 mx-auto my-12">
      <div className="text-left">
        <h2 className="text-4xl font-bold mb-6">Answering your questions</h2>

        <div className="divide-y divide-gray-300">
          {faqs.map((faq, index) => (
            <div key={index}>
              <button
                onClick={() => toggle(index)}
                className="w-full text-left py-5 flex justify-between items-center font-medium text-lg"
              >
                <span>{faq.question}</span>
                <ChevronDown
                  className={`w-5 h-5 transform ${
                    openIndex === index ? "rotate-180" : ""
                  }`}
                />
              </button>

              {openIndex === index && (
                <div className="pl-1 pb-4 text-sm text-gray-700">
                  {faq.answer}
                </div>
              )}
            </div>
          ))}
        </div>

        <div className="mt-6 text-sm">
          Still have questions?{" "}
          <a
            href="https://support.google.com/youtube"
            target="_blank"
            rel="noopener noreferrer"
            className="text-blue-600 underline"
          >
            YouTube Help Center
          </a>
        </div>
      </div>
    </section>
  );
}
