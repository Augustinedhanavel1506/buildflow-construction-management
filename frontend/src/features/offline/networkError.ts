/**
 * True when a request was sent but no response came back at all (offline, timeout,
 * DNS failure). False for anything the server actually responded to, including
 * error statuses — those are real validation/auth failures, not connectivity issues.
 */
export function isNetworkFailure(error: unknown): boolean {
  return (
    typeof error === "object" &&
    error !== null &&
    "request" in error &&
    !("response" in error && (error as { response?: unknown }).response)
  );
}
