package io.github.flank.gradle

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.*
import com.android.build.api.variant.Variant
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import io.github.flank.gradle.tasks.*
import io.github.flank.gradle.utils.getFlankProject
import io.github.flank.gradle.utils.getSmallAppTask
import io.github.flank.gradle.utils.useFixedKeystore
import io.github.flank.gradle.utils.verifyNotDefaultKeystore

val flankExecutable: Configuration by configurations.creating

dependencies { flankExecutable("com.github.flank:flank") { version { prefer("22.04.0") } } }

val simpleFlankExtension = extensions.create<SimpleFlankExtension>("simpleFlank")

plugins.withType<AppPlugin> {
  requireNotNull<ApplicationAndroidComponentsExtension>(extensions.findByType()).onVariants {
      variant ->
    val debugApkDir: Provider<Directory> = variant.artifacts.get(SingleArtifact.APK)
    val testApkDir: Provider<Directory>? = variant.androidTest?.artifacts?.get(SingleArtifact.APK)

    if (testApkDir != null) {
      val builtArtifactsLoader = variant.artifacts.getBuiltArtifactsLoader()
      val appApk: Apk = Apk.from(debugApkDir, builtArtifactsLoader)
      val testApk: Apk = Apk.from(testApkDir, builtArtifactsLoader)

      configureTasks(
          variant,
          appApk,
          testApk,
          requireNotNull(project.extensions.findByType<ApplicationExtension>()))
    }
  }
  tasks.register<FlankVersionTask>("flankVersion") { flankJarClasspath.from(flankExecutable) }
  registerRunFlankTask()
  verifyNotDefaultKeystore()
}

plugins.withType<LibraryPlugin> {
  useFixedKeystore()
  requireNotNull<LibraryAndroidComponentsExtension>(extensions.findByType()).onVariants { variant ->
    val testApkDir: Provider<Directory>? = variant.androidTest?.artifacts?.get(SingleArtifact.APK)

    if (testApkDir != null) {
      val copySmallAppTask: CopySmallAppTask = getSmallAppTask()
      val builtArtifactsLoader = variant.artifacts.getBuiltArtifactsLoader()
      val appApk: Apk = Apk.from(copySmallAppTask.appApk)
      val testApk: Apk = Apk.from(testApkDir, builtArtifactsLoader)

      configureTasks(
          variant,
          appApk,
          testApk,
          requireNotNull(project.extensions.findByType<LibraryExtension>()))
      tasks.withType<BaseFlankApkTask>().configureEach { dependsOn(copySmallAppTask) }
    }
  }
  tasks.register<FlankVersionTask>("flankVersion") { flankJarClasspath.from(flankExecutable) }
  registerRunFlankTask()
}

fun registerFlankYamlWriter(
    variant: Variant,
    testApk: Apk,
    androidExtension: CommonExtension<*, *, *, *>
): TaskProvider<FlankYmlWriterTask> =
    tasks.register<FlankYmlWriterTask>("flankYaml${variant.name.capitalize()}") {
      projectId.convention(simpleFlankExtension.projectId)
      flankProject.convention(getFlankProject())
      this@register.variant.convention(variant.name)
      useOrchestrator.convention(
          provider {
            androidExtension.testOptions.execution.toUpperCase() == "ANDROIDX_TEST_ORCHESTRATOR"
          })

      devices.convention(
          simpleFlankExtension.devices.orElse(
              provider { listOf(NexusLowRes.deviceForMinSdk(variant.minSdkVersion.apiLevel)) }))
      this.testApk.convention(testApk)
    }

fun registerFlankRun(
    variant: Variant,
    testApk: Apk,
): TaskProvider<FlankRunTask> =
    tasks.register<FlankRunTask>("flankRun${variant.name.capitalize()}") {
      flankJarClasspath.from(flankExecutable)

      serviceAccountCredentials.set(simpleFlankExtension.credentialsFile)
      this@register.variant.set(variant.name)
      hermeticTests.set(simpleFlankExtension.hermeticTests)
      this.testApk.set(testApk)
      val dumpShards: String? by project
      this@register.dumpShards.set(dumpShards.toBoolean())
      val dry: String? by project
      this@register.dry.set(dry.toBoolean())
    }

fun registerFlankDoctor(
    variant: Variant,
    testApk: Apk,
): TaskProvider<FlankDoctorTask> =
    tasks.register<FlankDoctorTask>("flankDoctor${variant.name.capitalize()}") {
      flankJarClasspath.from(flankExecutable)
      this@register.variant.set(variant.name)
      this.testApk.set(testApk)
    }

fun configureTasks(
    variant: Variant,
    appApk: Apk,
    testApk: Apk,
    commonExtension: CommonExtension<*, *, *, *>
) {
  val yamlWriterTask: TaskProvider<FlankYmlWriterTask> =
      registerFlankYamlWriter(variant, testApk, commonExtension)
  yamlWriterTask.configure { this.appApk.set(appApk) }
  registerFlankRun(
          variant,
          testApk,
      )
      .configure {
        flankYaml.set(yamlWriterTask.get().flankYaml)
        this.appApk.set(appApk)
      }
  registerFlankDoctor(variant, testApk).configure {
    flankYaml.set(yamlWriterTask.get().flankYaml)
    this.appApk.set(appApk)
  }
}

fun registerRunFlankTask() {
  tasks.register("flankRun") {
    group = Test.TASK_GROUP
    description = "Run all androidTest using flank."
    dependsOn(tasks.withType<FlankRunTask>())
  }
}
