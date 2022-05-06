package io.github.flank.gradle

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

open class Device(
    @Input val id: String,
    @Input val osVersion: Int,
    @Internal val make: String? = null,
    @Internal val model: String? = null,
    @Input @Optional val locale: String? = null,
    @Input @Optional val orientation: Orientation? = null
) : Comparable<Device> {
  private val comparator =
      compareBy<Device>({ it.id }, { it.osVersion }, { it.locale }, { it.orientation })

  override fun toString(): String {
    return "$make $model - $osVersion"
  }
  enum class Orientation {
    portrait,
    landscape
  }

  interface MinSdk {
    val sdks: List<Int>
    fun deviceForMinSdk(minSdk: Int): Device
    fun selectedVersionForMinSdk(minSdk: Int): Int {
      return sdks.firstOrNull { it >= minSdk }
          ?: throw NoSuchElementException(
              "NexusLowRes doesn't support $minSdk (currently supports ${NexusLowRes.sdks})")
    }
  }

  override fun compareTo(other: Device): Int = comparator.compare(this, other)
}

class NexusLowRes(osVersion: Int, locale: String? = null, orientation: Orientation? = null) :
    Device("NexusLowRes", osVersion, "Generic", "Low-resolution MDPI phone", locale, orientation) {
  companion object : MinSdk {
    override val sdks = listOf(23, 24, 25, 26, 27, 28, 29, 30)
    override fun deviceForMinSdk(minSdk: Int) = NexusLowRes(selectedVersionForMinSdk(minSdk))
  }
}
