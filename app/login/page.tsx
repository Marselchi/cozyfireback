import LoginClient from "@/components/login-page";
import { Suspense } from "react";

export default function LoginPage() {
  return (
    <Suspense fallback={null}>
      <LoginClient />
    </Suspense>
  );
}
