import java.io.File
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isNotNull
import strikt.gradle.testkit.*

class EmptyTestApkTest : GradleTest() {
  @Test
  fun projectWithoutTestsDoesntFail() {
    projectFromResources("app")
    File(testProjectDir.root, "src/androidTest").deleteRecursively()

    val build = gradleRunner("flankRun", "--stacktrace").forwardOutput().build()

    expectThat(build) {
      task(":flankRunDebug").isNotNull()
      task(":flankRun").isNotNull()
    }
  }

  @Test
  fun projectUsingOldFlankVersionWithoutTestsDoesntFail() {
    projectFromResources("app")
    File(testProjectDir.root, "src/androidTest").deleteRecursively()
    File(testProjectDir.root, "build.gradle.kts")
        .appendText(
            """
                dependencies {
                  flankExecutable("com.github.flank:flank:22.04.0")
                }
            """.trimIndent())

    val build = gradleRunner("flankRun", "--stacktrace").forwardOutput().buildAndFail()

    expectThat(build) { task(":flankRunDebug").isNotNull().isFailed() }
  }
}
