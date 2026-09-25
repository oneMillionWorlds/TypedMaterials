# TypedMaterials
A plugin to create java files for each j3md material file

For instructions on how to use this plugin, please refer to the [Wiki](https://github.com/oneMillionWorlds/TypedMaterials/wiki)

Upgrading from an older version? See the [migration guide](MIGRATION.md).

## Attribution and licensing

This project is licensed under the BSD-3 license, meaning it can be used in a commercial project free of charge with no need to provide attribution. 
That said if you want to mention that TypedMaterials is used in your project that would be very welcome.

# Maintainer notes

## Coding standard

Standard Java coding conventions (try to match existing style).

British English; "Centre", "Colour" etc

## Signing

To sign jars for maven central appropriate details will need to be in C:\Users\{user}\.gradle\gradle.properties. Will need

    signing.keyId=keyId
    signing.password=password
    signing.secretKeyRingFile=C:/Users/{user}/AppData/Roaming/gnupg/pubring.kbx

Note that the keyId is just the last 8 characters of the long id, and the secretRing must be explicitly exported `gpg --export-secret-keys -o secring.gpg`

If no `signing.keyId` is configured the build simply doesn't sign (so e.g. `publishToMavenLocal` works on a machine with no key).

## Publishing

The plugin is published to two places:

- The [Gradle plugin portal](https://plugins.gradle.org/plugin/com.onemillionworlds.typed-materials), which is how
  most users consume it
- Maven central (as `com.onemillionworlds:typed-materials`)

The version is held in `gradle.properties`.

### Gradle plugin portal

Run the "Release to the gradle plugins portal" GitHub action. This publishes to the plugin portal, tags the release,
and increments the version in `gradle.properties` (e.g. `2.0.0` -> `2.0.1`, or `2.0.0-alpha1` -> `2.0.0-alpha2`).

### Maven central

Project is provisioned on https://central.sonatype.com/publishing

Deploy via pipeline by:
- Running the "Stage to maven central" GitHub action. This builds a bundle (`./gradlew prepareCentralBundle`, which
  creates `build/central-bundle.zip`) and uploads it via the Central Publisher API as a `USER_MANAGED` deployment
- Go to https://central.sonatype.com/publishing and log in
- Go to Deployments and select the deployment
- If all looks well (it should reach the `VALIDATED` state) "Publish" it

The action's `OSSRH_USERNAME` / `OSSRH_PASSWORD` secrets must be a Central Portal user token (generated from your
account page on https://central.sonatype.com), not the old OSSRH credentials.

Deploy manually by:
- Running `./gradlew prepareCentralBundle` (with signing configured, see above)
- Uploading it with `.github/scripts/upload_central.sh build/central-bundle.zip` (with `CENTRAL_USERNAME` and
  `CENTRAL_PASSWORD` environment variables set to the user token), or uploading `build/central-bundle.zip` by hand
  at https://central.sonatype.com/publishing
- Then publish the deployment as above

### Testing a deployment before publishing

A `VALIDATED` deployment can be consumed straight from the Portal, so a release can be tested from a real
consuming project before it is made permanent.

Add to the consuming `build.gradle`:

    repositories {
        maven {
            name = "centralManualTesting"
            url = "https://central.sonatype.com/api/v1/publisher/deployments/download/"
            credentials(HttpHeaderCredentials)
            authentication { header(HttpHeaderAuthentication) }
        }
    }

And to that project's `gradle.properties` (or better, your user level `~/.gradle/gradle.properties`):

    centralManualTestingAuthHeaderName=Authorization
    centralManualTestingAuthHeaderValue=Bearer <base64 of tokenUsername:tokenPassword>

Where the token value is the base64 encoding of `tokenUsername:tokenPassword`, e.g.

    printf '%s:%s' "$CENTRAL_USERNAME" "$CENTRAL_PASSWORD" | base64 -w 0

Then depend on the version being released as normal. That URL serves files from any of your validated
deployments; to pin to one specific deployment use
`https://central.sonatype.com/api/v1/publisher/deployment/<deploymentId>/download/` instead.

Remember to remove this repository from the consuming project once the version is actually published.

### Testing without uploading at all

For quick local iteration skip publishing entirely and use maven local:

    ./gradlew publishToMavenLocal

Then add `mavenLocal()` to the consuming project's repositories (for a plugin, in `pluginManagement.repositories`
in `settings.gradle`). Use a distinct version number so it is obvious which artifacts are being picked up.
