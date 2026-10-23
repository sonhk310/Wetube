import "../globals.css";

import { Theme, ThemePanel } from "@radix-ui/themes";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Phát trực tiếp với WeTube",
};

export default function RootLayout({
  children,
}: {
  readonly children: React.ReactNode;
}) {
  return (
    <Theme
      appearance="light"
      accentColor="indigo"
      grayColor="sand"
      radius="large"
    >
      {children}
      <ThemePanel defaultOpen={false} />
    </Theme>
  );
}
