plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Add the Google services Gradle plugin
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.atdev.gestor"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.atdev.gestor"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // Librerías base de Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Importa el BoM de Firebase (gestiona versiones automáticamente)
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))

    // Librería para notificaciones (Cloud Messaging)
    implementation("com.google.firebase:firebase-messaging-ktx")

    // (Opcional) Para análisis, ayuda a que aparezca en el Dashboard
    implementation("com.google.firebase:firebase-analytics-ktx")

    implementation("com.android.volley:volley:1.2.1")

    // Pruebas
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}