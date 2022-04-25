package com.github.flank.gradle.tasks

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations

abstract class FlankDoctorTask : DefaultTask() {
  @get:Inject protected abstract val projectLayout: ProjectLayout
  @get:Inject protected abstract val execOperations: ExecOperations

  @get:Input abstract val variant: Property<String>

  @get:InputFile @get:Classpath @get:Optional abstract val appApk: RegularFileProperty
  @get:InputDirectory @get:Classpath @get:Optional abstract val appApkDir: DirectoryProperty
  @get:InputDirectory @get:Classpath abstract val testApkDir: DirectoryProperty

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
    check(appApk.isPresent xor appApkDir.isPresent) {
      "One, and only one, of appApk or appApkDir must be set"
    }
    execOperations
        .javaexec {
          classpath = flankJarClasspath
          mainClass.set("ftl.Main")
          args = listOf("firebase", "test", "android", "doctor")
          workingDir(projectLayout.buildDirectory.dir("flank/${variant.get()}").get().asFile)
        }
        .assertNormalExitValue()
  }
}
