package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.ConsoleMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

object PuterChatHelper {
    private const val TAG = "PuterChatHelper"
    private var webView: WebView? = null
    private var isInitialized = false
    private var pendingDeferred: CompletableDeferred<String>? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun initialize(context: Context) {
        if (isInitialized) return
        
        Handler(Looper.getMainLooper()).post {
            try {
                val wv = WebView(context.applicationContext)
                wv.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    @Suppress("DEPRECATION")
                    allowFileAccessFromFileURLs = true
                    @Suppress("DEPRECATION")
                    allowUniversalAccessFromFileURLs = true
                }
                
                wv.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d(TAG, "Puter HTML loaded successfully on URL: $url")
                        isInitialized = true
                    }
                }

                wv.webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        Log.d("PuterConsole", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                        return true
                    }
                }
                
                wv.addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onAnswerReceived(answer: String) {
                        Log.d(TAG, "Answer received: $answer")
                        pendingDeferred?.complete(answer)
                        pendingDeferred = null
                    }
                    
                    @JavascriptInterface
                    fun onErrorReceived(error: String) {
                        Log.e(TAG, "Error received: $error")
                        pendingDeferred?.completeExceptionally(Exception(error))
                        pendingDeferred = null
                    }
                }, "AndroidInterface")
                
                try {
                    val htmlContent = context.assets.open("index.html").bufferedReader().use { it.readText() }
                    val puterJsContent = context.assets.open("puter.js").bufferedReader().use { it.readText() }
                    
                    // Replace the puter.js script tag with the inline compiled library source
                    val mergedHtml = htmlContent.replace("<script src=\"puter.js\"></script>", "<script>$puterJsContent</script>")
                    
                    // Load into WebView with a secure puter.com origin to enable CORS requests
                    wv.loadDataWithBaseURL("https://puter.com", mergedHtml, "text/html", "UTF-8", null)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load/merge index.html or puter.js from assets: ${e.message}", e)
                    // Fallback to loading file URL if asset read fails
                    wv.loadUrl("file:///android_asset/index.html")
                }
                
                webView = wv
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize WebView: ${e.message}", e)
            }
        }
    }

    suspend fun askExamBot(question: String): String {
        val deferred = CompletableDeferred<String>()
        pendingDeferred = deferred
        
        Handler(Looper.getMainLooper()).post {
            val wv = webView
            if (wv == null) {
                deferred.completeExceptionally(Exception("Puter service is not initialized yet. Please try again in a moment!"))
                return@post
            }
            
            // Escaping single quotes and other special characters for JavaScript string
            val escapedQuestion = question.replace("\\", "\\\\")
                                          .replace("'", "\\'")
                                          .replace("\"", "\\\"")
                                          .replace("\n", "\\n")
                                          .replace("\r", "\\r")
            
            wv.evaluateJavascript("javascript:askExamBot('$escapedQuestion');", null)
        }
        
        // Timeout after 15 seconds to gracefully fallback to Gemini if Puter takes too long or fails
        val result = withTimeoutOrNull(15000) {
            try {
                deferred.await()
            } catch (e: Exception) {
                throw e
            }
        }
        
        if (result == null) {
            pendingDeferred = null
            throw Exception("Puter AI request timed out")
        }
        
        return result
    }
}
