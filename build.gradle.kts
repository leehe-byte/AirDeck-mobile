// Top-level build file
plugins {
    // 与主仓库 AirDeck 保持同一套版本组合（已验证可用）。
    // 原来 AGP 8.1.0 + Gradle 8.5 在新 JDK 上跑 lintVitalAnalyzeRelease 会抛
    // InvocationTargetException，导致 release 包根本打不出来。
    id("com.android.application") version "8.13.0" apply false
    id("com.android.library") version "8.1.1" apply false
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false // 增加 Kotlin 声明
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
