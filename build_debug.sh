#!/bin/bash

# Set up Android Studio JDK
export JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
export PATH="$JAVA_HOME/bin:$PATH"

# Download and run Gradle wrapper if needed
if [ ! -f "./gradlew" ]; then
    # Create a minimal gradlew script that invokes gradle through Java
    cat > ./gradlew << 'GRADLEW'
#!/bin/bash
exec java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain "$@"
GRADLEW
    chmod +x ./gradlew
fi

# Run the build
./gradlew assembleDebug
