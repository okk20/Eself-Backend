const SENSITIVE_PATTERNS = [
  /sk_(test|live)_[a-zA-Z0-9]+/g,
  /pk_(test|live)_[a-zA-Z0-9]+/g,
  /Bearer\s+[a-zA-Z0-9._-]+/g,
  /"card[^"]*":\s*"[^"]+"/gi,
  /"cvv[^"]*":\s*"[^"]+"/gi,
  /"pin[^"]*":\s*"[^"]+"/gi,
  /(password|secret|token|key)\s*[:=]\s*['"][^'"]+['"]/gi,
];

function redact(str) {
  if (typeof str !== 'string') return str;
  let result = str;
  for (const pattern of SENSITIVE_PATTERNS) {
    result = result.replace(pattern, (match) => {
      if (match.startsWith('"card') || match.startsWith('"cvv') || match.startsWith('"pin')) {
        return match.replace(/:"[^"]+"/, ': "[REDACTED]"');
      }
      if (/password|secret|token|key/i.test(match)) {
        return match.replace(/['"][^'"]+['"]/, ' "[REDACTED]"');
      }
      return match.substring(0, Math.min(8, match.indexOf('_'))) + '****';
    });
  }
  return result;
}

function formatMessage(level, message, meta) {
  const timestamp = new Date().toISOString();
  const metaStr = meta ? ' ' + JSON.stringify(redact(JSON.stringify(meta))) : '';
  return `[${timestamp}] [${level}] ${redact(message)}${metaStr}`;
}

const logger = {
  info: (message, meta) => {
    console.log(formatMessage('INFO', message, meta));
  },
  warn: (message, meta) => {
    console.warn(formatMessage('WARN', message, meta));
  },
  error: (message, meta) => {
    console.error(formatMessage('ERROR', message, meta));
  },
  debug: (message, meta) => {
    if (process.env.NODE_ENV !== 'production' || process.env.DEBUG) {
      console.debug(formatMessage('DEBUG', message, meta));
    }
  },
  audit: (action, details) => {
    const entry = {
      timestamp: new Date().toISOString(),
      action,
      details: typeof details === 'object' ? { ...details } : details,
    };
    if (entry.details && entry.details.paymentReference) {
      entry.details.paymentReference = entry.details.paymentReference;
    }
    if (entry.details && entry.details.userId) {
      entry.details.userId = entry.details.userId;
    }
    const safe = JSON.parse(redact(JSON.stringify(entry)));
    console.log(`[AUDIT] ${JSON.stringify(safe)}`);
    return safe;
  },
};

module.exports = logger;
