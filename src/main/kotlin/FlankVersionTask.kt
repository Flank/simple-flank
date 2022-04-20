import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.*
import org.gradle.api.tasks.testing.Test
import org.gradle.process.ExecOperations

abstract class FlankVersionTask : DefaultTask() {
  @get:Inject protected abstract val execOperations: ExecOperations

  @get:InputFiles @get:Classpath abstract val flankJarClasspath: ConfigurableFileCollection

  init {
    group = Test.TASK_GROUP
    description = "Runs instrumentation tests using flank on firebase test lab."
  }

  @TaskAction
  fun run() {
    execOperations
        .javaexec {
          classpath = flankJarClasspath
          mainClass.set("ftl.Main")
          args = listOf("-v")
        }
        .assertNormalExitValue()
  }
}
