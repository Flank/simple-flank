package io.github.flank.gradle

import org.gradle.api.tasks.Input

sealed class AvailableVirtualDevice(
    @Input val id: String,
    @Input val make: String,
    @Input val model: String,
    @Input val osVersion: Int
) {
  override fun toString(): String {
    return "$make $model - $osVersion"
  }
}

class NexusLowRes(osVersion: Int) :
    AvailableVirtualDevice("NexusLowRes", "Generic", "Low-resolution MDPI phone", osVersion) {
  companion object {
    fun deviceForMinSdk(minSdk: Int) = NexusLowRes(selectedVersionForMinSdk(minSdk))
    private fun selectedVersionForMinSdk(minSdk: Int): Int {
      val selectedSdk = Integer.max(minOsVersion, minSdk)
      if (selectedSdk > maxOsVersion) {
        throw RuntimeException(
            "NexusLowRes doesn't support $minSdk yet, max sdk version is $maxOsVersion")
      }
      return selectedSdk
    }
    val minOsVersion = 23
    val maxOsVersion = 30
    fun api23() = NexusLowRes(23)
    fun api24() = NexusLowRes(24)
    fun api25() = NexusLowRes(25)
    fun api26() = NexusLowRes(26)
    fun api27() = NexusLowRes(27)
    fun api28() = NexusLowRes(28)
    fun api29() = NexusLowRes(29)
    fun api30() = NexusLowRes(30)
  }
}

class Nexus5(osVersion: Int) : AvailableVirtualDevice("Nexus5", "LG", "Nexus 5", osVersion) {
  companion object {
    fun api19() = Nexus5(19)
    fun api21() = Nexus5(21)
    fun api22() = Nexus5(22)
    fun api23() = Nexus5(23)
  }
}

class Nexus5X(osVersion: Int) : AvailableVirtualDevice("Nexus5X", "LG", "Nexus 5X", osVersion) {
  companion object {
    fun api23() = Nexus5X(23)
    fun api24() = Nexus5X(24)
    fun api25() = Nexus5X(25)
    fun api26() = Nexus5X(26)
  }
}

class Nexus7(osVersion: Int) :
    AvailableVirtualDevice("Nexus7", "Asus", "Nexus 7 (2012)", osVersion) {
  companion object {
    fun api19() = Nexus7(19)
    fun api21() = Nexus7(21)
    fun api22() = Nexus7(22)
  }
}

class Nexus7Clone169(osVersion: Int) :
    AvailableVirtualDevice(
        "Nexus7_clone_16_9", "Generic", "Nexus7 clone, DVD 16:9 aspect ratio ", osVersion) {
  companion object {
    fun api23() = Nexus7Clone169(23)
    fun api24() = Nexus7Clone169(24)
    fun api25() = Nexus7Clone169(25)
    fun api26() = Nexus7Clone169(26)
  }
}

class Nexus9(osVersion: Int) : AvailableVirtualDevice("Nexus9", "HTC", "Nexus 9", osVersion) {
  companion object {
    fun api21() = Nexus9(21)
    fun api22() = Nexus9(22)
    fun api23() = Nexus9(23)
    fun api24() = Nexus9(24)
    fun api25() = Nexus9(25)
  }
}
