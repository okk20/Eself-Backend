const jwt = require('jsonwebtoken');
const { config } = require('./config');
const { AuthenticationError } = require('./errors');
const logger = require('./logger');

function authenticateUser(req, res, next) {
  let userId = null;

  const authHeader = req.headers['authorization'];
  if (authHeader && authHeader.startsWith('Bearer ')) {
    const token = authHeader.split(' ')[1];
    try {
      const decoded = jwt.verify(token, config.jwtSecret);
      userId = decoded.userId || decoded.id || decoded.sub;
      if (userId) {
        logger.debug('Authenticated via JWT', { userId });
      }
    } catch (err) {
      logger.warn('JWT verification failed, trying fallback auth', {
        error: err.message,
      });
    }
  }

  if (!userId) {
    userId = req.headers['x-user-id'] || req.query.userId || req.body.userId;
    if (userId) {
      logger.debug('Authenticated via header/param', { userId });
    }
  }

  if (!userId) {
    throw new AuthenticationError(
      'Authentication required. Provide Authorization Bearer token, x-user-id header, or userId parameter.'
    );
  }

  req.userId = userId.toString();
  next();
}

module.exports = { authenticateUser };
