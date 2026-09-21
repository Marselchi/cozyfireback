import { Suspense } from "react";
import { AdminSubNav } from "@/components/admin/sub-nav";

// AdminSubNav calls usePathname(), which Next.js requires to be inside a
// Suspense boundary when it's read outside of one at the route root
// ("found... usePathname() in a Client Component outside of Suspense").
// The fallback below mirrors AdminSubNav's own height (h-12) so there's no
// layout jump while it streams in.
function AdminSubNavFallback() {
  return <div className="h-12 border-b border-border bg-card" />;
}

export default function AdminLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <div className="flex h-[calc(100vh-var(--navbar-height,4rem))] flex-col">
      <Suspense fallback={<AdminSubNavFallback />}>
        <AdminSubNav />
      </Suspense>
      <div className="min-h-0 flex-1">{children}</div>
    </div>
  );
}
