"use client";

import { useActionState, useEffect, useRef } from "react";
import { useFormStatus } from "react-dom";
import { CheckCircle2, Loader2, XCircle } from "lucide-react";

import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { createFeedback, FeedbackState } from "@/server/user/feedback";

export const initialFeedbackState: FeedbackState = {
  status: "idle",
  message: "",
};
interface FeedbackFormProps {
  /**
   * Identifies where this form is used, e.g. "pricing-page",
   * "dashboard-sidebar", "checkout-flow". Sent to the server action
   * alongside the message so you can tell submissions apart.
   */
  source: string;
  /** Heading shown above the field. */
  title?: string;
  /** Optional supporting copy shown under the title. */
  description?: string;
  /** Placeholder text for the textarea. */
  placeholder?: string;
  /** Label for the submit button. */
  buttonText?: string;
  /** Extra classes for the outer card, e.g. to constrain width. */
  className?: string;
}

function SubmitButton({ label }: Readonly<{ label: string }>) {
  const { pending } = useFormStatus();

  return (
    <Button type="submit" disabled={pending} className="w-full sm:w-auto">
      {pending ? (
        <>
          <Loader2 className="size-4 animate-spin" />
          Отправляю...
        </>
      ) : (
        label
      )}
    </Button>
  );
}

export function FeedbackForm({
  source,
  title = "Фидбек",
  description,
  placeholder = "И че и че",
  buttonText = "Отправить",
  className,
}: Readonly<FeedbackFormProps>) {
  const [state, formAction] = useActionState(
    createFeedback,
    initialFeedbackState,
  );
  const formRef = useRef<HTMLFormElement>(null);

  // Clear the textarea once a submission succeeds.
  useEffect(() => {
    if (state.status === "success") {
      formRef.current?.reset();
    }
  }, [state.status]);

  return (
    <Card className={cn("w-full max-w-md", className)}>
      <CardHeader>
        <CardTitle className="text-lg sm:text-xl">{title}</CardTitle>
        {description ? <CardDescription>{description}</CardDescription> : null}
      </CardHeader>

      <form ref={formRef} action={formAction}>
        <CardContent className="space-y-3">
          <input type="hidden" name="source" value={source} />

          <Textarea
            name="content"
            placeholder={placeholder}
            required
            rows={4}
            maxLength={2000}
            aria-label={title}
            aria-describedby={
              state.message ? "feedback-form-status" : undefined
            }
            className="resize-none"
          />

          {state.status !== "idle" ? (
            <div
              id="feedback-form-status"
              role="status"
              aria-live="polite"
              className={cn(
                "flex items-start gap-2 rounded-md border px-3 py-2 text-sm",
                state.status === "success" &&
                  "border-emerald-600/20 bg-emerald-50 text-emerald-700 dark:border-emerald-400/20 dark:bg-emerald-950/40 dark:text-emerald-400",
                state.status === "error" &&
                  "border-destructive/20 bg-destructive/5 text-destructive dark:bg-destructive/10",
              )}
            >
              {state.status === "success" ? (
                <CheckCircle2 className="mt-0.5 size-4 shrink-0" />
              ) : (
                <XCircle className="mt-0.5 size-4 shrink-0" />
              )}
              <span>{state.message}</span>
            </div>
          ) : null}
        </CardContent>

        <CardFooter className="flex justify-end">
          <SubmitButton label={buttonText} />
        </CardFooter>
      </form>
    </Card>
  );
}
