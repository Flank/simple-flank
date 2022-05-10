package io.github.flank.gradle.tasks

import io.github.flank.gradle.Device
import java.io.File
import javax.inject.Inject
import org.gradle.api.file.Directory
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "This task is faster to run than to download")
abstract class FlankYmlWriterTask
@Inject
constructor(objectFactory: ObjectFactory, private val projectLayout: ProjectLayout) :
    BaseFlankApkTask() {
  @get:Input abstract val projectId: Property<String>
  @get:Input abstract val flankProject: Property<String>
  @get:Input abstract val variant: Property<String>

  @get:Nested abstract val devices: ListProperty<Device>

  @get:Input abstract val useOrchestrator: Property<Boolean>

  @get:Input
  val maxTestShards: Property<Int> = objectFactory.property(Int::class.java).convention(40)
  @get:Input val shardTime: Property<Int> = objectFactory.property(Int::class.java).convention(120)

  @get:Input @get:Optional abstract val directoriesToPull: ListProperty<String>
  @get:Input @get:Optional abstract val filesToDownload: ListProperty<String>
  @get:Input @get:Optional abstract val keepFilePath: Property<Boolean>

  @get:Input @get:Optional abstract val environmentVariables: MapProperty<String, String>

  @get:Input @get:Optional abstract val additionalGcloudOptions: MapProperty<String, String>
  @get:Input @get:Optional abstract val additionalFlankOptions: MapProperty<String, String>

  @get:Input @get:Optional abstract val skipConfigValidation: Property<Boolean>

  private val workingDir: Provider<Directory> =
      variant.flatMap { projectLayout.buildDirectory.dir("flank/$it") }

  @get:OutputFile
  val flankYaml: RegularFileProperty =
      objectFactory.fileProperty().value(variant.map { workingDir.get().file("flank.yml") })

  init {
    group = "flank"
    description = "Write flank YAML configuration."
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
            devices.get(),
            appApk.get().singleFile().asFile.relativeTo(workingDir.get().asFile),
            testApk.get().singleFile().asFile.relativeTo(workingDir.get().asFile),
            useOrchestrator.get(),
        )

    logger.debug(flankYaml.get().asFile.readText())
  }

  private fun optional(key: String, provider: Provider<*>, indent: Int): String =
      if (provider.isPresent) {
        "\n" + " ".repeat(indent) + "$key: ${provider.get()}"
      } else ""

  private fun optional(key: String, provider: ListProperty<*>, indent: Int): String =
      if (provider.isPresent && provider.get().isNotEmpty()) {
        provider
            .get()
            .joinToString(
                prefix = "\n" + " ".repeat(indent) + "$key:\n" + " ".repeat(indent + 2) + "- ",
                separator = "\n" + " ".repeat(indent + 2) + "- ")
      } else ""

  private fun optional(key: String, provider: MapProperty<*, *>, indent: Int): String =
      if (provider.isPresent && provider.get().isNotEmpty()) {
        provider
            .get()
            .map { "${it.key}: ${it.value}" }
            .joinToString(
                prefix = "\n" + " ".repeat(indent) + "$key:\n" + " ".repeat(indent + 2),
                separator = "\n" + " ".repeat(indent + 2))
      } else ""

  private fun optionalValue(key: String, value: Any?, indent: Int): String =
      if (value != null) {
        "\n" + " ".repeat(indent) + "$key: $value"
      } else ""

  private fun File.writeYaml(
      projectId: String,
      flankProject: String,
      variant: String,
      devices: List<Device>,
      appApk: File,
      testApk: File,
      useOrchestrator: Boolean,
  ) {
    writeText(
        """
gcloud:
  app: $appApk
  test: $testApk
  device: ${
    devices.joinToString("") { device -> """
    - model: ${device.id}
      version: "${device.osVersion}"""" +
        optionalValue("locale",device.locale,6) +
        optionalValue("orientation",device.orientation,6)
    }
  }
  results-history-name: $flankProject.$variant
  use-orchestrator: $useOrchestrator
""" +
            optional("directories-to-pull", directoriesToPull, 2) +
            optional("environment-variables", environmentVariables, 2) +
            additionalGcloudOptions
                .getOrElse(emptyMap())
                .map { optionalValue(it.key, it.value, 2) }
                .joinToString(separator = ""))
    appendText(
        """
flank:
  smart-flank-gcs-path: gs://$projectId/$flankProject.$variant/JUnitReport.xml
  max-test-shards: ${maxTestShards.get()}
  shard-time: ${shardTime.get()}
  default-test-time: 1
  use-average-test-time-for-new-tests: true
  output-style: single
""" +
            optional("files-to-download", filesToDownload, 2) +
            optional("keep-file-path", keepFilePath, 2) +
            optional("skip-config-validation", skipConfigValidation, 2) +
            additionalFlankOptions
                .getOrElse(emptyMap())
                .map { optionalValue(it.key, it.value, 2) }
                .joinToString(separator = ""))
  }
}
