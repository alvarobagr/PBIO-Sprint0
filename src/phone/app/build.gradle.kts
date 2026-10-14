import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Clave de demostracion cargada localmente, nunca desde GitHub.
// En la raiz del proyecto Android: src/phone/local.properties
val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.isFile) {
        archivo.inputStream().use { load(it) }
    }
}

val tokenPbio = propiedadesLocales
    .getProperty("PBIO_API_TOKEN", "")
    .trim()

// El token generado en PowerShell son 64 caracteres hexadecimales.
// Si no esta configurado, el proyecto compila, pero no puede enviar POST.
require(tokenPbio.isEmpty() || tokenPbio.matches(Regex("[0-9a-fA-F]{64}"))) {
    "PBIO_API_TOKEN debe ser un token hexadecimal de 64 caracteres."
}

android {
    namespace = "com.example.abalgra1.pbio"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.abalgra1.pbio"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Necesario para generar BuildConfig.PBIO_API_TOKEN.
    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            // SOLO para la APK de demostracion/debug.
            buildConfigField("String", "PBIO_API_TOKEN", "\"$tokenPbio\"")
        }

        release {
            // No incluir el secreto de demostracion en la APK release.
            buildConfigField("String", "PBIO_API_TOKEN", "\"\"")
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
