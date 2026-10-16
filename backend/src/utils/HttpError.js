// An error that carries an HTTP status code (400 = bad input, 500 = our bug ...).
// The error handler in errorHandler.js turns it into a JSON response.
export class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}
