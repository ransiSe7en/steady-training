plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.steady.training"
    compileSdk = 36
    defaultConfig { applicationId = "com.steady.training"; minSdk = 28; targetSdk = 35; versionCode = 7; versionName = "1.4.1" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    sourceSets["main"].assets.srcDir("../../dist")
    signingConfigs { create("personal") { val keyPath = System.getenv("STEADY_KEYSTORE_PATH"); if (keyPath != null) { storeFile = file(keyPath); storePassword = "android"; keyAlias = "androiddebugkey"; keyPassword = "android" } } }; buildTypes { getByName("debug") { isDebuggable = true; if (System.getenv("STEADY_KEYSTORE_PATH") != null) signingConfig = signingConfigs.getByName("personal") } }
}
dependencies {
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("androidx.health.connect:connect-client:1.1.0")
}






