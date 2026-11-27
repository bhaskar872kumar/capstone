/** Turns an axios error into a message for the user, using the API's ProblemDetail "detail". */
export function errorMessage(error, fallback = 'Something went wrong. Please try again.') {
  if (!error) return fallback;
  if (error.response) {
    const detail = error.response.data?.detail;
    return typeof detail === 'string' && detail.trim() ? detail : fallback;
  }
  if (error.code === 'ECONNABORTED') return 'The server took too long to answer. Please try again.';
  if (error.request) return "Can't reach the server. Check your connection and try again.";
  return fallback;
}

/** Field errors from a 400 validation ProblemDetail: { fieldName: message }. */
export function fieldErrors(error) {
  return error?.response?.data?.errors ?? {};
}
