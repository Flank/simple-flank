import java.io.File

fun File.writeYaml(
    projectId: String,
    flankProject: String,
    variant: String,
    device: AvailableVirtualDevice,
    appApk: File,
    testApk: File,
    useOrchestrator: Boolean,
) {
  writeText(
      """
      gcloud:
        app: $appApk
        test: $testApk
        device:
        - model: "${device.id}"
          version: "${device.osVersion}"

        use-orchestrator: $useOrchestrator
        auto-google-login: false
        record-video: false
        performance-metrics: false
        timeout: 15m
        results-history-name: $flankProject.$variant
        num-flaky-test-attempts: 0

      flank:
        max-test-shards: 40
        shard-time: 120
        smart-flank-gcs-path: gs://$projectId/$flankProject.$variant/JUnitReport.xml
        keep-file-path: false
        ignore-failed-tests: false
        disable-sharding: false
        smart-flank-disable-upload: false
        legacy-junit-result: false
        full-junit-result: false
        output-style: single
        default-test-time: 1.0
        use-average-test-time-for-new-tests: true
    """.trimIndent())
}
