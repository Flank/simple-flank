# Creating test tasks manually

    tasks.register<FlankRunTask>("myFlankRunTask") {
      // `flankExecutable` is the configuration cotaining flank dependency
      flankJarClasspath.from(flankExecutable)

      // Set the credentials file
      serviceAccountCredentials.set(...)

      // Any string you want to use, usually the variant we are testing
      // It will be used in the path to store the results in your build: "build/flank/$variant"
      variant.set(...)

      // The flank yaml file, it can be the output of a task or a file you created manually for example.
      // It's important that you use relative paths to your apks if you wan't it to be cacheable
      flankYaml.set(...)

      // Do you want your task to be cacheable? defaults to false
      hermeticTests.set(true)

      // Set any Provider<RegularFile> that will provide the APKs
      // Must be the same you used for the yaml
      appApk(...)
      testApk(...)
    }
