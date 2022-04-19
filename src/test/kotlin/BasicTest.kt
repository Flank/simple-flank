import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isNotNull
import strikt.gradle.testkit.isFromCache
import strikt.gradle.testkit.task
import java.io.File

internal class BasicTest: GradleTest() {

    @Test
    internal fun libraryWorks() {
        projectFromResources("library")
        File(testProjectDir.root, "build.gradle").appendText(
            """
                simpleFlank {
                  credentialsFile.set(project.rootProject.file("some-credentials.json"))
                }
            """.trimIndent()
        )

        gradleRunner("flankRun", "--stacktrace").forwardOutput().build()
        gradleRunner("clean").build()
        val build = gradleRunner("flankRun", "--stacktrace", "--info").forwardOutput().build()

        expectThat(build) {
            task(":flankRunDebug").isNotNull().isFromCache()
            task(":flankRun").isNotNull()
        }
    }

    @Test
    internal fun appWorks() {
        projectFromResources("app")

        gradleRunner("flankRun", "--stacktrace").forwardOutput().build()
        gradleRunner("clean").build()
        val build = gradleRunner("flankRun", "--stacktrace").forwardOutput().build()

        expectThat(build) {
            task(":flankRunDebug").isNotNull().isFromCache()
            task(":flankRun").isNotNull()
        }
    }
}