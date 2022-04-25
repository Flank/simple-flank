package io.github.flank.gradle.utils

import org.gradle.api.Project

fun Project.getFlankProject(): String {
  // Add the rootProject name in case the user changed the group
  val projectPrefix: String =
      if (rootProject == project || group.toString().contains(rootProject.name)) {
        ""
      } else {
        rootProject.name
      }
  return listOf(projectPrefix, group.toString(), name).filter { it.isNotEmpty() }.joinToString(".")
}
