import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

abstract class GradleTest {
    @get:Rule
    val testProjectDir = TemporaryFolder()

    fun projectFromResources(resourceFolder: String) {
        File(javaClass.getResource(resourceFolder)!!.toURI()).copyRecursively(testProjectDir.root)
    }

    fun gradleRunner(vararg arguments: String): GradleRunner {
        return GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments(arguments.asList())
            .withPluginClasspath()
    }

}