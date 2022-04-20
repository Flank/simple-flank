import java.io.File
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isNotNull
import strikt.gradle.testkit.isSuccess
import strikt.gradle.testkit.output
import strikt.gradle.testkit.task

class FlankVersionTest : GradleTest() {
  @Test
  fun version() {
    projectFromResources("app")

    val build = gradleRunner("flankVersion", "--stacktrace").forwardOutput().build()

    expectThat(build) {
      task(":flankVersion").isNotNull().isSuccess()
      output.contains("version: v22.04.0")
    }
  }

  @Test
  fun versionCanBeDowngraded() {
    projectFromResources("app")
    File(testProjectDir.root, "build.gradle.kts")
        .appendText(
            """
                dependencies {
                    flankExecutable("com.github.flank:flank:21.11.0")
                }
            """.trimIndent())

    val build = gradleRunner("flankVersion", "--stacktrace").forwardOutput().build()

    expectThat(build) {
      task(":flankVersion").isNotNull().isSuccess()
      output.contains("version: v21.11.0")
    }
  }
}
