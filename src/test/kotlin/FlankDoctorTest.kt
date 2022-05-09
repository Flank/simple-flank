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
}
