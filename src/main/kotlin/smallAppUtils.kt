import org.gradle.api.Project

fun Project.getSmallAppTask(): CopySmallApp =
    rootProject.tasks.maybeCreate("copySmallApp", CopySmallApp::class.java).apply {
      pluginJar.setFrom(
          zipTree(
              SimpleFlankExtension::class.java.classLoader.getResource("small-app.apk")!!
                  .path
                  .split("!")
                  .first()))
    }
