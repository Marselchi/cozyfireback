/// <reference lib="webworker" />
import { initializeApp } from "firebase/app";
import { getMessaging, onBackgroundMessage } from "firebase/messaging/sw";
import type { MessagePayload } from "firebase/messaging";

declare const self: ServiceWorkerGlobalScope;

const firebaseApp = initializeApp({
  apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY!,
  authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN!,
  projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID!,
  storageBucket: process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET!,
  messagingSenderId: process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID!,
  appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID!,
});

const messaging = getMessaging(firebaseApp);

self.addEventListener("notificationclick", (event: NotificationEvent) => {
  event.notification.close();
  const data = event.notification.data as Record<string, unknown>;
  const route = (data?.route as string) || "/";

  event.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((clientList) => {
      for (const client of clientList) {
        if ("focus" in client && client.url.includes(route)) return client.focus();
      }
      return self.clients.openWindow(route);
    })
  );
});

onBackgroundMessage(messaging, (payload: MessagePayload) => {
  const data = (payload.data || {}) as Record<string, string>;
  const route = "/rooms" + data.route || ""
  self.registration.showNotification(data.title || "Уведомление", {
    body: data.body || "",
    // icon: "/icons/icon-192.png",
    // badge: "/icons/badge.png",
    data: { route: route, type: data.type || "" },
  });
});