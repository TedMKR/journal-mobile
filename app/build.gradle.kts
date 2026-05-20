import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

fun loadEnv(): Properties {
    val props = Properties()
    val envName = (project.findProperty("env") as String?) ?: "test"
    val envFile = rootProject.file("config/env.$envName.properties")
    if (envFile.exists()) {
        envFile.inputStream().use { props.load(it) }
    }
    return props
}

val env = loadEnv()

android {
    namespace = "com.journal.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.journal.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appAuthRedirectScheme"] = "com.university.journal"

        buildConfigField("String", "API_BASE_URL", "\"${env.getProperty("API_BASE_URL", "http://153.80.240.154:8080/api/v1/")}\"")
        buildConfigField("String", "OPENAPI_URL", "\"${env.getProperty("OPENAPI_URL", "http://153.80.240.154/api/openapi.yaml")}\"")
        buildConfigField("String", "KEYCLOAK_BASE_URL", "\"${env.getProperty("KEYCLOAK_BASE_URL", "http://153.80.240.154:8180")}\"")
        buildConfigField("String", "KEYCLOAK_REALM", "\"${env.getProperty("KEYCLOAK_REALM", "university-journal")}\"")
        buildConfigField("String", "KEYCLOAK_CLIENT_ID", "\"${env.getProperty("KEYCLOAK_CLIENT_ID", "journal-android")}\"")
        buildConfigField("boolean", "USE_DEBUG_ROLE", env.getProperty("USE_DEBUG_ROLE", "true"))
        buildConfigField("String", "DEBUG_ROLE", "\"${env.getProperty("DEBUG_ROLE", "teacher")}\"")
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:ui"))
    implementation(project(":shared:navigation"))

    implementation(project(":features:auth"))
    implementation(project(":features:teacher:home"))
    implementation(project(":features:teacher:journal"))
    implementation(project(":features:teacher:studentcard"))
    implementation(project(":features:teacher:dashboard"))
    implementation(project(":features:teacher:ved"))
    implementation(project(":features:student:home"))
    implementation(project(":features:methodist:templates"))

    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("net.openid:appauth:0.11.1")

    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("net.openid:appauth:0.11.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
