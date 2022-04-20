import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isNotNull
import strikt.assertions.isOneOf
import strikt.gradle.testkit.outcome
import strikt.gradle.testkit.task

class FlankDoctorTest: GradleTest() {
    @Test
    fun doctor() {
        projectFromResources("app")

        val build = gradleRunner("flankDoctorDebug", "--stacktrace").forwardOutput().build()

        expectThat(build) {
            task(":flankDoctorDebug").isNotNull().outcome.isOneOf(TaskOutcome.SUCCESS, TaskOutcome.FROM_CACHE)
        }
    }
}