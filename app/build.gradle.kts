plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "org.aetheris.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.aetheris.app"

        minSdk = 26
        targetSdk = 36

        versionCode = 1
        versionName = "0.1.0-alpha"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility =
            JavaVersion.VERSION_17

        targetCompatibility =
            JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        /*
         * Utiliza o empacotamento moderno das bibliotecas
         * nativas para compatibilidade com dispositivos
         * que usam páginas de memória de 16 KB.
         */
        jniLibs {
            useLegacyPackaging = false
        }

        resources {
            excludes += setOf(
                "/META-INF/AL2.0",
                "/META-INF/LGPL2.1"
            )
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        checkDependencies = true
    }
}

dependencies {
    /*
     * AndroidX
     */

    implementation(
        libs.androidx.core.ktx
    )

    implementation(
        libs.androidx.activity.compose
    )

    /*
     * Lifecycle
     */

    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )

    /*
     * Dependência declarada diretamente porque ainda
     * não existe um alias correspondente no catálogo.
     */
    implementation(
        "androidx.lifecycle:lifecycle-runtime-compose:2.10.0"
    )

    implementation(
        libs.androidx.lifecycle.viewmodel.compose
    )

    /*
     * Jetpack Compose
     */

    implementation(
        platform(
            libs.androidx.compose.bom
        )
    )

    implementation(
        libs.androidx.compose.ui
    )

    implementation(
        libs.androidx.compose.ui.graphics
    )

    implementation(
        libs.androidx.compose.ui.tooling.preview
    )

    implementation(
        libs.androidx.compose.material3
    )

    debugImplementation(
        libs.androidx.compose.ui.tooling
    )

    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )

    /*
     * Koin
     */

    implementation(
        libs.koin.android
    )

    implementation(
        libs.koin.androidx.compose
    )

    /*
     * Kotlin Coroutines
     */

    implementation(
        libs.kotlinx.coroutines.core
    )

    implementation(
        libs.kotlinx.coroutines.android
    )

    /*
     * Google ARCore
     */

    implementation(
        libs.google.arcore
    )

    /*
     * Testes unitários executados na JVM
     */

    testImplementation(
        libs.junit
    )

    testImplementation(
        libs.mockk
    )

    testImplementation(
        libs.truth
    )

    testImplementation(
        libs.kotlinx.coroutines.test
    )

    /*
     * Testes instrumentados
     */

    androidTestImplementation(
        libs.androidx.junit
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(
        platform(
            libs.androidx.compose.bom
        )
    )

    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )
}