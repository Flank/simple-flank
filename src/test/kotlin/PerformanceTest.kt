import java.io.File
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isNotNull
import strikt.gradle.testkit.isFromCache
import strikt.gradle.testkit.isSuccess
import strikt.gradle.testkit.output
import strikt.gradle.testkit.task

class PerformanceTest : GradleTest() {
  private val samplePath = "app/runflank"

  @Test
  fun libraryFlankRunIsCacheable() {
    projectFromResources("library")
    File(testProjectDir.root, "build.gradle")
        .appendText(
            """
                simpleFlank {
                  credentialsFile.set(project.rootProject.file("some-credentials.json"))
                }
            """.trimIndent())

    gradleRunner("flankRun", "-PdumpShards=true", "--stacktrace").forwardOutput().build()
    gradleRunner("clean").build()
    val build =
        gradleRunner("flankRun", "-PdumpShards=true", "--stacktrace").forwardOutput().build()

    expectThat(build) {
      task(":flankRunDebug").isNotNull().isFromCache()
      task(":flankRun").isNotNull()
    }
  }

  @Test
  fun appFlankRunDumpShardsIsCacheable() {
    projectFromResources("app")

    gradleRunner("flankRun", "-PdumpShards=true", "--stacktrace").forwardOutput().build()
    gradleRunner("clean").build()
    val build =
        gradleRunner("flankRun", "-PdumpShards=true", "--stacktrace").forwardOutput().build()

    expectThat(build) {
      task(":flankRunDebug").isNotNull().isFromCache()
      task(":flankRun").isNotNull()
    }
  }

  @Test
  fun appFlankRunIsNotCacheableByDefault() {
    projectFromResources("app")

    gradleRunner("flankRun", "--stacktrace").forwardOutput().build()
    gradleRunner("clean").build()
    val build = gradleRunner("flankRun", "--stacktrace").forwardOutput().build()

    expectThat(build) {
      task(":flankRunDebug").isNotNull().isSuccess()
      task(":flankRun").isNotNull()
    }
  }

  @Test
  fun appFlankRunIsCacheable() {
    projectFromResources("app")
    File(testProjectDir.root, "build.gradle.kts")
        .appendText(
            """
                simpleFlank {
                  hermeticTests.set(true)
                }
            """.trimIndent())

    gradleRunner("flankRun", "--stacktrace").forwardOutput().build()
    gradleRunner("clean").build()
    val build = gradleRunner("flankRun", "--stacktrace").forwardOutput().build()

    expectThat(build) {
      task(":flankRunDebug").isNotNull().isFromCache()
      task(":flankRun").isNotNull()
    }
  }

  @Test
  fun appFlankRunWithPropertyIsCacheable() {
    projectFromResources("app")
    File(testProjectDir.root, "gradle.properties").appendText("simple-flank.hermeticTests=true\n")

    gradleRunner("flankRun", "--stacktrace").forwardOutput().build()
    gradleRunner("clean").build()
    val build = gradleRunner("flankRun", "--stacktrace").forwardOutput().build()

    expectThat(build) {
      task(":flankRunDebug").isNotNull().isFromCache()
      task(":flankRun").isNotNull()
    }
  }

  @Test
  fun appConfigurationCacheCompatible() {
    projectFromResources("app")

    val storingBuild: BuildResult =
        try {
          gradleRunner(
                  "flankRun",
                  "-PdumpShards=true",
                  "--configuration-cache",
                  "--dry-run",
                  "--stacktrace")
              .forwardOutput()
              .build()
        } catch (e: UnexpectedBuildFailure) {
          val target = File("build/reports/configuration-cache/$samplePath").apply { mkdirs() }
          File(testProjectDir.root, "build/reports/configuration-cache/")
              .copyRecursively(target, true)
          throw e
        }
    expectThat(storingBuild.output) {
      contains("0 problems were found storing the configuration cache.")
      contains("Configuration cache entry stored.")
    }

    val build =
        gradleRunner(
                "flankRun",
                "-PdumpShards=true",
                "--configuration-cache",
                "--no-build-cache",
                "--stacktrace")
            .forwardOutput()
            .build()

    expectThat(build) {
      output.contains("Reusing configuration cache.")
      task(":flankRunDebug").isNotNull().isSuccess()
    }
  }

  @Test
  fun libraryConfigurationCacheCompatible() {
    projectFromResources("library")
    File(testProjectDir.root, "build.gradle")
        .appendText(
            """
                simpleFlank {
                  credentialsFile.set(project.rootProject.file("some-credentials.json"))
                }
            """.trimIndent())

    val storingBuild: BuildResult =
        try {
          gradleRunner(
                  "flankRun",
                  "-PdumpShards=true",
                  "--configuration-cache",
                  "--dry-run",
                  "--stacktrace")
              .forwardOutput()
              .build()
        } catch (e: UnexpectedBuildFailure) {
          val target = File("build/reports/configuration-cache/$samplePath").apply { mkdirs() }
          File(testProjectDir.root, "build/reports/configuration-cache/")
              .copyRecursively(target, true)
          throw e
        }
    expectThat(storingBuild.output) {
      contains("0 problems were found storing the configuration cache.")
      contains("Configuration cache entry stored.")
    }

    val build =
        gradleRunner(
                "flankRun",
                "-PdumpShards=true",
                "--configuration-cache",
                "--no-build-cache",
                "--stacktrace")
            .forwardOutput()
            .build()

    expectThat(build) {
      output.contains("Reusing configuration cache.")
      task(":flankRunDebug").isNotNull().isSuccess()
    }
  }
}
