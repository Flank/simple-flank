package io.github.flank.gradle.tasks

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations

abstract class FlankVersionTask @Inject constructor(private val execOperations: ExecOperations) :
    DefaultTask() {
  @get:InputFiles @get:Classpath abstract val flankJarClasspath: ConfigurableFileCollection

  init {
    group = "flank"
    description = "Print flank version."
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
