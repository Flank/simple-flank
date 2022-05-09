package io.github.flank.gradle

import java.io.File
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property

abstract class SimpleFlankExtension(private val project: Project) {
  val credentialsFile: RegularFileProperty =
      project.objects.fileProperty().convention { project.rootProject.file("ftl-credentials.json") }
  val projectId: Property<String> =
      project
          .objects
          .property<String>()
          .convention(credentialsFile.map { defaultProjectId(it.asFile) })

  val hermeticTests =
      project
          .objects
          .property<Boolean>()
          .convention(
              project
                  .providers
                  .gradleProperty("simple-flank.hermeticTests")
                  .map { it.toBoolean() }
                  .orElse(false))

  val devices: ListProperty<Device> = project.objects.listProperty(Device::class.java).value(null)

  private fun defaultProjectId(file: File): String {
    val projectIdRegex = "\"project_id\": \"(.*)\"".toRegex()
    return if (file.exists())
        projectIdRegex.find(file.readText())?.groups?.get(1)?.value
            ?: throw RuntimeException(file.name + " doesn't contain a project_id")
    else ""
  }
}
