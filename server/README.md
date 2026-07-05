# Eself Paystack Payment Integration Backend

This is a lightweight Node.js + Express backend designed to securely communicate with the Paystack API on behalf of your Android application. It manages transaction initializations, verification of payment references, and processes real-time webhook callbacks from Paystack.

By routing all Paystack API calls through this server, your **Paystack Secret Key is kept securely on the server** and is never compiled into or exposed in your Android APK.

---

## Endpoints

1. **`POST /payments/initialize`**
   * **Purpose:** Initiates a Paystack checkout transaction.
   * **Request Body:**
     ```json
     {
       "email": "student@example.com",
       "amount": 20.00,
       "studentId": 1
     }
     ```
   * **Response:**
     ```json
     {
       "success": true,
       "authorization_url": "https://checkout.paystack.com/abcdefg...",
       "reference": "...",
       "access_code": "..."
     }
     ```

2. **`GET /payments/verify/:reference`**
   * **Purpose:** Verifies whether a payment with the given reference was successful.
   * **Response:**
     ```json
     {
       "success": true,
       "status": "success",
       "reference": "...",
       "amount": 20.00,
       "studentId": 1
     }
     ```

3. **`POST /payments/webhook`**
   * **Purpose:** Receives event webhooks directly from Paystack (e.g. `charge.success`) and verifies payloads using HMAC SHA512 signatures.

---

## Local Setup & Development

1. Open your terminal in the `server` directory:
   ```bash
   cd server
   ```

2. Install the dependencies:
   ```bash
   npm install
   ```

3. Create your `.env` configuration:
   ```bash
   cp .env.example .env
   ```
   Open the `.env` file and replace the placeholder `sk_test_...` with your actual **Paystack Secret Key** (found under **Settings -> API Keys & Webhooks** in your Paystack dashboard).

4. Run the server locally:
   ```bash
   npm start
   ```
   The backend will be live at: `http://localhost:3000`

---

## Deployment Guide

Deploying this backend to a hosting service is quick and completely free on most tiers. Below are step-by-step instructions for the two most popular hosting choices.

### Option A: Render (Recommended, Free & Fast)

1. **Sign Up:** Go to [Render](https://render.com/) and sign up for a free account.
2. **New Web Service:** In the Render dashboard, click **New** -> **Web Service**.
3. **Connect Repository:** Link your GitHub/GitLab account and select your repository containing this project.
4. **Configure Service Settings:**
   * **Name:** `eself-paystack-backend`
   * **Environment:** `Node`
   * **Region:** Choose a region closest to your main user base (e.g., Europe or US).
   * **Branch:** `main` (or your active branch name).
   * **Root Directory:** `server` (This makes Render compile and run *only* the contents of the `/server` folder).
   * **Build Command:** `npm install`
   * **Start Command:** `npm start`
5. **Configure Environment Variables:**
   * Scroll down to the **Environment Variables** section (or select the **Env Groups / Environment** tab).
   * Add a new environment variable:
     * **Key:** `PAYSTACK_SECRET_KEY`
     * **Value:** *Your actual Paystack Secret Key (e.g., `sk_test_...` or `sk_live_...`)*
6. **Deploy:** Click **Create Web Service**. Within a couple of minutes, your server will be built and deployed! Render will provide you with a public URL (e.g., `https://eself-paystack-backend.onrender.com`).

---

### Option B: Railway (Extremely Lightweight)

1. **Sign Up:** Go to [Railway](https://railway.app/) and sign up.
2. **New Project:** Click **New Project** -> **Deploy from GitHub repo**.
3. **Select Repository:** Choose your repository and select the branch.
4. **Variables (Important):**
   * In the Railway project panel, go to **Variables**.
   * Add the variable `PAYSTACK_SECRET_KEY` and paste your Paystack Secret Key.
5. **Set Root Directory:**
   * In your Service settings in Railway, under **Build**, specify the **Root Directory** as `/server`.
6. **Deploy:** Railway will automatically build and deploy your Express backend. You can generate a public domain in the **Settings** tab.

---

## Linking to Your Android App

Once your server is successfully deployed, update your Android application's connection details.

1. Open **AI Studio Secrets** or your local project's `.env` file in the root directory.
2. Add or update the variable `PAYSTACK_BACKEND_URL`:
   ```properties
   PAYSTACK_BACKEND_URL=https://your-deployed-backend-url.onrender.com
   ```
3. Re-compile the Android applet. The Retrofit engine in the application will now target your live, secure payment backend!

---

## Configuring Webhooks in Paystack

To enable real-time payment updates even if the app crashes or the student closes the screen:
1. Log in to your [Paystack Dashboard](https://dashboard.paystack.com/).
2. Navigate to **Settings -> API Keys & Webhooks**.
3. In the **Webhook URL** field, enter your deployed backend's webhook endpoint:
   `https://your-deployed-backend-url.onrender.com/payments/webhook`
4. Click **Save Changes**. Paystack will now trigger the `/payments/webhook` endpoint on your server for all payment events!
