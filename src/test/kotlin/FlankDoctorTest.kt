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
}
