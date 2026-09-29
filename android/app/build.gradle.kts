import java.util.Properties
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { input ->
            load(input)
        }
    }
}

val neisApiKey = localProperties.getProperty("NEIS_API_KEY", "")
val webBaseUrl = localProperties.getProperty("WEB_BASE_URL", "")
val googleServicesFile = file("google-services.json")

if (googleServicesFile.exists()) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

android {
    namespace = "com.lbs.schoolhelper"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lbs.schoolhelper"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "NEIS_BASE_URL", "\"https://open.neis.go.kr/\"")
        buildConfigField("String", "NEIS_API_KEY", "\"$neisApiKey\"")
        buildConfigField("String", "WEB_BASE_URL", "\"$webBaseUrl\"")
    }

    buildTypes {
        debug {
            // Ordinary debug builds never send telemetry.
            buildConfigField("boolean", "TELEMETRY_ALLOWED", "false")
            buildConfigField("String", "TELEMETRY_ENVIRONMENT", "\"debug\"")
        }
        create("qa") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".qa"
            versionNameSuffix = "-qa"
            // QA stays debuggable and is intentionally not minified.
            isDebuggable = true
            matchingFallbacks += listOf("debug")
            // google-services.json must contain an explicit com.lbs.schoolhelper.qa client.
            buildConfigField("boolean", "TELEMETRY_ALLOWED", "true")
            buildConfigField("String", "TELEMETRY_ENVIRONMENT", "\"qa\"")
        }
        create("r8Qa") {
            initWith(getByName("qa"))
            // Same QA package/signing lets this minified build replace QA safely.
            applicationIdSuffix = ".qa"
            versionNameSuffix = "-r8qa"
            // Keep the QA application id/signing, but run the real R8 pipeline.
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            matchingFallbacks += listOf("qa", "release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            buildConfigField("boolean", "TELEMETRY_ALLOWED", googleServicesFile.exists().toString())
            buildConfigField("String", "TELEMETRY_ENVIRONMENT", "\"release\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
        dataBinding = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// Keep the canonical Gradle outputs intact and provide release-ready copies
// whose names are easy to identify in CI artifacts and on a developer machine.
fun registerNamedArtifactTask(variant: String, versionName: String) {
    val variantCapitalized = variant.replaceFirstChar { it.uppercaseChar() }
    tasks.register("package${variantCapitalized}NamedArtifacts") {
        group = "build"
        description = "Builds the $variant APK/AAB with a version-and-timestamp filename."
        dependsOn("assemble$variantCapitalized", "bundle$variantCapitalized")

        doLast {
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val outputDirectory = layout.buildDirectory.dir("outputs/named/$variant").get().asFile
            outputDirectory.mkdirs()

            val apk = fileTree(layout.buildDirectory.dir("outputs/apk/$variant").get().asFile)
                .matching { include("*.apk") }
                .files
                .singleOrNull()
                ?: error("Expected exactly one APK for $variant")
            val aab = fileTree(layout.buildDirectory.dir("outputs/bundle/$variant").get().asFile)
                .matching { include("*.aab") }
                .files
                .singleOrNull()
                ?: error("Expected exactly one AAB for $variant")

            val baseName = "schoolon_${versionName}_${android.defaultConfig.versionCode}_$timestamp"
            apk.copyTo(outputDirectory.resolve("$baseName.apk"), overwrite = true)
            aab.copyTo(outputDirectory.resolve("$baseName.aab"), overwrite = true)
            logger.lifecycle("Named artifacts: ${outputDirectory.resolve("$baseName.apk").absolutePath}")
            logger.lifecycle("Named artifacts: ${outputDirectory.resolve("$baseName.aab").absolutePath}")
        }
    }
}

val baseVersionName = android.defaultConfig.versionName ?: error("versionName must be configured")
registerNamedArtifactTask("qa", "$baseVersionName-qa")
registerNamedArtifactTask("release", baseVersionName)

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
