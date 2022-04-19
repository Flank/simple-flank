import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.BuiltArtifactsLoader
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import java.lang.Integer.max
import java.lang.Integer.min

val flankExecutable: Configuration by configurations.creating

dependencies {
    flankExecutable("com.github.flank:flank") {
        version {
            prefer("22.04.0")
        }
    }
}

val simpleFlankExtension = extensions.create<SimpleFlankExtension>("simpleFlank")

plugins.withType(AppPlugin::class.java) {
    val appExtension = requireNotNull(extensions.findByType<ApplicationAndroidComponentsExtension>())
    appExtension.onVariants {
        val debugApkDir: Provider<Directory> = it.artifacts.get(SingleArtifact.APK)
        val testApkDir: Provider<Directory>? = it.androidTest?.artifacts?.get(SingleArtifact.APK)

        if (testApkDir != null) {
            val builtArtifactsLoader = it.artifacts.getBuiltArtifactsLoader()
            val apkProvider: Provider<File> = debugApkDir.map { apk ->
                file { builtArtifactsLoader.load(apk)?.elements?.single()?.outputFile }
            }
            registerFlankRun(it, testApkDir, builtArtifactsLoader).configure {
                appApk.fileProvider(
                    apkProvider
                )
            }
        }
    }
    tasks.register("flankRun") {
        dependsOn(tasks.withType<FlankExecutionTask>())
    }
    verifyNotDefaultKeystore()
}

plugins.withType(LibraryPlugin::class.java) {
    useFixedKeystore()
    val libraryExtension = requireNotNull(extensions.findByType<LibraryAndroidComponentsExtension>())
    libraryExtension.onVariants {
        val testApkDir: Provider<Directory>? = it.androidTest?.artifacts?.get(SingleArtifact.APK)

        if (testApkDir != null) {
            val copySmallApp: Copy = getSmallAppTask()
            val builtArtifactsLoader = it.artifacts.getBuiltArtifactsLoader()
            registerFlankRun(it, testApkDir, builtArtifactsLoader).configure {
                dependsOn(copySmallApp)
                appApk.value {
                    files(copySmallApp).asFileTree.matching { include("*.apk") }.singleFile
                }
            }
        }
    }
    tasks.register("flankRun") {
        dependsOn(tasks.withType<FlankExecutionTask>())
    }
}

fun registerFlankRun(
    variant: Variant,
    testApkDir: Provider<Directory>,
    builtArtifactsLoader: BuiltArtifactsLoader
) =
    tasks.register<FlankExecutionTask>("flankRun${variant.name.capitalize()}") {
        flankJarClasspath.from(flankExecutable)

        serviceAccountCredentials.set(simpleFlankExtension.credentialsFile)
        projectId.set(simpleFlankExtension.projectId)
        flankProject.set(getFlankProject())
        this@register.variant.set(variant.name)
        hermeticTests.set(simpleFlankExtension.hermetic)

        val minSdk = variant.minSdkVersion.apiLevel
        val selectedSdk = min(max(NexusLowRes.minOsVersion(), minSdk), NexusLowRes.maxOsVersion())
        device.set(NexusLowRes(selectedSdk))
        testApk.fileProvider(
            testApkDir.map { apk ->
                file { builtArtifactsLoader.load(apk)?.elements?.single()?.outputFile }
            }
        )
        val dumpShards: String by project
        this@register.dumpShards.set(dumpShards.toBoolean())
    }
