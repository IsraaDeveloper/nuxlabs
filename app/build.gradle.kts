import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { stream ->
        localProperties.load(stream)
    }
}
val rawServerUrl: String = localProperties.getProperty("nux.server.url") ?: ""
val rawServerApiKey: String = localProperties.getProperty("nux.server.api.key") ?: ""
val rawAiApiKey: String = localProperties.getProperty("nux.ai.api.key") ?: ""
val rawAiApiKey2: String = localProperties.getProperty("nux.ai.api.key2") ?: ""
val rawAiApiKey3: String = localProperties.getProperty("nux.ai.api.key3") ?: ""

android {
    namespace = "com.israadev.nuxlauncher"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.israadev.nuxlauncher"
        minSdk = 26
        targetSdk = 34
        versionCode = 14
        versionName = "1.1.3"

        buildConfigField("String", "SERVER_BASE_URL", "\"$rawServerUrl\"")
        buildConfigField("String", "SERVER_API_KEY", "\"$rawServerApiKey\"")
        buildConfigField("String", "DEFAULT_AI_API_KEY", "\"$rawAiApiKey\"")
        buildConfigField("String", "DEFAULT_AI_API_KEY_2", "\"$rawAiApiKey2\"")
        buildConfigField("String", "DEFAULT_AI_API_KEY_3", "\"$rawAiApiKey3\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters.clear()
            abiFilters.add("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.commons.compress)
    implementation(libs.xz)
    implementation(libs.bytehook)
    implementation("androidx.webkit:webkit:1.12.1")

    debugImplementation(libs.androidx.ui.tooling)
}
