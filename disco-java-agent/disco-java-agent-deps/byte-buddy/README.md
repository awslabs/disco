## How to upgrade?

To upgrade the version of ByteBuddy which disco-java-agent takes dependencies on,
delete the existing byte-buddy agent and dep jars.
Then execute `./gradlew upgradeByteBuddy -Pbbversion=x.x.x` for whichever
ByteBuddy release is desired (must be an official GitHub release of ByteBuddy.)

This will download, build, copy the dependency artifacts, and cleanup. From here,
update the dependencies in disco-java-agent-plugin-api and disco-java-agent-inject-api's
build.gradle.kts files to match the new ByteBuddy version's file names.