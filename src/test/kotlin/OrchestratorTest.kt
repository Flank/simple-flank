import java.io.File
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.contains

class OrchestratorTest : GradleTest() {
  @Test
  fun disabledByDefault() {
    projectFromResources("app")

    gradleRunner("flankDoctor").build()

    val flankYaml = File(testProjectDir.root, "build/flank/debug/flank.yml").readText()
    expectThat(flankYaml) { contains("use-orchestrator: false") }
  }

  @Test
  fun enableOrchestrator() {
    projectFromResources("app")
    File(testProjectDir.root, "build.gradle.kts")
        .appendText(
            """
      android {
        defaultConfig {
          testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
          testInstrumentationRunnerArguments += mapOf(
            "clearPackageData" to "true",
            "disableAnalytics" to "true",
          )
        }

        testOptions {
          execution = "ANDROIDX_TEST_ORCHESTRATOR"
        }
      }
      dependencies {
        androidTestImplementation("androidx.test:runner:1.1.0")
        androidTestUtil("androidx.test:orchestrator:1.1.0")
      }
    """.trimIndent())

    File(testProjectDir.root, "gradle.properties")
        .appendText("""
      android.useAndroidX=true
    """.trimIndent())

    gradleRunner("flankDoctor").forwardOutput().build()

    val flankYaml = File(testProjectDir.root, "build/flank/debug/flank.yml").readText()
    expectThat(flankYaml) { contains("use-orchestrator: true") }
  }
}
