plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "6.4.2"
    id("com.gradle.plugin-publish") version "1.0.0-rc-1"
    `maven-publish`
}

group = "io.github.flank.gradle"
version = "0.3.0"

repositories {
    google()
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

val fixtureClasspath: Configuration by configurations.creating
dependencies {
    implementation(kotlin("stdlib"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.strikt:strikt-gradle:0.31.0")
    compileOnly("com.android.tools.build:gradle:8.0.1")
    fixtureClasspath("com.android.tools.build:gradle:8.0.1")
    fixtureClasspath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.20")
}

tasks.named("pluginUnderTestMetadata", PluginUnderTestMetadata::class.java) {
    pluginClasspath.setFrom(
        // Inspired by: https://discuss.gradle.org/t/how-to-make-gradle-testkit-depend-on-output-jar-rather-than-just-classes/18940
        // We need the jar because Gradle by default uses just classes instead of the output jar.
        // By not using the jar, the test environment is different from the production environment.
        // In the production environment, resources (keystore and small apk) are inside the jar archive that we need to extract.
        // In the test environment, resources are just files in the classpath, no need to extract the jar because there isn't one.
        tasks.named("jar", Jar::class.java).map { it.archiveFile },
        // Inspired by: https://github.com/square/sqldelight/blob/83145b28cbdd949e98e87819299638074bd21147/sqldelight-gradle-plugin/build.gradle#L18
        // Append any extra dependencies to the test fixtures via a custom configuration classpath. This
        // allows us to apply additional plugins in a fixture while still leveraging dependency resolution
        // and de-duplication semantics.
        fixtureClasspath
    )

}

tasks.withType<Test> {
    if (!environment.containsKey("ANDROID_HOME")) {
        environment("ANDROID_HOME", "${environment["HOME"]}/Library/Android/sdk")
    }
}

spotless {
    kotlin {
        ktfmt()
        targetExclude(
            "**/build/**",
            "**/.gradle/**",
        )
    }
}

gradlePlugin {
    plugins {
        named("io.github.flank.gradle.simple-flank") {
            displayName = "simple-flank plugin: a 0 configuration Gradle plugin for Flank"
        }
    }
}
pluginBundle {
    website = "https://github.com/Flank/simple-flank"
    vcsUrl = "https://github.com/Flank/simple-flank"

    description = "simple-flank is a new gradle plugin with a clear focus: make the setup as simple as possible."
    tags = listOf("flank", "testing", "android", "firebase", "test-lab", "instrumentation", "espresso", "firebase-test-lab")
}
