import Link from "next/link";
import { ComponentProps } from "react";

export default function CustomLink(props: ComponentProps<typeof Link>) {
  // trying the prefetch
  return (
    <Link {...props} prefetch={false}>
      {props.children}
    </Link>
  );
}
