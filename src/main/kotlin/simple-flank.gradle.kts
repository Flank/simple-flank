import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.*
import com.android.build.api.variant.Variant
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin

val flankExecutable: Configuration by configurations.creating

dependencies { flankExecutable("com.github.flank:flank") { version { prefer("22.04.0") } } }

val simpleFlankExtension = extensions.create<SimpleFlankExtension>("simpleFlank")

plugins.withType(AppPlugin::class.java) {
  tasks.register<FlankVersionTask>("flankVersion") { flankJarClasspath.from(flankExecutable) }

  val appExtension = requireNotNull(extensions.findByType<ApplicationAndroidComponentsExtension>())
  appExtension.onVariants {
    val debugApkDir: Provider<Directory> = it.artifacts.get(SingleArtifact.APK)
    val testApkDir: Provider<Directory>? = it.androidTest?.artifacts?.get(SingleArtifact.APK)

    if (testApkDir != null) {
      val builtArtifactsLoader = it.artifacts.getBuiltArtifactsLoader()
      val apkProvider: Provider<File> =
          debugApkDir.map { apk ->
            file { builtArtifactsLoader.load(apk)?.elements?.single()?.outputFile }
          }
      registerFlankRun(
              it,
              testApkDir,
              builtArtifactsLoader,
              requireNotNull(project.extensions.findByType<ApplicationExtension>()))
          .configure { appApk.fileProvider(apkProvider) }
      tasks.register<FlankDoctorTask>("flankDoctor${it.name.capitalize()}") {
        appApk.fileProvider(apkProvider)
        flankJarClasspath.from(flankExecutable)
        projectId.set(simpleFlankExtension.projectId)
        flankProject.set(getFlankProject())
        this@register.variant.set(it.name)
        useOrchestrator.set(
            provider {
              requireNotNull(project.extensions.findByType<ApplicationExtension>())
                  .testOptions
                  .execution
                  .toUpperCase() == "ANDROIDX_TEST_ORCHESTRATOR"
            })

        device.set(NexusLowRes.deviceForMinSdk(it.minSdkVersion.apiLevel))
        testApk.fileProvider(
            testApkDir.map { apk ->
              file { builtArtifactsLoader.load(apk)?.elements?.single()?.outputFile }
            })
      }
    }
  }
  tasks.register("flankRun") { dependsOn(tasks.withType<FlankRunTask>()) }
  verifyNotDefaultKeystore()
}

plugins.withType(LibraryPlugin::class.java) {
  tasks.register<FlankVersionTask>("flankVersion") { flankJarClasspath.from(flankExecutable) }

  useFixedKeystore()
  val libraryExtension = requireNotNull(extensions.findByType<LibraryAndroidComponentsExtension>())
  libraryExtension.onVariants {
    val testApkDir: Provider<Directory>? = it.androidTest?.artifacts?.get(SingleArtifact.APK)

    if (testApkDir != null) {
      val copySmallApp: Copy = getSmallAppTask()
      val builtArtifactsLoader = it.artifacts.getBuiltArtifactsLoader()
      registerFlankRun(
              it,
              testApkDir,
              builtArtifactsLoader,
              requireNotNull(project.extensions.findByType<LibraryExtension>()))
          .configure {
            dependsOn(copySmallApp)
            appApk.value { files(copySmallApp).asFileTree.matching { include("*.apk") }.singleFile }
          }
    }
  }
  tasks.register("flankRun") { dependsOn(tasks.withType<FlankRunTask>()) }
}

fun registerFlankRun(
    variant: Variant,
    testApkDir: Provider<Directory>,
    builtArtifactsLoader: BuiltArtifactsLoader,
    androidExtension: CommonExtension<*, *, *, *>
): TaskProvider<FlankRunTask> =
    tasks.register<FlankRunTask>("flankRun${variant.name.capitalize()}") {
      flankJarClasspath.from(flankExecutable)

      serviceAccountCredentials.set(simpleFlankExtension.credentialsFile)
      projectId.set(simpleFlankExtension.projectId)
      flankProject.set(getFlankProject())
      this@register.variant.set(variant.name)
      hermeticTests.set(simpleFlankExtension.hermeticTests)
      useOrchestrator.set(
          provider {
            androidExtension.testOptions.execution.toUpperCase() == "ANDROIDX_TEST_ORCHESTRATOR"
          })

      device.set(NexusLowRes.deviceForMinSdk(variant.minSdkVersion.apiLevel))
      testApk.fileProvider(
          testApkDir.map { apk ->
            file { builtArtifactsLoader.load(apk)?.elements?.single()?.outputFile }
          })
      val dumpShards: String by project
      this@register.dumpShards.set(dumpShards.toBoolean())
    }
