package io.github.flank.gradle.tasks

import com.android.build.api.variant.BuiltArtifactsLoader
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Nested

abstract class BaseFlankApkTask : DefaultTask() {
  @get:Nested internal abstract val appApk: Property<Apk>
  fun appApk(apk: Provider<RegularFile>) = appApk.set(Apk.from(apk))
  fun appApk(apkDir: Provider<Directory>, loader: BuiltArtifactsLoader) =
      appApk.set(Apk.from(apkDir, loader))

  @get:Nested internal abstract val testApk: Property<Apk>
  fun testApk(apk: Provider<RegularFile>) = testApk.set(Apk.from(apk))
  fun testApk(apkDir: Provider<Directory>, loader: BuiltArtifactsLoader) =
      testApk.set(Apk.from(apkDir, loader))
}
