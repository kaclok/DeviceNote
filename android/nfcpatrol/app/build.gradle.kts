plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.smlj.nfcpatrol"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.smlj.nfcpatrol"
        minSdk = 26
        targetSdk = 36
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

    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // http网络库
    implementation(libs.okhttp)
    implementation(libs.okio)
    implementation(libs.okhttpLogInterceptor)

    // json库
    // implementation(libs.hutoolJson)
    implementation(libs.gson)

    implementation(libs.retrofit)
    implementation(libs.retrofitConverterGson)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}