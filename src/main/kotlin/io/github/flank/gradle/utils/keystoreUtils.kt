package io.github.flank.gradle.utils

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.tasks.ValidateSigningTask
import io.github.flank.gradle.SimpleFlankExtension
import java.io.File
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.maybeCreate
import org.gradle.kotlin.dsl.withType

fun verifyNotDefaultKeystore(
    applicationExtension: ApplicationExtension,
    variant: Variant,
    hermeticTest: Provider<Boolean>,
    logger: Logger
) {
  val signingName =
      applicationExtension.buildTypes.named(variant.buildType!!).get().signingConfig?.name!!
  val signingConfigFile = applicationExtension.signingConfigs.named(signingName).get().storeFile

  if (signingConfigFile?.path?.contains("/.android/debug.keystore") == true) {
    val message =
        "The ${variant.name} keystore should be set, using the default means the cache won't work"
    if (hermeticTest.getOrElse(false)) {
      throw RuntimeException(message, null)
    } else {
      logger.warn("Warning: $message")
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
