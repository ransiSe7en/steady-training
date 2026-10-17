package com.steady.training

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.webkit.*
import android.widget.TextView
import android.widget.ScrollView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.ZoneId

class MainActivity : ComponentActivity() {
    private lateinit var web: WebView
    private val permissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))
    private var chooserCallback: ValueCallback<Array<Uri>>? = null
    private var pendingExport: ByteArray? = null
    private var requestedDate = LocalDate.now().toString()
    private var ready = false
    private val permissionLauncher = registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        if (granted.containsAll(permissions)) readSteps(requestedDate)
        else emit("permission", "Step access was not granted. You can enable it in Health Connect.")
    }
    private val chooser = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        chooserCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data))
        chooserCallback = null
    }
    private val exporter = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = pendingExport
        pendingExport = null
        if (result.resultCode == Activity.RESULT_OK && result.data?.data != null && data != null) {
            try { contentResolver.openOutputStream(result.data!!.data!!)?.use { it.write(data) }; emitFile("Saved to your chosen folder") }
            catch (_: Exception) { emitFile("File could not be saved. Please try again.") }
        } else emitFile("Save cancelled")
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(19,23,21)
        window.navigationBarColor = Color.rgb(19,23,21)
        web = WebView(this)
        web.setBackgroundColor(Color.rgb(19,23,21))
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setSupportMultipleWindows(false)
        }
        val assetLoader = WebViewAssetLoader.Builder().addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this)).build()
        web.addJavascriptInterface(Bridge(), "SteadyAndroid")
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                if (request.url.scheme == "https" && request.url.host == "appassets.androidplatform.net") return assetLoader.shouldInterceptRequest(request.url)
                return WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", emptyMap(), ByteArrayInputStream(ByteArray(0)))
            }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (uri.scheme == "https" && uri.host == "appassets.androidplatform.net" && uri.path?.startsWith("/assets/") == true) return false
                if (uri.scheme in setOf("https", "http")) runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                return true
            }
            override fun onPageFinished(view: WebView, url: String) { ready = true; readSteps(LocalDate.now().toString()) }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(view: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                chooserCallback?.onReceiveValue(null)
                chooserCallback = callback
                return try { chooser.launch(params.createIntent()); true } catch (_: Exception) { chooserCallback = null; callback.onReceiveValue(null); false }
            }
            override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
                android.app.AlertDialog.Builder(this@MainActivity).setMessage(message).setPositiveButton("Continue") { _, _ -> result.confirm() }.setNegativeButton("Cancel") { _, _ -> result.cancel() }.setOnCancelListener { result.cancel() }.show()
                return true
            }
        }
        setContentView(web)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { if (web.canGoBack()) web.goBack() else { isEnabled = false; onBackPressedDispatcher.onBackPressed() } }
        })
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }
    override fun onResume() { super.onResume(); if (ready) readSteps(LocalDate.now().toString()) }
    override fun onDestroy() { chooserCallback?.onReceiveValue(null); web.removeJavascriptInterface("SteadyAndroid"); web.destroy(); super.onDestroy() }
    private fun emit(status: String, message: String = "", count: Long? = null, date: String = requestedDate) {
        val payload = JSONObject().put("status", status).put("message", message).put("date", date)
        if (count != null) payload.put("count", count).put("source", "Samsung Health via Health Connect").put("syncedAt", java.time.Instant.now().toString())
        runOnUiThread { if (ready) web.evaluateJavascript("window.dispatchEvent(new CustomEvent('steady-health',{detail:$payload}));", null) }
    }
    private fun emitFile(message: String) { web.evaluateJavascript("window.dispatchEvent(new CustomEvent('steady-file',{detail:${JSONObject.quote(message)}}));", null) }
    private fun readSteps(date: String) {
        requestedDate = date
        val requested = runCatching { LocalDate.parse(date) }.getOrNull() ?: return
        val today = LocalDate.now()
        if (requested.isAfter(today) || requested.isBefore(today.minusDays(29))) { emit("history", "Health Connect access is limited to recent days in this app.", date=date); return }
        val status = HealthConnectClient.getSdkStatus(this)
        if (status != HealthConnectClient.SDK_AVAILABLE) { emit("unavailable", "Install or update Health Connect, then enable Samsung Health step sharing.", date=date); return }
        lifecycleScope.launch {
            try {
                val client = HealthConnectClient.getOrCreate(this@MainActivity)
                if (!client.permissionController.getGrantedPermissions().containsAll(permissions)) { emit("permission", "Connect Health Connect to read your Samsung Health steps.", date=date); return@launch }
                val zone = ZoneId.systemDefault()
                val start = requested.atStartOfDay(zone).toInstant()
                val end = if (requested == today) java.time.Instant.now() else requested.plusDays(1).atStartOfDay(zone).toInstant()
                val result = client.aggregate(AggregateRequest(metrics=setOf(StepsRecord.COUNT_TOTAL), timeRangeFilter=TimeRangeFilter.between(start,end), dataOriginFilter=setOf(DataOrigin("com.sec.android.app.shealth"))))
                val count = result[StepsRecord.COUNT_TOTAL]
                if (count == null) emit("no_data", "No Samsung Health step records for this date yet. Check Samsung Health sharing permissions and open Samsung Health to sync.", date=date)
                else emit("connected", count=count, date=date)
            } catch (_: SecurityException) { emit("permission", "Step permission has been revoked. Reconnect Health Connect.", date=date) }
            catch (_: Exception) { emit("error", "Steps could not refresh. Try again after opening Samsung Health.", date=date) }
        }
    }
    inner class Bridge {
        @JavascriptInterface fun isNative(): Boolean = true
        @JavascriptInterface fun readSteps(date: String) { runOnUiThread { this@MainActivity.readSteps(date) } }
        @JavascriptInterface fun connectHealth(date: String) { runOnUiThread {
            requestedDate = date
            if (HealthConnectClient.getSdkStatus(this@MainActivity) == HealthConnectClient.SDK_AVAILABLE) permissionLauncher.launch(permissions)
            else runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"))) }.onFailure { emit("unavailable", "Install or update Health Connect from Google Play.") }
        } }
        @JavascriptInterface fun healthSettings() { runOnUiThread {
            runCatching { startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)) }.onFailure { emit("unavailable", "Open phone Settings and search for Health Connect.") }
        } }
        @JavascriptInterface fun saveFile(name: String, text: String, mime: String) { runOnUiThread {
            if (mime !in setOf("application/json", "text/calendar") || text.length > 160*1024*1024 || pendingExport != null) return@runOnUiThread
            pendingExport = text.toByteArray(Charsets.UTF_8)
            try { exporter.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type=mime; putExtra(Intent.EXTRA_TITLE, name.take(100).replace(Regex("[^A-Za-z0-9._-]"), "_")) }) }
            catch (_: Exception) { pendingExport=null; emitFile("Could not open the Android save dialog.") }
        } }
    }
}

class PrivacyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = TextView(this).apply { textSize=17f; setPadding(32,48,32,48); text="Steady · Your data stays on your phone\n\nSteady requests read-only access to Steps through Android Health Connect. It reads Samsung Health step totals for the selected day, displaying them on your homepage and saving the count locally with the last sync time. It does not write health records or access heart rate, sleep, location or contacts.\n\nSteps refresh while the app is open or when you tap Refresh. Samsung Health must be allowed to write Steps to Health Connect. Revoke Steady access at any time in Health Connect app permissions. Previously saved totals remain in Steady until you remove them or clear app data.\n\nWorkouts, nutrition, steps, measurements and photos are stored only in the app's local storage. No analytics, accounts, ads, server uploads or cloud sync are included. Android automatic app backup is disabled. Exported JSON backups include your personal data and photos, are not encrypted, and are saved only to the location you choose.\n\nClearing app data or uninstalling deletes local records. Export a backup first. Browser and Android app storage are separate; use backup export/restore to move your existing log.\n\nExternal Calendar links open your chosen calendar app/browser only when tapped. You control whether to save those reminders.\n\nThis is a personal-use build. The app cannot verify that Samsung Health has finished synchronizing. Its counts may temporarily differ from the Samsung Health screen." }
        setContentView(ScrollView(this).apply { addView(text) })
    }
}
