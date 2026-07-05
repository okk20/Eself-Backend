class AppError extends Error {
  constructor(message, statusCode = 500, code = 'INTERNAL_ERROR') {
    super(message);
    this.name = this.constructor.name;
    this.statusCode = statusCode;
    this.code = code;
    Error.captureStackTrace(this, this.constructor);
  }

  toJSON() {
    return {
      success: false,
      error: this.message,
      code: this.code,
    };
  }
}

class ValidationError extends AppError {
  constructor(message) {
    super(message, 400, 'VALIDATION_ERROR');
  }
}

class AuthenticationError extends AppError {
  constructor(message = 'Authentication required') {
    super(message, 401, 'AUTHENTICATION_ERROR');
  }
}

class NotFoundError extends AppError {
  constructor(message = 'Resource not found') {
    super(message, 404, 'NOT_FOUND');
  }
}

class DuplicateError extends AppError {
  constructor(message = 'Resource already exists') {
    super(message, 409, 'DUPLICATE_ERROR');
  }
}

class PaystackError extends AppError {
  constructor(message, statusCode = 502) {
    super(message, statusCode, 'PAYSTACK_ERROR');
  }
}

module.exports = {
  AppError,
  ValidationError,
  AuthenticationError,
  NotFoundError,
  DuplicateError,
  PaystackError,
};
