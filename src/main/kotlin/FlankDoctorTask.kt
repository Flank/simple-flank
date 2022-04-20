import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.testing.Test
import org.gradle.process.ExecOperations

@CacheableTask
abstract class FlankDoctorTask : DefaultTask() {
  @get:Inject protected abstract val objectFactory: ObjectFactory

  @get:Inject protected abstract val projectLayout: ProjectLayout

  @get:Inject protected abstract val execOperations: ExecOperations

  @get:Input abstract val projectId: Property<String>

  @get:Input abstract val flankProject: Property<String>

  @get:Input abstract val variant: Property<String>

  @get:InputFile @get:Classpath abstract val appApk: RegularFileProperty

  @get:InputFile @get:Classpath abstract val testApk: RegularFileProperty

  @get:Nested abstract val device: Property<AvailableVirtualDevice>

  @get:InputFiles @get:Classpath abstract val flankJarClasspath: ConfigurableFileCollection

  @get:Input abstract val useOrchestrator: Property<Boolean>

  @Input
  fun getWorkingDirRelativePath(): String =
      workingDir.asFile.get().relativeTo(projectLayout.flankDir.get().asFile).path

  private val workingDir = objectFactory.directoryProperty().convention(projectLayout.flankDir)
  fun setUpWorkingDir(configName: String) {
    workingDir.set(projectLayout.buildDirectory.dir("flank/$configName"))
  }

  @get:OutputFile val flankYaml = workingDir.file("flank.yml")

  init {
    group = Test.TASK_GROUP
    description = "Runs instrumentation tests using flank on firebase test lab."
  }

  @TaskAction
  fun run() {
    flankYaml
        .get()
        .asFile
        .writeYaml(
            projectId.get(),
            flankProject.get(),
            variant.get(),
            device.get(),
            appApk.get().asFile.relativeTo(workingDir.get().asFile),
            testApk.get().asFile.relativeTo(workingDir.get().asFile),
            useOrchestrator.get(),
        )

    logger.debug(flankYaml.get().asFile.readText())

    execOperations
        .javaexec {
          classpath = flankJarClasspath
          mainClass.set("ftl.Main")
          args = listOf("firebase", "test", "android", "doctor")
          workingDir(this@FlankDoctorTask.workingDir.asFile.get())
        }
        .assertNormalExitValue()
  }
}
