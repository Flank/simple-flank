package io.github.flank.gradle.tasks

import io.github.flank.gradle.Device
import java.io.File
import javax.inject.Inject
import org.gradle.api.file.Directory
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
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
          - model: "${device.id}"
            version: "${device.osVersion}"""" +
              if (device.locale!=null) """
            locale: "${device.locale}"""" else {""} +
              if (device.orientation!=null) """
            orientation: "${device.orientation}"""" else {""}
          }
        }

        use-orchestrator: $useOrchestrator
        auto-google-login: false
        record-video: false
        performance-metrics: false
        timeout: 15m
        results-history-name: $flankProject.$variant
        num-flaky-test-attempts: 0

      flank:
        max-test-shards: 40
        shard-time: 120
        smart-flank-gcs-path: gs://$projectId/$flankProject.$variant/JUnitReport.xml
        keep-file-path: false
        ignore-failed-tests: false
        disable-sharding: false
        smart-flank-disable-upload: false
        legacy-junit-result: false
        full-junit-result: false
        output-style: single
        default-test-time: 1.0
        use-average-test-time-for-new-tests: true
    """.trimIndent())
  }
}
