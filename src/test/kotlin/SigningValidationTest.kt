import java.io.File
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.contains

class SigningValidationTest : GradleTest() {
  @Test
  fun warnIfDefaultSigning() {
    projectFromResources("app")

    val build = gradleRunner("flankRunDebug").forwardOutput().build()

    expectThat(build.output) { contains("Warning: The debug keystore should be set") }
  }

  @Test
  fun failIfDefaultSigningAndHermeticTests() {
    projectFromResources("app")
    File(testProjectDir.root, "build.gradle.kts")
        .appendText(
            """
      simpleFlank {
        hermeticTests.set(true)
      }
    """.trimIndent())

    gradleRunner("flankRunDebug").forwardOutput().buildAndFail()
  }

  @Test
  fun failIfNoSigningAndHermeticTests() {
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
      simpleFlank {
        hermeticTests.set(true)
      }
    """.trimIndent())

    gradleRunner("flankRunDebug").forwardOutput().buildAndFail()
  }

  @Test
  fun workFineIfCustomSigning() {
    projectFromResources("app")
    File(testProjectDir.root, "build.gradle.kts")
        .appendText(
            """
      android {
       signingConfigs {
          named("debug") {
            storeFile = File(projectDir, "someKeystore")
            keyAlias = "debugKey"
            keyPassword = "debugKeystore"
            storePassword = "debugKeystore"
          }
        }
        buildTypes {
          getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
          }
        }
      }
      simpleFlank {
        hermeticTests.set(true)
      }
    """.trimIndent())

    val build = gradleRunner("flankRunDebug").forwardOutput().build()

    expectThat(build.output) { not { contains("The debug keystore should be set") } }
  }

  @Test
  fun workFineNotOnlyWithDebugSigningConfig() {
    projectFromResources("app")
    File(testProjectDir.root, "build.gradle.kts")
        .appendText(
            """
      android {
       signingConfigs {
          create("mySigning") {
            storeFile = File(projectDir, "someKeystore")
            keyAlias = "debugKey"
            keyPassword = "debugKeystore"
            storePassword = "debugKeystore"
          }
        }
        buildTypes {
          getByName("debug") {
            signingConfig = signingConfigs.getByName("mySigning")
          }
        }
      }
      simpleFlank {
        hermeticTests.set(true)
      }
    """.trimIndent())

    gradleRunner("flankRunDebug").forwardOutput().build()
  }
}
