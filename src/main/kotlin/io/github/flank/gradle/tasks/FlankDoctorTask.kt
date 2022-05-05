package io.github.flank.gradle.tasks

import javax.inject.Inject
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations

abstract class FlankDoctorTask : BaseFlankApkTask() {
  @get:Inject protected abstract val projectLayout: ProjectLayout
  @get:Inject protected abstract val execOperations: ExecOperations

  @get:Input abstract val variant: Property<String>

  @get:InputFiles @get:Classpath abstract val flankJarClasspath: ConfigurableFileCollection

  @get:InputFile
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val flankYaml: RegularFileProperty

  init {
    group = "flank"
    description = "Check for errors in the YAML."
  }

  @TaskAction
  fun run() {
    execOperations
        .javaexec {
          classpath = flankJarClasspath
          mainClass.set("ftl.Main")
          args = listOf("firebase", "test", "android", "doctor", "-c=${flankYaml.get()}")
          workingDir(projectLayout.buildDirectory.dir("flank/${variant.get()}").get())
        }
        .assertNormalExitValue()
  }
}
