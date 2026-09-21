"use client";

import { Component, type ReactNode } from "react";
import { AlertCircle } from "lucide-react";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";

interface ErrorBoundaryProps {
  children: ReactNode;
  fallbackTitle?: string;
}

interface ErrorBoundaryState {
  error: Error | null;
}

export class ErrorBoundary extends Component<
  ErrorBoundaryProps,
  ErrorBoundaryState
> {
  state: ErrorBoundaryState = { error: null };

  static getDerivedStateFromError(error: Error) {
    return { error };
  }

  reset = () => {
    this.setState({ error: null });
  };

  render() {
    if (this.state.error) {
      return (
        <Alert variant="destructive">
          <AlertCircle className="size-4" />
          <AlertTitle>
            {this.props.fallbackTitle ?? "Something went wrong"}
          </AlertTitle>
          <AlertDescription className="flex flex-col gap-3">
            <span>
              {this.state.error.message || "Unable to load this data."}
            </span>
            <Button
              size="sm"
              variant="outline"
              onClick={this.reset}
              className="w-fit"
            >
              Попробуйте еще разок
            </Button>
          </AlertDescription>
        </Alert>
      );
    }

    return this.props.children;
  }
}
