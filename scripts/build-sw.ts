// scripts/build-sw.ts
import { build } from "esbuild";
import { mkdirSync } from "fs";
import { join } from "path";

const outDir = join(process.cwd(), "public");
mkdirSync(outDir, { recursive: true });

console.log("🔨 Building Service Worker...");

build({
  entryPoints: ["lib/firebase-messaging-sw.ts"],
  outfile: join(outDir, "firebase-messaging-sw.js"),
  bundle: true,
  minify: true, 
  target: ["esnext"], // Используем современный синтаксис
  define: {
    "process.env.NEXT_PUBLIC_FIREBASE_API_KEY": JSON.stringify(process.env.NEXT_PUBLIC_FIREBASE_API_KEY),
    "process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN": JSON.stringify(process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN),
    "process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID": JSON.stringify(process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID),
    "process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET": JSON.stringify(process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET),
    "process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID": JSON.stringify(process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID),
    "process.env.NEXT_PUBLIC_FIREBASE_APP_ID": JSON.stringify(process.env.NEXT_PUBLIC_FIREBASE_APP_ID),
  },
  platform: "browser",
  format: "esm", // Важно: ES Module
})
.then(() => console.log("✅ Service Worker built successfully"))
.catch((e) => {
  console.error("❌ Service Worker build failed:", e);
  process.exit(1);
});