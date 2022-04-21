import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.testing.Test
import org.gradle.process.ExecOperations

abstract class FlankDoctorTask : DefaultTask() {
  @get:Inject protected abstract val projectLayout: ProjectLayout
  @get:Inject protected abstract val execOperations: ExecOperations

  @get:Input abstract val variant: Property<String>

  @get:InputFile @get:Classpath abstract val appApk: RegularFileProperty
  @get:InputFile @get:Classpath abstract val testApk: RegularFileProperty

  @get:InputFiles @get:Classpath abstract val flankJarClasspath: ConfigurableFileCollection

  @get:InputFile
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val flankYaml: RegularFileProperty

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
          args = listOf("firebase", "test", "android", "doctor")
          workingDir(
              projectLayout
                  .buildDirectory
                  .dir("flank/${variant.get()}")
                  .get()
                  .asFile)
        }
        .assertNormalExitValue()
  }
}
