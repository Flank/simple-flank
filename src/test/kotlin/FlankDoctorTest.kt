import java.io.File
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isNotNull
import strikt.gradle.testkit.isSuccess
import strikt.gradle.testkit.task

class FlankDoctorTest : GradleTest() {
  @Test
  fun doctor() {
    projectFromResources("app")
    File(testProjectDir.root, "build.gradle.kts")
        .appendText(
            """
      android {
        defaultConfig {
          testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunnercillo"
        }
      }
    """.trimIndent())

    val build = gradleRunner("flankDoctorDebug", "--stacktrace").forwardOutput().build()

    expectThat(build) { task(":flankDoctorDebug").isNotNull().isSuccess() }
  }

  @Test
  fun validYamlWithCustomDevice() {
    projectFromResources("app")
    File(testProjectDir.root, "build.gradle.kts")
        .appendText(
            """
        tasks.withType<io.github.flank.gradle.tasks.FlankYmlWriterTask>().configureEach {
          devices.set(listOf(
            io.github.flank.gradle.NexusLowRes(28, "it", io.github.flank.gradle.Device.Orientation.portrait)
          ))
        }
        """.trimIndent())

    val build = gradleRunner("flankDoctorDebug", "--stacktrace").forwardOutput().build()

    expectThat(build) { task(":flankDoctorDebug").isNotNull().isSuccess() }
  }

  @Test
  fun validYamlWithMultipleDevices() {
    projectFromResources("app")
    File(testProjectDir.root, "build.gradle.kts")
        .appendText(
            """
        simpleFlank {
          devices.set(listOf(
            io.github.flank.gradle.NexusLowRes(23),
            io.github.flank.gradle.NexusLowRes(30, "es_ES", io.github.flank.gradle.Device.Orientation.landscape),
            io.github.flank.gradle.Device("oriole",31,"Google","Pixel 6")
          ))
        }
        """.trimIndent())

    val build = gradleRunner("flankDoctorDebug", "--stacktrace").forwardOutput().build()

    expectThat(build) { task(":flankDoctorDebug").isNotNull().isSuccess() }
  }

  @Test
  fun validYamlDownloadingFiles() {
    projectFromResources("app")
    File(testProjectDir.root, "build.gradle.kts")
        .appendText(
            """
        tasks.withType<io.github.flank.gradle.tasks.FlankYmlWriterTask>().configureEach {
          directoriesToPull.set(listOf("/sdcard/"))
          filesToDownload.set(listOf("a.txt","b.txt"))
          keepFilePath.set(true)
        }
        """.trimIndent())

    val build = gradleRunner("flankDoctorDebug", "--stacktrace").forwardOutput().build()

    expectThat(build) { task(":flankDoctorDebug").isNotNull().isSuccess() }
  }

  @Test
  fun validYamlWithEnvVars() {
    projectFromResources("app")
    File(testProjectDir.root, "build.gradle.kts")
        .appendText(
            """
        tasks.withType<io.github.flank.gradle.tasks.FlankYmlWriterTask>().configureEach {
          environmentVariables.set(mapOf("clearPackageData" to "true", "something" to "1", "whatever" to "I don't know"))
        }
        """.trimIndent())

    val build = gradleRunner("flankDoctorDebug", "--stacktrace").forwardOutput().build()

    expectThat(build) { task(":flankDoctorDebug").isNotNull().isSuccess() }
  }

  @Test
  fun validYamlWithAdditionalOptions() {
    projectFromResources("app")
    File(testProjectDir.root, "build.gradle.kts")
        .appendText(
            """
        tasks.withType<io.github.flank.gradle.tasks.FlankYmlWriterTask>().configureEach {
          additionalGcloudOptions.set(mapOf("test-runner-class" to "a.b.c", "record-video" to "true"))
          additionalFlankOptions.set(mapOf("custom-sharding-json" to "./custom_sharding.json"))
        }
        """.trimIndent())

    val build = gradleRunner("flankDoctorDebug", "--stacktrace").forwardOutput().build()

    expectThat(build) { task(":flankDoctorDebug").isNotNull().isSuccess() }
  }

  @Test
  fun validYamlSkipConfigValidation() {
    projectFromResources("app")
    File(testProjectDir.root, "build.gradle.kts")
        .appendText(
            """
        tasks.withType<io.github.flank.gradle.tasks.FlankYmlWriterTask>().configureEach {
          shardTime.set(-2)
          skipConfigValidation.set(true)
        }
        """.trimIndent())

    val build = gradleRunner("flankDoctorDebug", "--stacktrace").forwardOutput().build()

    expectThat(build) { task(":flankDoctorDebug").isNotNull().isSuccess() }
  }

  @Test
  fun validYamlWithTestTargetsDSL() {
    projectFromResources("app")
    File(testProjectDir.root, "build.gradle.kts")
        .appendText(
            """
        simpleFlank {
          testTargets {
            inClass("io.flank.sample.TestClass")
            notInClass("io.flank.sample.NotATestClass")
            small()
            notAnnotation("io.flank.test.Flaky")
            inPackage("io.flank.sample")
          }
        }
        """.trimIndent())

    val build = gradleRunner("flankDoctorDebug", "--stacktrace").forwardOutput().build()

    expectThat(build) { task(":flankDoctorDebug").isNotNull().isSuccess() }
  }
}
