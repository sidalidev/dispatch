import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// The About screen shows the date of the latest security review: the newest docs/security/YYYY-MM-DD.md.
val lastSecurityReview: String = rootProject.file("docs/security")
    .listFiles { f -> Regex("""\d{4}-\d{2}-\d{2}\.md""").matches(f.name) }
    ?.maxOfOrNull { it.name.removeSuffix(".md") }
    ?: error("No security review found in docs/security")

/** Copies CHANGELOG.md and PRIVACY.md into the APK's assets, so the app shows the same text as the repository. */
abstract class BundleDocs : DefaultTask() {
    @get:InputFiles abstract val docs: ConfigurableFileCollection
    @get:OutputDirectory abstract val out: DirectoryProperty

    @TaskAction fun copy() {
        val dir = out.get().asFile
        dir.deleteRecursively(); dir.mkdirs()
        docs.files.forEach { it.copyTo(File(dir, it.name)) }
    }
}
val bundleDocs = tasks.register<BundleDocs>("bundleDocs") {
    docs.from(rootProject.file("CHANGELOG.md"), rootProject.file("PRIVACY.md"))
}

// Release signing: ~/.dispatch/keystore.properties (never in the repo).
val signing = File(System.getProperty("user.home"), ".dispatch/keystore.properties")
val signingProps = Properties().apply { if (signing.exists()) signing.inputStream().use { load(it) } }

android {
    namespace = "dev.sidali.dispatch"
    compileSdk = 36
    defaultConfig {
        applicationId = "dev.sidali.dispatch"
        minSdk = 30
        targetSdk = 36
        versionCode = 5
        versionName = "0.4.0"
        buildConfigField("String", "SECURITY_REVIEW", "\"$lastSecurityReview\"")
    }
    signingConfigs {
        if (signing.exists()) create("release") {
            storeFile = file(signingProps.getProperty("storeFile"))
            storePassword = signingProps.getProperty("storePassword")
            keyAlias = signingProps.getProperty("keyAlias")
            keyPassword = signingProps.getProperty("keyPassword")
        }
    }
    buildTypes {
        release {
            // Reproducible builds: no git commit id baked into the APK.
            vcsInfo.include = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // -Punsigned builds an unsigned APK, used to check that a build is reproducible.
            if (!project.hasProperty("unsigned")) signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    dependenciesInfo { includeInApk = false; includeInBundle = false }
}
androidComponents {
    onVariants { v -> v.sources.assets?.addGeneratedSourceDirectory(bundleDocs, BundleDocs::out) }
}
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

dependencies {
    val bom = platform("androidx.compose:compose-bom:2025.09.01")
    implementation(bom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.11.0")
    testImplementation("junit:junit:4.13.2")
}
