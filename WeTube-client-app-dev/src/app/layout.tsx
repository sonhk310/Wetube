import Providers from "@/providers";
import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import React, { Suspense } from "react";
import "./globals.css";
import { AuthProvider } from "@/contexts/auth-context";
import { LoadingBarProvider } from "@/contexts/loading-bar-context";
import { LoadingBar } from "@/components/LoadingBar";
import { NavigationEvents } from "@/components/NavigationEvents";
import { Bounce, ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "WeTube",
  description: "Stream and Share Videos Seamlessly",
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
      lang="en"
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
              <ToastContainer
                position="top-center"
                autoClose={3000}
                hideProgressBar={false}
                newestOnTop={false}
                closeOnClick={false}
                rtl={false}
                pauseOnFocusLoss={false}
                draggable={false}
                pauseOnHover={false}
                theme="colored"
                transition={Bounce}
              />
            </Providers>
          </AuthProvider>
        </LoadingBarProvider>
      </body>
    </html>
  );
}
