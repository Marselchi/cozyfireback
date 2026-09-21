import Image from "next/image";
import { cn } from "@/lib/utils";

interface ImageWithCaptionProps {
  src: string;
  alt: string;
  /** Optional caption rendered below the image. */
  caption?: string;
  width?: number;
  height?: number;
  className?: string;
}

export function ImageWithCaption({
  src,
  alt,
  caption,
  width = 1200,
  height = 630,
  className,
}: Readonly<ImageWithCaptionProps>) {
  return (
    <figure className={cn("space-y-2.5", className)}>
      <div
        className="overflow-hidden rounded-lg border border-border bg-muted relative mx-auto"
        style={{ width, height }}
      >
        <Image src={src} alt={alt} fill className="object-contain" />
      </div>
      {caption && (
        <figcaption className="text-center text-xs text-muted-foreground leading-relaxed px-4">
          {caption}
        </figcaption>
      )}
    </figure>
  );
}
