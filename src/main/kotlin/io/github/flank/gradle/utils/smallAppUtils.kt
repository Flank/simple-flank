package io.github.flank.gradle.utils

import io.github.flank.gradle.SimpleFlankExtension
import io.github.flank.gradle.tasks.CopySmallAppTask
import org.gradle.api.Project

fun Project.getSmallAppTask(): CopySmallAppTask =
    tasks.maybeCreate("copySmallApp", CopySmallAppTask::class.java).apply {
      pluginJar.setFrom(
          zipTree(
              SimpleFlankExtension::class.java.classLoader.getResource("small-app.apk")!!
                  .path
                  .split("!")
                  .first()))
    }
