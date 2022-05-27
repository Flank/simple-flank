# simple-flank
simple-flank is a new gradle plugin with a clear focus: make the setup as simple as possible.

Applied to any application or library module it creates a task called `flankRun` that will run all the android tests (all flavors, debug buildType by default)

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

That's it, run `./gradlew flankRun` and get the results.

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

# What happens if I don't have android tests for a project?

Maybe you are applying simple-flank from a convention plugin to all your subprojects. That approach has some issues 
because you will run tasks you don't need, but it won't fail, Flank does validate it and abort the execution and 
simple-flank just ignores that error.

AGP by default considers that all projects have androidTest enabled, but you could fix that with something like:
```kotlin
  androidComponents.beforeVariants(selector().withName("MY_NO_TESTS_VARIANT")) { variant ->
    variant.enableAndroidTest = false
  }
```

or check if the androidTest folders exist:
```kotlin
  androidComponents.beforeVariants { variant ->
    val basePath = "$projectDir/src/androidTest"
    val buildTypedPath = basePath + variant.buildType?.capitalize()
    val flavoredPath = basePath + variant.flavorName?.capitalize()
    val variantPath = flavoredPath + variant.buildType?.capitalize()
    variant.enableAndroidTest = (
      file(basePath).exists() ||
      file(buildTypedPath).exists() ||
      file(flavoredPath).exists() ||
      file(variantPath).exists()
    )
  }
```

disabling androidTest will save a lot of build time because you won't need to compile, package and run flank for 
those project.

## How to debug shards?

`./gradlew flankRun -PdumpShards=true`

## What if I want to use a different flank config?

Feel free to [create your own task](docs/manual_tasks_creation.md) and provide the config yaml you want.
