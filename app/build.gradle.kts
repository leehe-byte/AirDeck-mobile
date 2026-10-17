import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    // namespace 保持和主仓库一致：它决定 R 类 / BuildConfig 的包名，
    // 而源码里是直接用 R.layout.xxx（没有 import），
    // 所以 namespace 一动就得改所有 Kotlin 文件。applicationId 才是安装标识，
    // 两者本来就可以不同。
    namespace = "com.opengw.manager"
    compileSdk = 33

    defaultConfig {
        // 和主仓库 AirDeck 区分开。两边原来都是 com.opengw.manager，
        // 加上用的是同一把签名密钥，在装主 App 的设备上安装移动版会直接把它顶掉
        // （主 App 是设备上的服务端，被顶掉服务就停了）。
        // 分开之后两个 App 可以共存，误装也不会互相影响。
        // 代价：手机上已有的移动版无法覆盖升级，需要卸载重装一次
        //（这个 App 是无状态客户端，重装基本无损失）。
        applicationId = "com.opengw.manager.mobile"
        minSdk = 24
        targetSdk = 33
        versionCode = 12
        versionName = "2.5"
        setProperty("archivesBaseName", "${rootProject.name}-${versionName}")
    }

    // release 签名：keystore 由环境变量提供（和主仓库 AirDeck 用同一套密钥），
    // 本地没有 env 时不签名。
    //
    // 这里刻意做成「坏 secret 不会让构建崩」：
    // Base64.getDecoder().decode() 抛异常发生在配置阶段，会让整个构建直接失败，
    // 而且报错信息和 secret 本身没关系，很难定位。
    // 现在改成：依次尝试 basic / MIME / URL-safe 三种解码器，
    // 全失败才退回不签名 + 打一条醒目的警告，
    // 构建照常完成（产物会带 -unsigned 后缀，一眼能看出来）。
    val keystoreBytes: ByteArray? = System.getenv("KEYSTORE_BASE64")
        ?.filterNot { it.isWhitespace() }
        ?.takeIf { it.isNotEmpty() }
        ?.let { raw ->
            // 不同工具产出的 base64 变体不一样：
            //   basic    A-Za-z0-9+/=          —— 标准
            //   mime     basic + 允许折行       —— openssl base64 默认会折行
            //   url      A-Za-z0-9-_           —— URL-safe，basic 会直接拒绝
            val decoders = listOf<Pair<String, (String) -> ByteArray>>(
                "basic" to { s -> Base64.getDecoder().decode(s) },
                "mime" to { s -> Base64.getMimeDecoder().decode(s) },
                "url-safe" to { s -> Base64.getUrlDecoder().decode(s) },
            )
            var picked: String? = null
            var out: ByteArray? = null
            for ((name, decode) in decoders) {
                val bytes = try {
                    decode(raw)
                } catch (e: IllegalArgumentException) {
                    null
                }
                if (bytes != null) {
                    picked = name
                    out = bytes
                    break
                }
            }
            if (out == null) {
                logger.warn(
                    "⚠️  KEYSTORE_BASE64 三种 base64 解码器都失败了" +
                        "（长度 ${raw.length}，开头 \"${raw.take(8)}\"，" +
                        "结尾 \"${raw.takeLast(4)}\"）。本次构建不会签名，" +
                        "产物是 *-unsigned.apk。请检查 Repository secret " +
                        "KEYSTORE_FILE。"
                )
            } else {
                logger.lifecycle("keystore 解码成功（${picked}），${out.size} 字节")
            }
            out
        }

    signingConfigs {
        create("release") {
            if (keystoreBytes != null) {
                val keystoreFile = File.createTempFile("airdeck_release", ".jks")
                keystoreFile.writeBytes(keystoreBytes)
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: ""
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        val hasReleaseKey = keystoreBytes != null
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