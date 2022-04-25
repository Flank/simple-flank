package com.github.flank.gradle.utils

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.tasks.ValidateSigningTask
import com.github.flank.gradle.SimpleFlankExtension
import java.io.File
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.maybeCreate
import org.gradle.kotlin.dsl.withType

fun Project.verifyNotDefaultKeystore() {
  val androidExtension = project.extensions.getByName("android") as ApplicationExtension
  afterEvaluate {
    androidExtension.signingConfigs {
      named("debug") {
        if (storeFile?.path?.contains("/.android/debug.keystore") == true) {
          println(
              "Warning: The debug keystore should be set, using the default means the cache won't work")
        }
      }
    }
  }
}

fun Project.useFixedKeystore() {
  val copyDebugKeystore: Copy = rootProject.tasks.maybeCreate<Copy>("copyDebugKeystore")
  with(copyDebugKeystore) {
    val pluginJar =
        zipTree(
            SimpleFlankExtension::class.java.classLoader.getResource("debugKeystore")!!
                .path
                .split("!")
                .first())
    from({ pluginJar.single { it.name == "debugKeystore" }.path })
    into("$buildDir/uitestKeystore")
  }

  val androidExtension = extensions.getByName("android") as LibraryExtension
  tasks.withType<ValidateSigningTask>().configureEach { dependsOn(copyDebugKeystore) }
  androidExtension.signingConfigs {
    named("debug") {
      storeFile = File(rootProject.buildDir, "uitestKeystore/debugKeystore")
      keyAlias = "debugKey"
      keyPassword = "debugKeystore"
      storePassword = "debugKeystore"
    }
  }
}
