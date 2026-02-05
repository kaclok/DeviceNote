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
        versionName = "1.0.1"

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

    testOptions {
        unitTests.isIncludeAndroidResources = false
    }

    androidComponents {
        beforeVariants { variant ->
            variant.androidTest.enable = false
        }

        onVariants(selector().all()) { variant ->
            val appName = "NFCPatrol"
            val versionName = defaultConfig.versionName ?: "1.0"

            variant.outputs.forEach { output ->
                // 对于 APK 输出
                if (output is com.android.build.api.variant.impl.VariantOutputImpl) {
                    output.outputFileName.set("$appName-${variant.buildType}-$versionName.apk")
                }
            }
        }
    }

}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.ui.graphics)
    /*
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    */

    // http网络库
    implementation(libs.okhttp)
    implementation(libs.okio)
    implementation(libs.okhttpLogInterceptor)

    // json库
    // implementation(libs.hutoolJson)
    // implementation(libs.gson)

    implementation(libs.retrofit)
    implementation(libs.retrofitConverterGson)

    implementation(libs.socketIO) {
        // 因Android已自带org.json，所以需要屏蔽
        exclude(group = "org.json", module = "json")
    }

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}