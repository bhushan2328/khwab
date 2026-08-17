import java.util.Properties


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

android {
    lint {
        disable += "MissingPermission"
    }

    namespace = "com.toblad.khwab"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.toblad.khwab"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProps = Properties()
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localProps.load(localPropsFile.inputStream())
        }
        buildConfigField("String", "GEMINI_API_KEY",
            "\"${localProps.getProperty("GEMINI_API_KEY", "")}\"")
        buildConfigField("String", "OPENROUTER_API_KEY",
            "\"${localProps.getProperty("OPENROUTER_API_KEY", "")}\"")

        // Support both arm64-v8a (64-bit devices) and armeabi-v7a (32-bit ROM devices
        // such as POCO C3 which ships with a 32-bit Android userspace despite having a
        // 64-bit CPU). Unity 6000.5.7f1 ships armeabi-v7a static libs in its Variations
        // folder, so the IL2CPP build produces valid .so files for both ABIs.
        ndk {
            abiFilters += "arm64-v8a"
            abiFilters += "armeabi-v7a"
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {

    implementation(project(":core"))
    implementation(project(":integration"))
    implementation(project(":unityLibrary"))

    // Unity classes JAR — needed to reference UnityPlayerForActivityOrService and
    // IUnityPlayerLifecycleEvents in UnityAuraManager.  The JAR is already bundled
    // inside :unityLibrary at runtime; this entry adds it to the :app compile classpath
    // only, so the classes are resolvable at compile time without being double-packaged.
    compileOnly(files("../../khwab-aura-unity/export/unityLibrary/libs/unity-classes.jar"))

    // Coroutines (Android main-thread dispatcher for VoiceService scope)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Security (EncryptedSharedPreferences)
    implementation(libs.androidx.security.crypto)

    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)

    // Material Icons (Mic, Chat, StopCircle, etc.)
    implementation("androidx.compose.material:material-icons-extended")

    // Google Fonts (Nunito)
    implementation("androidx.compose.ui:ui-text-google-fonts:1.7.8")

    // Navigation
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Sherpa-ONNX Java API stub (classes.jar only, 229 KB).
    // The native .so files are NOT bundled in the APK — they are downloaded
    // at first launch from GitHub Releases by ModelDownloadManager and loaded
    // via System.load() in SpeechManager before the engine initialises.
    implementation(files("libs/sherpa-onnx-stub.jar"))

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}