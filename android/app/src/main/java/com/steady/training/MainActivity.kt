package com.steady.training

import android.Manifest
import android.app.AlarmManager
import android.os.Build
import android.provider.Settings
import android.content.pm.PackageManager
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
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
    private var nutritionRequest = false
    private var nutritionDate = LocalDate.now().toString()
    private val nutritionPermissions = setOf(HealthPermission.getReadPermission(NutritionRecord::class))
    private val notificationLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if (allowed) requestPreciseTimer() else emitRest("Enable Steady notifications in Android Settings to hear rest alerts.")
    }
    private val permissionLauncher = registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        if (nutritionRequest) {
            nutritionRequest=false
            if(granted.containsAll(nutritionPermissions)) readNutrition(nutritionDate) else emitNutrition("permission","Nutrition access was not granted.")
        } else if (granted.containsAll(permissions)) readSteps(requestedDate)
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
        RestAlerts.channel(this)
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
        ViewCompat.setOnApplyWindowInsetsListener(web) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            val params = view.layoutParams as android.view.ViewGroup.MarginLayoutParams; params.setMargins(bars.left, bars.top, bars.right, bars.bottom); view.layoutParams = params
            insets
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { if (web.canGoBack()) web.goBack() else { isEnabled = false; onBackPressedDispatcher.onBackPressed() } }
        })
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }
    override fun onResume() { super.onResume(); RestAlerts.rearm(this); if (ready) readSteps(LocalDate.now().toString()) }
    override fun onDestroy() { chooserCallback?.onReceiveValue(null); web.removeJavascriptInterface("SteadyAndroid"); web.destroy(); super.onDestroy() }
    private fun emit(status: String, message: String = "", count: Long? = null, date: String = requestedDate, source: String = "Samsung Health via Health Connect") {
        val payload = JSONObject().put("status", status).put("message", message).put("date", date)
        if (count != null) payload.put("count", count).put("source", source).put("syncedAt", java.time.Instant.now().toString())
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
                val end = requested.plusDays(1).atStartOfDay(zone).toInstant()
                val result = client.aggregate(AggregateRequest(metrics=setOf(StepsRecord.COUNT_TOTAL), timeRangeFilter=TimeRangeFilter.between(start,end), dataOriginFilter=setOf(DataOrigin("com.sec.android.app.shealth"))))
                val count = result[StepsRecord.COUNT_TOTAL]
                if (count == null) {
                    val other = client.aggregate(AggregateRequest(metrics=setOf(StepsRecord.COUNT_TOTAL),timeRangeFilter=TimeRangeFilter.between(start,end)))[StepsRecord.COUNT_TOTAL]
                    if (other != null) emit("connected", "Using Health Connect total because Samsung Health has no records for this date.", count=other, date=date, source="Health Connect")
                    else emit("no_data", "Health Connect has no step records for this date yet.", date=date)
                }
                else emit("connected", count=count, date=date)
            } catch (_: SecurityException) { emit("permission", "Step permission has been revoked. Reconnect Health Connect.", date=date) }
            catch (error: Exception) { emit("error", "Health Connect read failed (${error.javaClass.simpleName}). Open Samsung Health to sync and retry.", date=date) }
        }
    }
    private fun emitRest(message:String) { if(ready)web.evaluateJavascript("window.dispatchEvent(new CustomEvent('steady-rest',{detail:{message:${JSONObject.quote(message)}}}));",null) }
    private fun requestPreciseTimer() {
        if(Build.VERSION.SDK_INT>=31&&!getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) {
            runCatching { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:$packageName"))) }
            emitRest("Allow precise alarms for timely rest alerts with the screen off.")
        }
    }
    private fun emitNutrition(status:String,message:String="",totals:JSONObject?=null) {
        val payload=totals?:JSONObject()
        payload.put("status",status).put("message",message).put("date",nutritionDate)
        if(ready)web.evaluateJavascript("window.dispatchEvent(new CustomEvent('steady-nutrition',{detail:$payload}));",null)
    }
    private fun readNutrition(date:String) {
        val selected=runCatching { LocalDate.parse(date) }.getOrNull()?:return
        nutritionDate=date
        if(selected.isAfter(LocalDate.now())||selected.isBefore(LocalDate.now().minusDays(29))) { emitNutrition("history","Choose a date within the past 30 days.");return }
        if(HealthConnectClient.getSdkStatus(this)!=HealthConnectClient.SDK_AVAILABLE){emitNutrition("unavailable","Install or update Health Connect first.");return}
        lifecycleScope.launch {
            try {
                val client=HealthConnectClient.getOrCreate(this@MainActivity)
                if(!client.permissionController.getGrantedPermissions().containsAll(nutritionPermissions)){
                    nutritionRequest=true;permissionLauncher.launch(nutritionPermissions);return@launch
                }
                val zone=ZoneId.systemDefault()
                val result=client.aggregate(AggregateRequest(metrics=setOf(NutritionRecord.ENERGY_TOTAL,NutritionRecord.PROTEIN_TOTAL,NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL,NutritionRecord.TOTAL_FAT_TOTAL),timeRangeFilter=TimeRangeFilter.between(selected.atStartOfDay(zone).toInstant(),selected.plusDays(1).atStartOfDay(zone).toInstant()),dataOriginFilter=setOf(DataOrigin("com.myfitnesspal.android"))))
                val totals=JSONObject()
                result[NutritionRecord.ENERGY_TOTAL]?.let { totals.put("calories",it.inKilocalories) }
                result[NutritionRecord.PROTEIN_TOTAL]?.let { totals.put("protein",it.inGrams) }
                result[NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL]?.let { totals.put("carbs",it.inGrams) }
                result[NutritionRecord.TOTAL_FAT_TOTAL]?.let { totals.put("fat",it.inGrams) }
                if(totals.length()==0)emitNutrition("no_data","No MyFitnessPal nutrition records for this date. In MyFitnessPal > Apps & Devices > Health Connect, enable nutrition sharing, then use MyFitnessPal's Sync option.")
                else emitNutrition("connected",totals=totals)
            } catch(_:SecurityException){emitNutrition("permission","Nutrition permission was revoked. Tap Import again to reconnect.")}
            catch(error:Exception){emitNutrition("error","Nutrition import failed (${error.javaClass.simpleName}). Try syncing MyFitnessPal first.")}
        }
    }
    inner class Bridge {
        @JavascriptInterface fun isNative(): Boolean = true
        @JavascriptInterface fun readNutrition(date:String) { runOnUiThread { this@MainActivity.readNutrition(date) } }
        @JavascriptInterface fun startRest(seconds:Int) { runOnUiThread {
            RestAlerts.schedule(this@MainActivity,seconds)
            if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else requestPreciseTimer()
        } }
        @JavascriptInterface fun stopRest() { runOnUiThread { RestAlerts.stop(this@MainActivity) } }
        @JavascriptInterface fun finishRest() { runOnUiThread { RestAlerts.finish(this@MainActivity) } }
        @JavascriptInterface fun readSteps(date: String) { runOnUiThread { this@MainActivity.readSteps(date) } }
        @JavascriptInterface fun connectHealth(date: String) { runOnUiThread {
            nutritionRequest=false
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
        val text = TextView(this).apply { textSize=17f; setPadding(32,48,32,48); text="Steady · Your data stays on your phone\n\nSteady requests read-only access to Steps and, only when importing MyFitnessPal totals, Nutrition through Android Health Connect. It reads Samsung Health step totals, falling back to the Health Connect aggregated total when Samsung records are unavailable, for the selected day, displaying them on your homepage and saving the count locally with the last sync time. It imports calories, protein, carbohydrates and fat shared by MyFitnessPal for review before saving. It does not write health records or access heart rate, sleep, location or contacts. Rest alerts use notification and precise-alarm permission.\n\nSteps refresh while the app is open or when you tap Refresh. For Samsung-specific totals, Samsung Health must be allowed to write Steps to Health Connect. Revoke Steady access at any time in Health Connect app permissions. Previously saved totals remain in Steady until you remove them or clear app data.\n\nWorkouts, nutrition, steps, measurements and photos are stored only in the app's local storage. No analytics, accounts, ads, server uploads or cloud sync are included. Android automatic app backup is disabled. Exported JSON backups include your personal data and photos, are not encrypted, and are saved only to the location you choose.\n\nClearing app data or uninstalling deletes local records. Export a backup first. Browser and Android app storage are separate; use backup export/restore to move your existing log.\n\nExternal Calendar links open your chosen calendar app/browser only when tapped. You control whether to save those reminders.\n\nThis is a personal-use build. The app cannot verify that Samsung Health has finished synchronizing. Its counts may temporarily differ from the Samsung Health screen." }
        setContentView(ScrollView(this).apply { addView(text) })
    }
}



