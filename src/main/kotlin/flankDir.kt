import org.gradle.api.file.Directory
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Provider

internal val ProjectLayout.flankDir: Provider<Directory>
  get() = buildDirectory.dir("flank")
