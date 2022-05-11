# simple-flank
simple-flank is a new gradle plugin with a clear focus: make the setup as simple as possible.

Applied to any application or library module it creates a task called `runFlank` that will run all the android tests (all flavors, debug buildType by default)

Tests will be executed using NexusLowRes emulators, and the lower sdk possible (between minSdk and available SDKs at Firebase)

# Setup

Apply the plugin on the application/library modules you want to run tests:
```
plugins {
  ...
  id("io.github.flank.gradle.simple-flank")
}
```

add your firebase credentials to the rootproject as `ftl-credentials.json`

That's it, run `./gradlew runFlank` and get the results.

# Optional configuration

```kotlin
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
    small()
    notAnnotation("io.flank.test.Flaky")
    inPackage("io.flank.sample")
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
```

## changing Flank version

```
dependencies {
  flankExecutable("com.github.flank:flank:21.11.0")
}
```

## Orchestrator

`simple-flank` will configure it automatically if you are using `ANDROIDX_TEST_ORCHESTRATOR`


# FAQ

## Can I change the buildType to run?

Android already provides a way for you to do it:
```
android {
  ...
  testBuildType = "beta"
  ...
}
```

## How to debug shards?

`./gradlew runFlank -PdumpShards=true`

## What if I want to use a different flank config?

Feel free to [create your own task](docs/manual_tasks_creation.md) and provide the config yaml you want.
