package io.github.flank.gradle

import java.io.File
import org.gradle.api.Action
import org.gradle.api.HasImplicitReceiver
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property

abstract class SimpleFlankExtension(private val project: Project) {
  private val optionalDefaultCredentialFileProvider =
      project.objects.fileProperty().apply {
        // Se set the default credentials file only if exists already at configuration time.
        // When the credentials are not there, clients can use the extension.
        val defaultCredentialsFile = project.rootProject.file("ftl-credentials.json")
        if (defaultCredentialsFile.exists()) {
          value { defaultCredentialsFile }
        }
      }

  val credentialsFile: RegularFileProperty =
      project.objects.fileProperty().convention(optionalDefaultCredentialFileProvider)

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

  abstract val testTimeout: Property<String>

  abstract val directoriesToPull: ListProperty<String>
  abstract val filesToDownload: ListProperty<String>
  abstract val keepFilePath: Property<Boolean>

  abstract val recordVideo: Property<Boolean>
  abstract val numFlakyTestAttempts: Property<Int>
  abstract val failFast: Property<Boolean>
  abstract val performanceMetrics: Property<Boolean>
  abstract val testTargets: ListProperty<String>
  open fun testTargets(action: Action<in TestTargetExtension>) {
    action.execute(TestTargetExtension(testTargets))
  }
  abstract val environmentVariables: MapProperty<String, String>

  private fun defaultProjectId(file: File): String {
    val projectIdRegex = "\"project_id\": \"(.*)\"".toRegex()
    return if (file.exists())
        projectIdRegex.find(file.readText())?.groups?.get(1)?.value
            ?: throw RuntimeException(file.name + " doesn't contain a project_id")
    else ""
  }

  @HasImplicitReceiver
  open inner class TestTargetExtension(private val testTargets: ListProperty<String>) {
    fun inClass(vararg fullyClassifiedClass: String) {
      testTargets.add("class ${fullyClassifiedClass.joinToString(",")}")
    }
    fun notInClass(vararg fullyClassifiedClass: String) {
      testTargets.add("notClass ${fullyClassifiedClass.joinToString(",")}")
    }

    fun testFile(filePath: String) {
      testTargets.add("testFile $filePath")
    }
    fun notTestFile(filePath: String) {
      testTargets.add("notTestFile $filePath")
    }

    fun inPackage(packageString: String) {
      testTargets.add("package $packageString")
    }
    fun notInPackage(packageString: String) {
      testTargets.add("notPackage $packageString")
    }

    fun regex(regex: String) {
      testTargets.add("tests_regex $regex")
    }

    fun small() {
      testTargets.add("size small")
    }
    fun medium() {
      testTargets.add("size medium")
    }
    fun large() {
      testTargets.add("size large")
    }

    fun annotation(vararg annotation: String) {
      testTargets.add("annotation ${annotation.joinToString(",")}")
    }
    fun notAnnotation(vararg annotation: String) {
      testTargets.add("notAnnotation ${annotation.joinToString(",")}")
    }

    fun filter(vararg filter: String) {
      testTargets.add("notAnnotation ${filter.joinToString(",")}")
    }

    fun runnerBuilder(vararg builder: String) {
      testTargets.add("runnerBuilder ${builder.joinToString(",")}")
    }
  }
}
