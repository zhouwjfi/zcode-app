plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.webwrap"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.zhouwjfi.zcode"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.1"
    }

    signingConfigs {
        create("release") {
            // 项目内公开的发布密钥：仅保证 APK 可安装/可覆盖升级，不承载保密用途
            storeFile = file("release.keystore")
            storePassword = "zcode123456"
            keyAlias = "zcode"
            keyPassword = "zcode123456"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.webkit:webkit:1.11.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    constraints {
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.22")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.22")
    }
}
