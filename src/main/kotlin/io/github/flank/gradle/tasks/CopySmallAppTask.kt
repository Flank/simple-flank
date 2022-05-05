package io.github.flank.gradle.tasks

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class CopySmallAppTask
@Inject
constructor(
    private val fileSystemOperations: FileSystemOperations,
    private val projectLayout: ProjectLayout
) : DefaultTask() {
  @get:InputFiles @get:Classpath abstract val pluginJar: ConfigurableFileCollection

  private val apkName = "small-app.apk"

  @OutputFile
  val appApk: Provider<RegularFile> = projectLayout.buildDirectory.file("$path/smallApp/$apkName")

  @TaskAction
  fun run() {
    fileSystemOperations.copy {
      from({ pluginJar.single { it.name == apkName }.path })
      into(projectLayout.buildDirectory.file("$path/smallApp/").get())
    }
  }
}
