import { initializeApp } from "firebase/app";
import { getMessaging, isSupported } from "firebase/messaging";

const firebaseConfig = {
  apiKey: "AIzaSyBOIOwxe8A-C60FaVq8WTHunHbD8ujXXtg",
  authDomain: "cozyfireplace.firebaseapp.com",
  projectId: "cozyfireplace",
  storageBucket: "cozyfireplace.firebasestorage.app",
  messagingSenderId: "77687187760",
  appId: "1:77687187760:web:10b3bd3b54bd3e2bf8c0f5",
  measurementId: "G-CBSR21GGQF"
};

const app = initializeApp(firebaseConfig);

export async function getMessagingSafe() {
  const supported = await isSupported();
  if (!supported) return null;
  return getMessaging(app);
}