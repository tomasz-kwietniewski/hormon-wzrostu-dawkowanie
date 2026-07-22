plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "pl.hormonwzrostu"
    compileSdk = 36

    defaultConfig {
        applicationId = "pl.hormonwzrostu"
        minSdk = 26
        targetSdk = 36
        versionCode = 24
        versionName = "1.23"

        // Pełne symbole debugowania kodu natywnego (bibliotek) w AAB -> czytelne
        // raporty crashy/ANR natywnych w Play Console. Bez wpływu na działanie apki.
        ndk {
            debugSymbolLevel = "FULL"
        }
    }

    // Stały klucz podpisujący dostarczany przez CI (zmienne środowiskowe z secretów).
    // Lokalnie, gdy zmienne nie są ustawione, debug korzysta z domyślnego klucza debug.
    val keystorePath: String? = System.getenv("KEYSTORE_PATH")
    signingConfigs {
        create("release") {
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            if (keystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        release {
            // R8: usuwanie nieużywanego kodu + obfuskacja. Generuje mapping.txt
            // (dołączany do AAB) -> czytelne raporty crashy w Play + mniejszy rozmiar.
            // Model danych (pl.hormonwzrostu.data.**) jest chroniony regułami keep
            // w proguard-rules.pro, by nie zepsuć serializacji backupów (kotlinx.serialization).
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (keystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
}
