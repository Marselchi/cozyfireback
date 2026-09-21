"use client";

import { cloneElement, isValidElement, useEffect, useState } from "react";

export function FreshKeyWrapper({ children }: { children: React.ReactNode }) {
  const [fresh, setFresh] = useState<string | null>(null);

  useEffect(() => {
    setFresh(crypto.randomUUID());
  }, []);

  if (!fresh || !isValidElement(children)) {
    return <>{children}</>;
  }

  return cloneElement(children, { key: fresh });
}
