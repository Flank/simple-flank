import org.gradle.api.Project
import org.gradle.api.tasks.Copy

fun Project.getSmallAppTask(): Copy =
    rootProject.tasks.maybeCreate("copySmallApp", Copy::class.java).apply {
      val pluginJar =
          zipTree(
              SimpleFlankExtension::class.java.classLoader.getResource("small-app.apk")!!
                  .path
                  .split("!")
                  .first())
      from({ pluginJar.single { it.name == "small-app.apk" }.path })
      into("$buildDir/smallApp")
    }
