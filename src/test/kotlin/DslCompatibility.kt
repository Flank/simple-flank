import java.io.File
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isNotNull
import strikt.gradle.testkit.isSuccess
import strikt.gradle.testkit.task

class DslCompatibility : GradleTest() {
  @Test
  fun kotlinCompatibility() {
    projectFromResources("app")
    File(testProjectDir.root, "build.gradle.kts")
        .appendText(
            """
            simpleFlank {
              // Changing the credentials file, default: rootProject.file("ftl-credentials.json")
              credentialsFile.set(file("some-credentials.json"))
              
              // Making the tests cacheable
              hermeticTests.set(true)
              // if all modules have hermetic tests, add `simple-flank.hermeticTests=true` to your `gradle.properties`

              // Choosing the devices manually
              // default is NexusLowRes, and the minSdk from the project
              devices.set(listOf(
                io.github.flank.gradle.NexusLowRes(23),
                io.github.flank.gradle.NexusLowRes(30, "es_ES", io.github.flank.gradle.Device.Orientation.landscape),
                io.github.flank.gradle.Device("oriole", 31, "Google", "Pixel 6")
              ))

              // Filtering tests
              testTargets {
                inClass("io.flank.sample.TestClass")
                notInClass("io.flank.sample.NotATestClass", "io.flank.sample.NotATestClassEither")

                small() // or medium() or large()

                annotation("io.flank.test.InstrumentationTest")
                notAnnotation("io.flank.test.Flaky")

                inPackage("io.flank.sample")
                notInPackage("io.flank.external")

                testFile("/sdcard/tmp/testFile.txt")
                notTestFile("/sdcard/tmp/notTestFile.txt")

                regex("BarTest.*")

                filter("com.android.foo.MyCustomFilter", "com.android.foo.AnotherCustomFilter")

                runnerBuilder("com.android.foo.MyCustomBuilder", "com.android.foo.AnotherCustomBuilder")
              }

              // EnvironmentVariables
              // default 
              environmentVariables.set(mapOf("clearPackageData" to "true", "something" to "1", "whatever" to "I don't know"))

              // default extracted from credentials
              projectId.set("my-GCP-project")

              // Downloading files
              directoriesToPull.set(listOf("/sdcard/"))
              filesToDownload.set(listOf("a.txt","b.txt"))
              keepFilePath.set(true)


              // other options
              testTimeout.set("15m")
              recordVideo.set(true)
              numFlakyTestAttempts.set(3)
              failFast.set(true)
              performanceMetrics.set(true)
            }
            """.trimIndent())

    val build = gradleRunner("flankDoctorDebug", "--stacktrace").forwardOutput().build()

    expectThat(build) { task(":flankDoctorDebug").isNotNull().isSuccess() }
  }

  @Test
  fun groovyCompatibility() {
    projectFromResources("library")
    File(testProjectDir.root, "build.gradle")
        .appendText(
            """
            simpleFlank {
              // Changing the credentials file, default: rootProject.file("ftl-credentials.json")
              credentialsFile = file("some-credentials.json")
              
              // Making the tests cacheable
              hermeticTests = true
              // if all modules have hermetic tests, add `simple-flank.hermeticTests=true` to your `gradle.properties`
  
              // Choosing the devices manually
              // default is NexusLowRes, and the minSdk from the project
              devices = [
                new io.github.flank.gradle.NexusLowRes(23),
                new io.github.flank.gradle.NexusLowRes(30, "es_ES", io.github.flank.gradle.Device.Orientation.landscape),
                new io.github.flank.gradle.Device("oriole", 31, "Google", "Pixel 6")
              ]
  
              // Filtering tests
              testTargets {
                inClass("io.flank.sample.TestClass")
                notInClass("io.flank.sample.NotATestClass", "io.flank.sample.NotATestClassEither")
  
                small() // or medium() or large()
  
                annotation("io.flank.test.InstrumentationTest")
                notAnnotation("io.flank.test.Flaky")
  
                inPackage("io.flank.sample")
                notInPackage("io.flank.external")
  
                testFile("/sdcard/tmp/testFile.txt")
                notTestFile("/sdcard/tmp/notTestFile.txt")
  
                regex("BarTest.*")
  
                filter("com.android.foo.MyCustomFilter", "com.android.foo.AnotherCustomFilter")
  
                runnerBuilder("com.android.foo.MyCustomBuilder", "com.android.foo.AnotherCustomBuilder")
              }
  
              // EnvironmentVariables
              // default 
              environmentVariables = [
                clearPackageData: "true",
                something: "1",
                whatever: "I don't know"
              ]
  
              // default extracted from credentials
              projectId = "my-GCP-project"
  
              // Downloading files
              directoriesToPull = ["/sdcard/"]
              filesToDownload = ["a.txt","b.txt"]
              keepFilePath = true
  
              // other options
              testTimeout = "15m"
              recordVideo = true
              numFlakyTestAttempts = 3
              failFast = true
              performanceMetrics = true
            }
        """.trimIndent())

    val build = gradleRunner("flankDoctorDebug", "--stacktrace").forwardOutput().build()

    expectThat(build) { task(":flankDoctorDebug").isNotNull().isSuccess() }
  }
}
