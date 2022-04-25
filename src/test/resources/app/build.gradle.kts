plugins {
  id("com.android.application")
  id("kotlin-android")
  id("io.github.flank.gradle.simple-flank")
}

repositories {
  google()
  mavenCentral()
}

android {
  compileSdkVersion(30)
  defaultConfig {
    applicationId = "flank.simpleflank.testApp"
    versionCode = 1
    versionName = "1.0"
    targetSdkVersion(30)
    minSdkVersion(21)
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
}
