@REM ----------------------------------------------------------------------------
@REM Maven Wrapper for Windows
@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.
@REM ----------------------------------------------------------------------------

@if "%DEBUG%"=="" @echo off
@REM Find the project base directory
set "MAVEN_PROJECTBASEDIR=%~dp0"

set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"
set "WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain"

set "MAVEN_OPTS=%MAVEN_OPTS% -Dfile.encoding=UTF-8"

if not exist "%WRAPPER_JAR%" (
    echo Maven Wrapper JAR not found. Downloading...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar' -OutFile '%WRAPPER_JAR%'" 2>nul
    if not exist "%WRAPPER_JAR%" (
        echo ERROR: Could not download maven-wrapper.jar automatically.
        echo Please install Maven manually: https://maven.apache.org/download.cgi
        exit /b 1
    )
)

@REM Execute Maven
"%JAVA_HOME%\bin\java.exe" %MAVEN_OPTS% -classpath "%WRAPPER_JAR%" %WRAPPER_LAUNCHER% %MAVEN_CONFIG% %*
