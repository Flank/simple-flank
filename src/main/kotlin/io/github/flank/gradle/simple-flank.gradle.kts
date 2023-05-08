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

dependencies { flankExecutable("com.github.flank:flank") { version { prefer("23.01.0") } } }

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

      val applicationExtension =
          requireNotNull(project.extensions.findByType<ApplicationExtension>())
      configureTasks(variant, variant.androidTest!!, appApk, testApk, applicationExtension)

      val signingConfigName =
          applicationExtension.buildTypes.getByName(variant.buildType!!).signingConfig?.name
      val signingConfig =
          signingConfigName?.let { applicationExtension.signingConfigs.named(it).get() }
      tasks.named<FlankRunTask>("flankRun${variant.name.capitalize()}").configure {
        doFirst {
          verifyNotDefaultKeystore(
              this@configure.variant.get(), hermeticTests.getOrElse(false), logger, signingConfig)
        }
      }
    }
  }
  tasks.register<FlankVersionTask>("flankVersion") { flankJarClasspath.from(flankExecutable) }
  registerRunFlankTask()
  registerAuthTask()
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
          variant.androidTest!!,
          appApk,
          testApk,
          requireNotNull(project.extensions.findByType<LibraryExtension>()))
      tasks.withType<BaseFlankApkTask>().configureEach { dependsOn(copySmallAppTask) }
    }
  }
  tasks.register<FlankVersionTask>("flankVersion") { flankJarClasspath.from(flankExecutable) }
  registerRunFlankTask()
  registerAuthTask()
}

fun registerFlankYamlWriter(
    variant: Variant,
    appApk: Apk,
    testApk: Apk,
    androidExtension: CommonExtension<*, *, *, *>,
    androidTest: AndroidTest
): TaskProvider<FlankYmlWriterTask> =
    tasks.register<FlankYmlWriterTask>("flankYaml${variant.name.capitalize()}") {
      projectId.convention(simpleFlankExtension.projectId)
      flankProject.convention(getFlankProject())
      this@register.variant.convention(variant.name)
      useOrchestrator.convention(
          provider {
            androidExtension.testOptions.execution.toUpperCase() == "ANDROIDX_TEST_ORCHESTRATOR"
          })

      testRunnerClass.convention(androidTest.instrumentationRunner)

      devices.convention(
          simpleFlankExtension.devices.orElse(
              provider { listOf(NexusLowRes.deviceForMinSdk(variant.minSdkVersion.apiLevel)) }))
      this.appApk.convention(appApk)
      this.testApk.convention(testApk)

      testTimeout.convention(simpleFlankExtension.testTimeout)

      directoriesToPull.convention(simpleFlankExtension.directoriesToPull)
      filesToDownload.convention(simpleFlankExtension.filesToDownload)
      keepFilePath.convention(simpleFlankExtension.keepFilePath)

      recordVideo.convention(simpleFlankExtension.recordVideo)
      numFlakyTestAttempts.convention(simpleFlankExtension.numFlakyTestAttempts)
      failFast.convention(simpleFlankExtension.failFast)
      performanceMetrics.convention(simpleFlankExtension.performanceMetrics)
      testTargets.convention(simpleFlankExtension.testTargets)

      environmentVariables.convention(simpleFlankExtension.environmentVariables)
      androidExtension.defaultConfig.testInstrumentationRunnerArguments["clearPackageData"]?.let {
        simpleFlankExtension.environmentVariables.convention(mapOf("clearPackageData" to it))
      }
    }

fun registerFlankRun(
    variant: Variant,
    appApk: Apk,
    testApk: Apk,
    flankYaml: RegularFileProperty,
): TaskProvider<FlankRunTask> =
    tasks.register<FlankRunTask>("flankRun${variant.name.capitalize()}") {
      flankJarClasspath.from(flankExecutable)

      serviceAccountCredentials.value(
          simpleFlankExtension.credentialsFile.convention(
              objects.fileProperty().let { property ->
                // Se set the default credentials file only if exists already at configuration time.
                // When the credentials are not there, clients can use the extension.
                val defaultCredentialsFile = rootProject.file("ftl-credentials.json")
                if (defaultCredentialsFile.exists()) property.value { defaultCredentialsFile }
                else property
              }))
      this@register.variant.convention(variant.name)
      hermeticTests.convention(simpleFlankExtension.hermeticTests)
      this.appApk.convention(appApk)
      this.testApk.convention(testApk)
      val dumpShards: String? by project
      this@register.dumpShards.convention(dumpShards.toBoolean())
      val dry: String? by project
      this@register.dry.convention(dry.toBoolean())
      this.flankYaml.convention(flankYaml)
    }

fun registerFlankDoctor(
    variant: Variant,
    appApk: Apk,
    testApk: Apk,
    flankYaml: RegularFileProperty,
): TaskProvider<FlankDoctorTask> =
    tasks.register<FlankDoctorTask>("flankDoctor${variant.name.capitalize()}") {
      flankJarClasspath.from(flankExecutable)
      this@register.variant.convention(variant.name)
      this.appApk.convention(appApk)
      this.testApk.convention(testApk)
      this.flankYaml.convention(flankYaml)
    }

fun configureTasks(
    variant: Variant,
    androidTest: AndroidTest,
    appApk: Apk,
    testApk: Apk,
    commonExtension: CommonExtension<*, *, *, *>
) {
  val yamlWriterTask =
      registerFlankYamlWriter(variant, appApk, testApk, commonExtension, androidTest)
  val flankYaml = yamlWriterTask.get().flankYaml
  registerFlankRun(variant, appApk, testApk, flankYaml)
  registerFlankDoctor(variant, appApk, testApk, flankYaml)
}

fun registerRunFlankTask() {
  tasks.register("flankRun") {
    group = Test.TASK_GROUP
    description = "Run all androidTest using flank."
    dependsOn(tasks.withType<FlankRunTask>())
  }
}

fun registerAuthTask() {
  tasks.register<FlankAuthTask>("flankAuth") { flankJarClasspath.from(flankExecutable) }
}
