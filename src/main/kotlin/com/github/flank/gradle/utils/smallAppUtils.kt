package com.github.flank.gradle.utils

import com.github.flank.gradle.SimpleFlankExtension
import com.github.flank.gradle.tasks.CopySmallAppTask
import org.gradle.api.Project

fun Project.getSmallAppTask(): CopySmallAppTask =
    rootProject.tasks.maybeCreate("copySmallApp", CopySmallAppTask::class.java).apply {
      pluginJar.setFrom(
          zipTree(
              SimpleFlankExtension::class.java.classLoader.getResource("small-app.apk")!!
                  .path
                  .split("!")
                  .first()))
    }
