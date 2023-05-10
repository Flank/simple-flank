package io.github.flank.gradle.tasks

import javax.inject.Inject
import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations

abstract class FlankRunTask
@Inject
constructor(
    objectFactory: ObjectFactory,
    private val projectLayout: ProjectLayout,
    private val execOperations: ExecOperations
) : BaseFlankApkTask() {

  @get:Input
  val hermeticTests: Property<Boolean> =
      objectFactory.property(Boolean::class.java).convention(false)

  @get:Input
  val dumpShards: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(false)

  @get:Input
  val dry: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(false)

  @get:InputFiles
  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val serviceAccountCredentials: RegularFileProperty

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.ABSOLUTE)
  abstract val flankAuthDirectory: DirectoryProperty

  @get:Input abstract val variant: Property<String>

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

  @get:OutputFile val flankLinksOutput = variant.map { workingDir.get().file("flank-links.log") }

  init {
    group = "flank"
    description = "Runs instrumentation tests using flank on firebase test lab."

    outputs.cacheIf { dumpShards.get() || hermeticTests.get() }
    outputs.upToDateWhen { dumpShards.get() || hermeticTests.get() }
  }

  @TaskAction
  fun run() {
    checkAuthentication()

    getOutputDir().get().asFile.deleteRecursively()
    execOperations
        .javaexec {
          isIgnoreExitValue = true
          classpath = flankJarClasspath
          mainClass.set("ftl.Main")
          serviceAccountCredentials.orNull?.takeIf { it.asFile.exists() }?.let { credentialsFile ->
            environment(mapOf("GOOGLE_APPLICATION_CREDENTIALS" to credentialsFile))
          }
          args = listOf("firebase", "test", "android", "run", "-c=${flankYaml.get()}")
          if (dumpShards.get()) args("--dump-shards")
          if (dry.get()) args("--dry")

          logger.lifecycle(args.toString())

          workingDir(this@FlankRunTask.workingDir)
        }
        .run {
          if (exitValue == NO_TESTS_EXIT_CODE) {
            logger.warn(
                """
                ${
              testApk.get().singleFile().asFile.relativeTo(projectLayout.projectDirectory.asFile)
            } doesn't contain any test or they are filtered out
                For projects without tests it's better not to apply this plugin, but if you need to apply it, the build 
                would be faster if you really deactivate the androidTests for this variant, like: 

                androidComponents.beforeVariants(selector().withFlavor("dimension" to "flavorName")) {
                  it.enableAndroidTest = false
                }""".trimIndent())
          } else {
            assertNormalExitValue()
          }
        }
  }

  private fun checkAuthentication() {
    val isCredentialsFileProvided = serviceAccountCredentials.orNull?.asFile?.exists() ?: false
    val isFlankAuthenticationProvided = flankAuthDirectory.get().asFile.exists()
    check(isCredentialsFileProvided || isFlankAuthenticationProvided) {
      """
        Either a service account credential file should be provided or the flank authentication performed.
        You can:
          - Declare the service account credentials in the ftl-credentials.json file on your rootProject
          - Declare the service account credentials in a custom path via the simple flank's extension simpleFlank { credentialsFile = "path/to/file.json" }
          - Perform the authentication with a Google Account via "./gradlew flankAuth"
      """.trimIndent()
    }
  }

  companion object {
    const val NO_TESTS_EXIT_CODE = 3
  }
}
