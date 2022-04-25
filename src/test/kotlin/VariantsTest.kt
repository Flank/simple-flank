import java.io.File
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isNotNull
import strikt.gradle.testkit.task

class VariantsTest : GradleTest() {

  @Test
  fun useTheDefaultBuildType() {
    projectFromResources("app")
    File(testProjectDir.root, "build.gradle.kts")
        .appendText(
            "tasks.withType<io.github.flank.gradle.tasks.FlankRunTask>().configureEach { enabled = false }")

    val build = gradleRunner("flankRun").forwardOutput().build()

    expectThat(build) {
      task(":packageDebug").isNotNull()
      task(":packageDebugAndroidTest").isNotNull()
      task(":flankRun").isNotNull()
    }
  }

  @Test
  fun allowToChangeTheBuildType() {
    projectFromResources("app")
    File(testProjectDir.root, "build.gradle.kts")
        .appendText(
            """
                android {
                    testBuildType = "beta"
                
                    buildTypes {
                        create("beta")
                    }
                }
                tasks.withType<io.github.flank.gradle.tasks.FlankRunTask>().configureEach { enabled = false }
            """.trimIndent())

    val build = gradleRunner("flankRun").forwardOutput().build()

    expectThat(build) {
      task(":packageBeta").isNotNull()
      task(":packageBetaAndroidTest").isNotNull()
      task(":flankRunBeta").isNotNull()
      task(":flankRun").isNotNull()
    }
  }

  @Test
  fun allowFlavors() {
    projectFromResources("app")
    File(testProjectDir.root, "build.gradle.kts")
        .appendText(
            """
                android {
                    flavorDimensions += listOf("env")
                
                    productFlavors {
                        create("pre") {
                            dimension = "env"
                        }
                        create("pro") {
                            dimension = "env"
                        }
                    }
                }
                tasks.withType<io.github.flank.gradle.tasks.FlankRunTask>().configureEach { enabled = false }
            """.trimIndent())

    val build = gradleRunner("flankRun").forwardOutput().build()

    expectThat(build) {
      task(":packagePreDebug").isNotNull()
      task(":packagePreDebugAndroidTest").isNotNull()
      task(":packageProDebug").isNotNull()
      task(":packageProDebugAndroidTest").isNotNull()
      task(":flankRun").isNotNull()
    }
  }
}
