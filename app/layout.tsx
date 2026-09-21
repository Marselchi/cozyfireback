import type React from "react";
import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { ThemeLayoutWrapper } from "@/components/navbar/theme-layout-wrapper";
import { ResponsiveToaster } from "@/components/utils/responsive-toaster";
import Providers from "@/components/providers/provider";

const inter = Inter({ subsets: ["cyrillic"] });

export const metadata: Metadata = {
  title: "Cozy Fireplace",
  description: "Ваши партии по НРИ. В одном месте",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ru" suppressHydrationWarning>
      <body>
        <ThemeLayoutWrapper>
          <ResponsiveToaster />
          <Providers>{children}</Providers>
        </ThemeLayoutWrapper>
      </body>
    </html>
  );
}
