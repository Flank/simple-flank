package io.github.flank.gradle.utils

import com.android.build.api.dsl.SigningConfig
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.tasks.ValidateSigningTask
import io.github.flank.gradle.SimpleFlankExtension
import java.io.File
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.withType

fun verifyNotDefaultKeystore(
    variantName: String,
    hermeticTest: Boolean,
    logger: Logger,
    signingConfig: SigningConfig?
) {
  val signingConfigFile = signingConfig?.storeFile

  if (signingConfigFile?.path?.contains("/.android/debug.keystore") != false) {
    val message =
        "The $variantName keystore should be set, using the default means the cache won't work"
    if (hermeticTest) {
      throw RuntimeException(message, null)
    } else {
      logger.warn("Warning: $message")
    }
  }
}

fun Project.useFixedKeystore() {
  val keystoreName = "debugKeystore"
  val copyDebugKeystore =
      tasks.register("copyDebugKeystore", Copy::class.java) {
        val pluginJar =
            zipTree(
                SimpleFlankExtension::class.java.classLoader.getResource(keystoreName)!!
                    .path
                    .split("!")
                    .first())
        from({ pluginJar.single { it.name == keystoreName }.path })
        into("$buildDir/uitestKeystore")
      }

  val androidExtension = extensions.getByName("android") as LibraryExtension
  tasks.withType<ValidateSigningTask>().configureEach { dependsOn(copyDebugKeystore) }
  androidExtension.signingConfigs {
    named("debug") {
      storeFile = File(copyDebugKeystore.get().destinationDir, keystoreName)
      keyAlias = "debugKey"
      keyPassword = "debugKeystore"
      storePassword = "debugKeystore"
    }
  }
}
