"use client";

import { useRef, useState } from "react";
import { MessageSquareText, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useQuoteHighlight } from "@/hooks/use-quote-highlight";
import { cn } from "@/lib/utils";
import { TextSelectionPopup } from "./questions/text-selection-popup";
import { QuestionPanel } from "./questions/question-panel";
import { Badge } from "../ui/badge";

interface LoreViewProps {
  loreId: number;
  questionCount: number;
  headerSlot: React.ReactNode;
  contentSlot: React.ReactNode;
}

export function LoreView({
  loreId,
  questionCount,
  headerSlot,
  contentSlot,
}: Readonly<LoreViewProps>) {
  const [questionMode, setQuestionMode] = useState(false);
  const loreContainerRef = useRef<HTMLDivElement>(null);
  const { scrollToQuote } = useQuoteHighlight(loreContainerRef);

  const handleQuoteClick = (quoteText: string) => {
    scrollToQuote(quoteText);
  };

  return (
    <div className="min-h-screen">
      {/* Question mode toggle button */}
      <div className="fixed right-4 bottom-4 z-40">
        <Button
          onClick={() => setQuestionMode(!questionMode)}
          variant={questionMode ? "default" : "outline"}
          size="sm"
          className={cn(
            "gap-2 shadow-md transition-all",
            questionMode && "bg-primary text-primary-foreground",
          )}
        >
          {questionMode ? (
            <>
              <X className="w-4 h-4" />
              Закрыть вопросы
            </>
          ) : (
            <div className="relative inline-flex items-center">
              <MessageSquareText className="w-4 h-4" />
              <span>Вопросы</span>

              {questionCount > 0 && (
                <Badge className="absolute -top-3 -left-5 h-5 w-5 p-0 flex items-center justify-center text-sm pointer-events-none z-10">
                  {questionCount}
                </Badge>
              )}
            </div>
          )}
        </Button>
      </div>

      <div
        className={cn(
          "transition-all duration-300 ease-in-out",
          questionMode ? "flex gap-0" : "",
        )}
      >
        {/* Lore section */}
        <div
          className={cn(
            "transition-all duration-300 ease-in-out",
            questionMode
              ? "w-1/2 border-r border-border min-h-screen"
              : "w-full",
          )}
        >
          <div
            className={cn(
              "mx-auto px-4 py-8 relative",
              questionMode ? "max-w-none" : "max-w-4xl",
            )}
          >
            <div ref={loreContainerRef} className="relative">
              {headerSlot}
              {contentSlot}
              <TextSelectionPopup
                containerRef={loreContainerRef}
                enabled={questionMode}
              />
            </div>
          </div>
        </div>

        {/* Question panel - slides in from the right */}
        {questionMode && (
          <div
            className="w-1/2 h-[calc(100vh-var(--navbar-height))] border-l border-input bg-background sticky overflow-hidden animate-in slide-in-from-right-5 duration-300"
            style={{ top: "var(--navbar-height, 0px)" }}
          >
            <QuestionPanel
              loreId={loreId.toString()}
              onQuoteClick={handleQuoteClick}
            />
          </div>
        )}
      </div>
    </div>
  );
}
