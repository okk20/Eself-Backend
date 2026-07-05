const fs = require('fs');
const path = require('path');

const DB_DIR = path.join(__dirname, 'data');
const DB_FILE = path.join(DB_DIR, 'db.json');

// Ensure database directory and file exist
if (!fs.existsSync(DB_DIR)) {
  fs.mkdirSync(DB_DIR, { recursive: true });
}

if (!fs.existsSync(DB_FILE)) {
  fs.writeFileSync(DB_FILE, JSON.stringify({ purchases: [], premiumUsers: {} }, null, 2));
}

/**
 * Read the current state of the database
 */
function readDb() {
  try {
    const data = fs.readFileSync(DB_FILE, 'utf8');
    return JSON.parse(data);
  } catch (err) {
    console.error('Error reading JSON database:', err);
    return { purchases: [], premiumUsers: {} };
  }
}

/**
 * Write state back to the database
 */
function writeDb(data) {
  try {
    fs.writeFileSync(DB_FILE, JSON.stringify(data, null, 2), 'utf8');
    return true;
  } catch (err) {
    console.error('Error writing to JSON database:', err);
    return false;
  }
}

module.exports = {
  /**
   * Save a verified purchase transaction
   * @param {Object} purchaseData 
   * @returns {Object} Saved purchase or error
   */
  savePurchase: (purchaseData) => {
    const db = readDb();
    
    // Validate required fields
    const { userId, paymentReference, amount, currency, paymentStatus } = purchaseData;
    if (!userId || !paymentReference) {
      throw new Error('userId and paymentReference are required');
    }

    // Check for duplicate payment reference to prevent re-processing
    const isDuplicate = db.purchases.some(p => p.paymentReference === paymentReference);
    if (isDuplicate) {
      throw new Error(`Duplicate reference: Payment reference ${paymentReference} has already been processed.`);
    }

    const newPurchase = {
      id: db.purchases.length + 1,
      userId: userId.toString(),
      paymentReference,
      amount: parseFloat(amount) || 0,
      currency: currency || 'GHS',
      paymentStatus: paymentStatus || 'success',
      purchaseDate: new Date().toISOString(),
      isPremium: true
    };

    db.purchases.push(newPurchase);
    
    // Mark user as premium
    db.premiumUsers[userId.toString()] = {
      isPremium: true,
      unlockedAt: new Date().toISOString()
    };

    writeDb(db);
    return newPurchase;
  },

  /**
   * Get premium status of a user
   * @param {string|number} userId 
   * @returns {boolean}
   */
  getPremiumStatus: (userId) => {
    if (!userId) return false;
    const db = readDb();
    return !!db.premiumUsers[userId.toString()]?.isPremium;
  },

  /**
   * Check if a transaction reference has already been processed
   * @param {string} reference 
   * @returns {boolean}
   */
  hasReferenceBeenProcessed: (reference) => {
    if (!reference) return false;
    const db = readDb();
    return db.purchases.some(p => p.paymentReference === reference);
  }
};
