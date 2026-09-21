"use server";

import { sendWithAuth } from "@/lib/auth/apiClient";

export type FeedbackState = {
  status: "idle" | "success" | "error";
  message: string;
};

/**
 * Server action for the <FeedbackForm /> component.
 *
 * Receives:
 *  - source:  which part of the app the feedback came from (passed as a
 *             hidden field by the component, set via the `source` prop)
 *  - content: the text the user typed
 *
 * Posts to the /polls endpoint via sendWithAuth. Adjust the endpoint/
 * payload shape to match your API.
 */
export async function createFeedback(
  _prevState: FeedbackState,
  formData: FormData,
): Promise<FeedbackState> {
  const source = formData.get("source");
  const content = formData.get("content");

  // Validate before it ever reaches the network — formData.get() returns
  // FormDataEntryValue | null, not string, so this also narrows the type.
  if (typeof content !== "string" || content.trim().length === 0) {
    return {
      status: "error",
      message: "Напиши хоть что-то перед отправкой.",
    };
  }

  if (content.trim().length > 2000) {
    return {
      status: "error",
      message: "Чет ты дохрена высрал братанчик.",
    };
  }

  if (typeof source !== "string" || source.trim().length === 0) {
    return {
      status: "error",
      message: "Это мой косяк, напиши мне.",
    };
  }

  const [error] = await sendWithAuth("/polls", "POST", {
    source,
    content: content.trim(),
  });

  if (error) {
    console.error("Failed to submit feedback:", error);
    return {
      status: "error",
      message:
        "Чет не то пошло, либо ты не авторизованный :), От таких мне фидбек не нужен",
    };
  }

  return {
    status: "success",
    message: "Пасиба, отправлено.",
  };
}
