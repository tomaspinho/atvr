# Gradle wrapper

The wrapper jar/scripts are not checked in here (binary). To bootstrap the
wrapper on a machine with a local Gradle install, run from the project root:

    gradle wrapper --gradle-version 8.9

This will generate `gradle/wrapper/gradle-wrapper.jar`,
`gradlew`, and `gradlew.bat`. Then use `./gradlew` for all builds.

Alternatively, open the project in Android Studio, which will provision the
Gradle distribution declared in `gradle/wrapper/gradle-wrapper.properties`
(Gradle 8.9) automatically.