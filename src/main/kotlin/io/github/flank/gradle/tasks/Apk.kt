package io.github.flank.gradle.tasks

import com.android.build.api.variant.BuiltArtifactsLoader
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles

interface Apk {
  fun singleFile(): RegularFile

  companion object {
    fun from(apk: Provider<RegularFile>) = FileApk(apk)
    fun from(apkDir: Provider<Directory>, loader: BuiltArtifactsLoader) = AndroidApk(apkDir, loader)
  }

  class FileApk(@get:InputFile @get:Classpath val provider: Provider<RegularFile>) : Apk {
    override fun singleFile(): RegularFile = provider.get()
  }

  class AndroidApk(
      @get:InputFiles @get:Classpath val provider: Provider<Directory>,
      private val loader: BuiltArtifactsLoader
  ) : Apk {
    override fun singleFile(): RegularFile =
        provider.map { it.file(loader.load(it)!!.elements.single().outputFile) }.get()
  }
}
