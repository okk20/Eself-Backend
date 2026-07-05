# 👑 Secure Payment Infrastructure & Deployment Guide

This project has been refactored to follow **Payment Security Best Practices** by removing all Paystack Secret Keys from the Android application and migrating them entirely to a secure Node.js Express backend. 

The Android app now operates with zero sensitive keys, utilizing only the **Paystack Public Key** to launch payment sessions client-side and relying on the **Express Backend** for secure, tamper-proof payment verification.

---

## 🛠️ System Architecture

1. **Transaction Initialization (Android)**: The user starts checkout. The app generates a secure inline Paystack iframe form loaded in a web bridge with the non-sensitive **Paystack Public Key** (`PAYSTACK_PUBLIC_KEY`).
2. **Payment Completion (Paystack Gateway)**: The user enters mobile money or card details and authorizes payment. Paystack returns a unique `transactionReference` to the secure Android client bridge.
3. **Secure Verification Call (Backend Request)**: The Android app intercepts the reference and transmits it along with the user's ID (`studentId`) to the secure backend endpoint: `POST /payments/verify`.
4. **Server-Side Verification (Paystack API)**: The Express Server verifies the transaction directly with Paystack’s official servers using the **Paystack Secret Key** (`PAYSTACK_SECRET_KEY`) hosted securely in the backend environment.
5. **Database Logging**: Upon successful verification, the backend ensures the reference is **unique** (preventing duplicate validation attacks), stores the transaction in the database, and marks the user as `isPremium = true`.
6. **Feature Activation**: The backend returns a secure status: `{"success": true, "premium": true}`. The Android app receives this response, unlocks lifetime offline premium features, and registers purchase tokens.

---

## 🚀 Backend Deployment Instructions

The backend is built with lightweight, enterprise-grade Node.js and Express. It requires zero configuration of heavy database engines by default, utilizing a safe, persistent, and fast JSON file-based database store.

### 📦 Option 1: Quick Local Deployment

To run the backend server on your development machine:

1. **Navigate to the server directory**:
   ```bash
   cd server
   ```

2. **Install dependencies**:
   ```bash
   npm install
   ```

3. **Configure Environment Variables**:
   Create a `.env` file from the example:
   ```bash
   cp .env.example .env
   ```
   Open the `.env` file and fill in your Paystack Secret Key:
   ```env
   PORT=3000
   PAYSTACK_SECRET_KEY=sk_test_your_actual_paystack_secret_key_here
   PAYSTACK_PUBLIC_KEY=pk_test_your_actual_paystack_public_key_here
   JWT_SECRET=super_secret_session_token_key_change_me
   ```

4. **Start the server**:
   * For development (with hot reload):
     ```bash
     npm run dev
     ```
   * For production:
     ```bash
     npm start
     ```
   The backend will be live at `http://localhost:3000`.

---

### 🌐 Option 2: Cloud Deployment (Render, Heroku, or Railway)

You can host your Node.js backend on standard cloud platforms for free.

#### Deploying on Render (Recommended)
1. Sign in to [Render](https://render.com/).
2. Click **New +** and select **Web Service**.
3. Connect your GitHub repository containing the project.
4. Set the following details:
   * **Root Directory**: `server`
   * **Runtime**: `Node`
   * **Build Command**: `npm install`
   * **Start Command**: `npm start`
5. Under **Environment Variables**, add the keys from your `.env` file:
   * `PAYSTACK_SECRET_KEY` = `sk_test_...`
   * `JWT_SECRET` = `(generate a random secure string)`
6. Click **Deploy Web Service**. Render will generate a public URL like `https://eself-payment.onrender.com`.

---

## 🔐 Android App Integration

To wire the Android application to your live server:

1. Open the **Secrets panel** in the Google AI Studio UI.
2. Configure the following environment variables:
   * **`PAYSTACK_PUBLIC_KEY`**: Set to your Paystack Test/Live Public Key (`pk_test_...`).
   * **`PAYSTACK_BACKEND_URL`**: Set to your deployed backend base URL (e.g., `https://eself-payment.onrender.com`).
3. Build the application. The secrets plugin will automatically inject these variables into the secure Android `BuildConfig` at compile-time.

---

## 🗄️ Database Model / Schema

Transactions are saved in the backend utilizing a clean schema. If you decide to transition from file storage to a cloud PostgreSQL database, map your tables directly to this schema:

```sql
CREATE TABLE purchases (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    payment_reference VARCHAR(150) UNIQUE NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'GHS',
    payment_status VARCHAR(50) DEFAULT 'success',
    purchase_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_premium BOOLEAN DEFAULT TRUE
);

CREATE TABLE premium_users (
    user_id VARCHAR(100) PRIMARY KEY,
    is_premium BOOLEAN DEFAULT TRUE,
    unlocked_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

---

## 🛡️ Security Guarantees & Features

* **Anti-Cheat Validation**: No local system timing or mock actions can bypass verification. The Android client only turns on Premium after obtaining a successful signed payload from the backend.
* **Double Spend Protection**: The file-based JSON store (and SQL schema) enforce a unique constraint on `paymentReference`. Attempting to verify a reference twice will abort immediately, neutralizing replay attacks.
* **Network Fail-Safes**: Built with try-catch blocks and explicit response status checks to handle network dropouts and gateway errors safely without crashing the client app.
