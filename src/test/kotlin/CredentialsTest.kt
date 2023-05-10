import java.io.File
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.gradle.testkit.output
import strikt.gradle.testkit.taskPaths

class CredentialsTest : GradleTest() {
  @get:Rule val userHomeDirectory = TemporaryFolder()

  @Test
  fun `When no service account or flank credentials are provided, build fails`() {
    projectFromResources("app")
    File(testProjectDir.root, "ftl-credentials.json").deleteRecursively()
    File(testProjectDir.root, "build.gradle.kts")
        .appendText("simpleFlank { projectId.set(\"my-project\") }")

    val build = gradleRunner("flankRun", "--stacktrace").forwardOutput().buildAndFail()

    expectThat(build) {
      output.contains(
          "Either a service account credential file should be provided or the flank authentication performed.")
    }
    expectThat(build) { taskPaths(TaskOutcome.FAILED).contains(":flankRunDebug") }
  }

  @Test
  fun `When service account is provided using the default file, build is successful`() {
    projectFromResources("app")

    val build = gradleRunner("flankRun", "--stacktrace").forwardOutput().build()

    expectThat(build) {
      output
          .not()
          .contains(
              "Either a service account credential file should be provided or the flank authentication performed.")
      taskPaths(TaskOutcome.SUCCESS).contains(":flankRunDebug")
    }
  }

  @Test
  fun `When service account is provided using the a custom file, build is successful`() {
    projectFromResources("app")

    val build = gradleRunner("flankRun", "--stacktrace").forwardOutput().build()
    File(testProjectDir.root, "ftl-credentials.json").deleteRecursively()
    File(testProjectDir.root, "custom-credentials.json")
        .appendText("{ \"project_id\": \"custom-project-id\" }")
    File(testProjectDir.root, "build.gradle.kts")
        .appendText("simpleFlank { credentialsFile.set(file(\"custom-credentials.json\")) }")

    expectThat(build) {
      output
          .not()
          .contains(
              "Either a service account credential file should be provided or the flank authentication performed.")
      taskPaths(TaskOutcome.SUCCESS).contains(":flankRunDebug")
    }
  }

  @Test
  fun `When flank Google Account authentication is provided, build is successful`() {
    projectFromResources("app")
    File(testProjectDir.root, "ftl-credentials.json").deleteRecursively()
    File(testProjectDir.root, "build.gradle.kts")
        .appendText("simpleFlank { projectId.set(\"my-project\") }")
    userHomeDirectory.root.resolve(".flank").mkdir()

    val build =
        gradleRunner("flankRun", "-Duser.home=${userHomeDirectory.root.path}", "--stacktrace")
            .forwardOutput()
            .build()

    expectThat(build) {
      output
          .not()
          .contains(
              "Either a service account credential file should be provided or the flank authentication performed.")
      taskPaths(TaskOutcome.SUCCESS).contains(":flankRunDebug")
    }
  }
}
