import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.opengw.manager"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.opengw.manager"
        minSdk = 24
        targetSdk = 33
        versionCode = 10
        versionName = "2.3"
        setProperty("archivesBaseName", "${rootProject.name}-${versionName}")
    }

    // release 签名：keystore 由环境变量提供（和主仓库 AirDeck 用同一套密钥），
    // 本地没有 env 时不签名，行为与之前一致。
    // 之前这里完全没有签名配置，assembleRelease 产出的是 *-unsigned.apk，装不上。
    signingConfigs {
        create("release") {
            val b64 = System.getenv("KEYSTORE_BASE64")
            if (!b64.isNullOrBlank()) {
                val keystoreFile = File.createTempFile("airdeck_release", ".jks")
                // secrets 里的 base64 可能含换行/空白，先剔除再解码
                keystoreFile.writeBytes(Base64.getDecoder().decode(b64.filterNot { it.isWhitespace() }))
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: ""
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        val hasReleaseKey = !System.getenv("KEYSTORE_BASE64").isNullOrBlank()
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseKey) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // 高性能 HTTP 客户端: OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // 协程支持
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    // Lifecycle KTX (lifecycleScope)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")
}