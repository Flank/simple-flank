package io.github.flank.gradle.tasks

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

abstract class FlankAuthTask @Inject constructor(private val execOperations: ExecOperations) :
    DefaultTask() {
  @get:InputFiles @get:Classpath abstract val flankJarClasspath: ConfigurableFileCollection

  init {
    group = "flank"
    description =
        "Performs the authentication with a Google Account. https://flank.github.io/flank/#authenticate-with-a-google-account"
  }

  @TaskAction
  fun run() {
    execOperations
        .javaexec {
          classpath = flankJarClasspath
          mainClass.set("ftl.Main")
          args = listOf("auth", "login")
        }
        .assertNormalExitValue()
  }
}
