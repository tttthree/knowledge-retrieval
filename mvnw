#!/bin/sh
# Maven Wrapper for Unix/Linux/Git Bash

# Find the project base directory
PRG="$0"
while [ -h "$PRG" ]; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`/"$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`" >/dev/null
APP_HOME="`pwd`"
cd "$SAVED" >/dev/null

WRAPPER_JAR="$APP_HOME/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

# Download wrapper jar if not present
if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Maven Wrapper JAR not found. Downloading..."
    if command -v curl > /dev/null 2>&1; then
        curl -sL -o "$WRAPPER_JAR" "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
    elif command -v wget > /dev/null 2>&1; then
        wget -q -O "$WRAPPER_JAR" "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
    else
        echo "ERROR: Need curl or wget to download maven-wrapper.jar"
        exit 1
    fi
fi

# Convert to Windows path if on MSYS/MinGW
case "`uname -s`" in
    MINGW*|MSYS*|CYGWIN*)
        APP_HOME=`cygpath -w "$APP_HOME" 2>/dev/null || echo "$APP_HOME"`
        ;;
esac

# Execute Maven
exec java \
    -Dfile.encoding=UTF-8 \
    -Dmaven.multiModuleProjectDirectory="$APP_HOME" \
    -classpath "$WRAPPER_JAR" \
    $WRAPPER_LAUNCHER \
    "$@"
