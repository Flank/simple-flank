import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import org.gradle.process.ExecOperations

abstract class FlankRunTask : DefaultTask() {
  @get:Inject protected abstract val objectFactory: ObjectFactory
  @get:Inject protected abstract val projectLayout: ProjectLayout
  @get:Inject protected abstract val execOperations: ExecOperations

  @get:Input
  val hermeticTests: Property<Boolean> =
      objectFactory.property(Boolean::class.java).convention(false)

  @get:Input
  val dumpShards: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(false)

  @get:InputFile
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val serviceAccountCredentials: RegularFileProperty

  @get:Input abstract val variant: Property<String>

  @get:InputFile @get:Classpath abstract val appApk: RegularFileProperty
  @get:InputFile @get:Classpath abstract val testApk: RegularFileProperty

  @get:InputFiles @get:Classpath abstract val flankJarClasspath: ConfigurableFileCollection

  @get:Input val localResultsDir = objectFactory.property(String::class.java).convention("results")

  @get:InputFile
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val flankYaml: RegularFileProperty

  private val workingDir: Provider<Directory> =
    variant.flatMap { projectLayout.buildDirectory.dir("flank/$it") }

  @OutputDirectory
  fun getOutputDir(): Provider<Directory> =
      variant.flatMap { workingDir.get().dir(localResultsDir) }

  @get:OutputFile
  val androidShardsOutput = variant.map { workingDir.get().file("android_shards.json") }

  @get:OutputFile
  val flankLinksOutput = variant.map { workingDir.get().file("flank-links.log") }

  init {
    group = Test.TASK_GROUP
    description = "Runs instrumentation tests using flank on firebase test lab."

    outputs.cacheIf { dumpShards.get() || hermeticTests.get() }
    outputs.upToDateWhen { dumpShards.get() || hermeticTests.get() }
  }

  @TaskAction
  fun run() {
    check(serviceAccountCredentials.get().asFile.exists()) {
      "serviceAccountCredential file doesn't exist ${serviceAccountCredentials.get()}"
    }

    getOutputDir().get().asFile.deleteRecursively()
    execOperations
        .javaexec {
          classpath = flankJarClasspath
          mainClass.set("ftl.Main")
          environment(
              mapOf("GOOGLE_APPLICATION_CREDENTIALS" to serviceAccountCredentials.get().asFile))
          args =
              if (dumpShards.get()) {
                listOf("firebase", "test", "android", "run", "--dump-shards")
              } else {
                listOf("firebase", "test", "android", "run")
              }
          workingDir(workingDir)
        }
        .assertNormalExitValue()
  }
}
