import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import java.io.File

abstract class SimpleFlankExtension(private val project: Project) {
  val credentialsFile: RegularFileProperty = project.objects.fileProperty()
    .convention { project.rootProject.file("ftl-credentials.json") }
  val projectId: Property<String> = project.objects.property<String>()
    .convention(
      credentialsFile.map { defaultProjectId(it.asFile) }
    )

  val hermetic = project.objects.property<Boolean>().convention(false)

  private fun defaultProjectId(file: File): String {
    val projectIdRegex = "\"project_id\": \"(.*)\"".toRegex()
    return if (file.exists())
      projectIdRegex.find(file.readText())?.groups?.get(1)?.value ?: throw RuntimeException(file.name + " doesn't contain a project_id")
    else ""
  }
}
