plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.journal.tests"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            // Makes Android SDK stub classes return defaults instead of throwing
            // (e.g. android.util.Base64, android.content.SharedPreferences stubs)
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // ── Modules under test ───────────────────────────────────────────────────
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    // core:network must be explicit – it's an `implementation` dep of core:data
    // so it is NOT exposed transitively to this module
    implementation(project(":core:network"))
    implementation(project(":features:teacher:home"))
    implementation(project(":features:teacher:journal"))
    implementation(project(":features:teacher:studentcard"))
    implementation(project(":features:methodist:journals"))

    // ── Transitive deps not exposed by the modules above ─────────────────────
    // kotlinx-serialization (used in JournalRepositoryTest for Json)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Retrofit annotations referenced by JournalApi interface at compile time
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Room runtime – needed so JournalDatabase's RoomDatabase supertype resolves.
    // At test runtime we only mock JournalDatabase, so no real Room/SQLite runs.
    implementation("androidx.room:room-runtime:2.6.1")

    // SavedStateHandle (used in TeacherJournalViewModelTest)
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.8.3")

    // ── Test runner & assertions ──────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.24")

    // Coroutines testing
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // Mocking (MockK)
    testImplementation("io.mockk:mockk:1.13.11")

    // Flow testing (Turbine)
    testImplementation("app.cash.turbine:turbine:1.1.0")

    // Android unit-test helpers (InstantTaskExecutorRule)
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    // Robolectric – needed for android.util.Base64 in JwtUtilsTest
    testImplementation("org.robolectric:robolectric:4.12.1")
    testImplementation("androidx.test:core-ktx:1.5.0")
    testImplementation("androidx.test.ext:junit-ktx:1.1.5")
}
