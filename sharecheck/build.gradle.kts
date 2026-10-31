// Test-only app: a share target that reports what it received (text, and whether each shared file opens).
// Used to check Dispatch end to end on an emulator. Never published.
plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "dev.sidali.sharecheck"; compileSdk = 36
    defaultConfig { applicationId = "dev.sidali.sharecheck"; minSdk = 30; targetSdk = 36 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
