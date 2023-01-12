task("upgradeByteBuddy") {
    doLast {
        exec {
            if (!project.hasProperty("bbversion")) {
                throw InvalidUserDataException("Missing argument, supply ByteBuddy version e.g. \"./gradlew upgradeByteBuddy -Pbbversion=1.12.6\"")
            }

            commandLine("python3", "upgradeByteBuddy.py", project.properties["bbversion"].toString())
        }
    }
}