import Providers from "@/providers";
import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import React, { Suspense } from "react";
import "./globals.css";
import { AuthProvider } from "@/contexts/auth-context";
import { LoadingBarProvider } from "@/contexts/loading-bar-context";
import { LoadingBar } from "@/components/LoadingBar";
import { NavigationEvents } from "@/components/NavigationEvents";
import { Toaster } from "@/components/ui/sonner";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  // Gốc cho các URL tương đối trong thẻ chia sẻ (og:url, canonical)
  metadataBase: new URL(process.env.NEXT_PUBLIC_SITE_URL || "https://wetube.vibe.tinhgon.xyz"),
  title: "WeTube",
  description: "Xem và chia sẻ video thật dễ dàng",
  openGraph: {
    siteName: "WeTube",
    locale: "vi_VN",
    title: "WeTube",
    description: "Xem và chia sẻ video thật dễ dàng",
  },
  icons: {
    icon: [{ url: "/favicon.ico" }],
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="vi"
      className="mdl-js"
    >
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased`}
      >
        <LoadingBarProvider>
          <AuthProvider>
            <Providers>
              <LoadingBar />
              <Suspense fallback={null}>
                <NavigationEvents />
              </Suspense>
              {children}
              {/* Toast của shadcn (Sonner), giữa cạnh dưới; gọi qua lib/toastEmitter hoặc toast của "sonner" */}
              <Toaster position="bottom-center" />
            </Providers>
          </AuthProvider>
        </LoadingBarProvider>
      </body>
    </html>
  );
}
