package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PremiumService
 * 
 * Secure billing service for JHS ExamBot.
 * Keeps only the Paystack Public Key on the device and delegates all secret operations,
 * signatures, and database verification locks securely to the Node.js backend.
 */
class PremiumService private constructor() {

    init {
        validateConfiguration()
        logConfiguration()
    }

    /**
     * Validates that Paystack configuration is correct and safe before any API calls.
     * Throws [IllegalStateException] with a clear message on misconfiguration.
     */
    private fun validateConfiguration() {
        val pubKey = try {
            BuildConfig.PAYSTACK_PUBLIC_KEY.trim()
        } catch (e: Exception) {
            ""
        }

        val backendUrl = try {
            BuildConfig.PAYSTACK_BACKEND_URL.trim()
        } catch (e: Exception) {
            ""
        }

        // SECURITY: If the Public Key field contains a Secret Key (sk_...),
        // the user has accidentally put their secret key in the Android config.
        if (pubKey.startsWith("sk_")) {
            throw IllegalStateException(
                "SECURITY ERROR: PAYSTACK_PUBLIC_KEY in .env starts with 'sk_', " +
                        "which looks like a Paystack Secret Key. " +
                        "You have placed your Secret Key in the Public Key field. " +
                        "Remove it from the Android .env and AI Studio Secrets immediately. " +
                        "The Secret Key should ONLY be set in your Node.js backend's server/.env file, " +
                        "never in the Android project."
            )
        }

        // If the backend URL has a custom value, verify it has a valid scheme.
        // Allow blank or placeholder values (normal "not configured" state) without crashing.
        if (backendUrl.isNotBlank() &&
            !backendUrl.contains("your-backend.com") &&
            !backendUrl.startsWith("http://") &&
            !backendUrl.startsWith("https://")
        ) {
            throw IllegalStateException(
                "CONFIGURATION ERROR: PAYSTACK_BACKEND_URL must start with 'http://' or 'https://'. " +
                        "Current value starts with: '${backendUrl.take(20)}...'. " +
                        "If you accidentally placed your Paystack Secret Key (sk_...) as the BACKEND_URL, " +
                        "remove it and set the correct Node.js backend deployment URL instead."
            )
        }
    }

    /**
     * Logs configuration state in debug builds only.
     * Never logs full keys — only the prefix (e.g. pk_live_****).
     */
    private fun logConfiguration() {
        if (!BuildConfig.DEBUG) return

        val pubKey = try {
            BuildConfig.PAYSTACK_PUBLIC_KEY.trim()
        } catch (e: Exception) {
            null
        }

        val backendUrl = try {
            BuildConfig.PAYSTACK_BACKEND_URL.trim()
        } catch (e: Exception) {
            null
        }

        Log.d("PremiumService", "--- PremiumService Configuration ---")
        Log.d("PremiumService", "Backend URL: ${if (backendUrl.isNullOrBlank()) "not configured" else backendUrl}")
        Log.d("PremiumService", "Public Key prefix: ${
            if (pubKey.isNullOrBlank()) "not configured"
            else "${pubKey.take(7)}****"
        }")
        Log.d("PremiumService", "Public key starts with pk_: ${pubKey?.startsWith("pk_") ?: false}")
        Log.d("PremiumService", "-----------------------------------")
    }

    // Retrieve keys dynamically from BuildConfig (populated from AI Studio Secrets panel)
    val paystackPublicKey: String
        get() = try {
            BuildConfig.PAYSTACK_PUBLIC_KEY.trim()
        } catch (e: Exception) {
            "YOUR_KEY_HERE"
        }

    val backendBaseUrl: String
        get() = try {
            BuildConfig.PAYSTACK_BACKEND_URL.trim()
        } catch (e: Exception) {
            "https://your-backend.com"
        }

    /**
     * Generates a fully functional, beautiful Paystack inline checkout HTML page 
     * running completely client-side in a secure WebView using only the Public Key.
     */
    fun generateInlineCheckoutHtml(email: String, amountGhs: Double, reference: String): String {
        val amountInPesewas = (amountGhs * 100).toInt()
        val pubKey = paystackPublicKey

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                <title>Secure Paystack Checkout</title>
                <script src="https://js.paystack.co/v1/inline.js"></script>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                        height: 100vh;
                        margin: 0;
                        background-color: #f8fafc;
                        color: #1e293b;
                        padding: 24px;
                        box-sizing: border-box;
                    }
                    .card {
                        background: white;
                        padding: 30px;
                        border-radius: 16px;
                        box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
                        text-align: center;
                        max-width: 400px;
                        width: 100%;
                    }
                    .loader {
                        border: 4px solid #f1f5f9;
                        border-top: 4px solid #10b981;
                        border-radius: 50%;
                        width: 48px;
                        height: 48px;
                        animation: spin 1s linear infinite;
                        margin: 0 auto 20px auto;
                    }
                    @keyframes spin {
                        0% { transform: rotate(0deg); }
                        100% { transform: rotate(360deg); }
                    }
                    .title {
                        font-size: 18px;
                        font-weight: 700;
                        margin-bottom: 8px;
                        color: #0f172a;
                    }
                    .subtitle {
                        font-size: 14px;
                        color: #64748b;
                        margin-bottom: 24px;
                        line-height: 1.5;
                    }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="loader"></div>
                    <div class="title">Securing Connection...</div>
                    <div class="subtitle">Opening Paystack secure checkout portal to authorize GH¢$amountGhs...</div>
                </div>

                <script type="text/javascript">
                    function launchPaystack() {
                        try {
                            var handler = PaystackPop.setup({
                                key: '$pubKey',
                                email: '$email',
                                amount: $amountInPesewas,
                                currency: 'GHS',
                                ref: '$reference',
                                callback: function(response) {
                                    console.log('Payment success. Ref: ' + response.reference);
                                    if (window.PaystackBridge) {
                                        window.PaystackBridge.onSuccess(response.reference);
                                    }
                                },
                                onClose: function() {
                                    console.log('Payment window closed');
                                    if (window.PaystackBridge) {
                                        window.PaystackBridge.onClosed();
                                    }
                                }
                            });
                            handler.openIframe();
                        } catch (err) {
                            console.error('Error launching Paystack inline:', err);
                            if (window.PaystackBridge) {
                                window.PaystackBridge.onError(err.message || err.toString());
                            }
                        }
                    }

                    // Launch automatically after script load
                    window.onload = function() {
                        setTimeout(launchPaystack, 800);
                    };
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Secures transaction verification by calling the Node.js backend.
     * Unlocks Premium ONLY if backend successfully validates transaction.
     */
    suspend fun verifyPaymentWithBackend(reference: String, userId: String): BackendVerifyResponse = withContext(Dispatchers.IO) {
        try {
            val api = PaystackApi.create(backendBaseUrl)
            val response = api.verifyTransactionWithBackend(
                BackendVerifyRequest(
                    paymentReference = reference,
                    userId = userId
                )
            )
            Log.d("PremiumService", "Backend verification success=${response.success}, premium=${response.premium}")
            response
        } catch (e: Exception) {
            Log.e("PremiumService", "Error verifying payment with backend", e)
            BackendVerifyResponse(
                success = false,
                premium = false,
                error = e.localizedMessage ?: "Unknown network connection failure"
            )
        }
    }

    /**
     * Retrieves premium subscription status directly from Node.js backend.
     */
    suspend fun checkPremiumStatusWithBackend(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val api = PaystackApi.create(backendBaseUrl)
            val response = api.checkPremiumStatusWithBackend(userId)
            response.success && response.premium
        } catch (e: Exception) {
            Log.e("PremiumService", "Error checking premium status on backend", e)
            false
        }
    }

    companion object {
        @Volatile
        private var instance: PremiumService? = null

        fun getInstance(): PremiumService {
            return instance ?: synchronized(this) {
                instance ?: PremiumService().also { instance = it }
            }
        }
    }
}
