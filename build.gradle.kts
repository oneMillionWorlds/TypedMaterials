// --- Central Publisher API staging (no network) ---
val centralPortalStagingDir = layout.buildDirectory.dir("central-portal-staging")

// Clean the staging repo to avoid stale artifacts from previous runs
val cleanCentralPortalStaging by tasks.registering(Delete::class) {
    group = "publishing"
    description = "Deletes the local Central Portal staging directory."
    delete(centralPortalStagingDir)
}

// Aggregate task to publish all subproject publications to the local staging repo
val publishAllToCentralPortalStaging by tasks.registering {
    group = "publishing"
    description = "Publishes all module publications to the local Central Portal staging directory."
    dependsOn(
        cleanCentralPortalStaging,
        ":plugin:publishMavenJavaPublicationToCentralPortalStagingRepository"
    )
}

// Zip the staging dir into a single bundle
val zipCentralPortalBundle by tasks.registering(Zip::class) {
    group = "publishing"
    description = "Zips the Central Portal staging directory into build/central-bundle.zip"
    dependsOn(publishAllToCentralPortalStaging)
    mustRunAfter(cleanCentralPortalStaging)
    from(centralPortalStagingDir)
    archiveFileName = "central-bundle.zip"
    destinationDirectory = layout.buildDirectory
}

// Main entrypoint for CI to prepare the bundle
tasks.register("prepareCentralBundle") {
    group = "publishing"
    description = "Stages and zips artifacts ready for Central Publisher Portal upload (no network)."
    dependsOn(zipCentralPortalBundle)
}
