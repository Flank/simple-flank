plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "6.4.2"
}

group = "com.github.flank.gradle"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
}

val fixtureClasspath: Configuration by configurations.creating
dependencies {
    implementation(kotlin("stdlib"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.strikt:strikt-gradle:0.31.0")
    compileOnly("com.android.tools.build:gradle:7.1.2")
    fixtureClasspath("com.android.tools.build:gradle:7.1.2")
    fixtureClasspath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.20")
}

// Inspired by: https://github.com/square/sqldelight/blob/83145b28cbdd949e98e87819299638074bd21147/sqldelight-gradle-plugin/build.gradle#L18
// Append any extra dependencies to the test fixtures via a custom configuration classpath. This
// allows us to apply additional plugins in a fixture while still leveraging dependency resolution
// and de-duplication semantics.
tasks.named("pluginUnderTestMetadata", PluginUnderTestMetadata::class.java) {
    pluginClasspath.from(fixtureClasspath)
}

tasks.withType<Test> {
    javaLauncher.set(
        javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(11)) }
    )

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
