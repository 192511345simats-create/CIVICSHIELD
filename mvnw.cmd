@echo off
setlocal
set MAVEN_PROJECTBASEDIR=%~dp0
set MAVEN_WRAPPER_URL=https://repo.maven.apache.org/maven2/io/takari/maven-wrapper/0.5.6/maven-wrapper-0.5.6.jar
set MAVEN_WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar
if not exist "%MAVEN_WRAPPER_JAR%" (
    echo Downloading Maven Wrapper...
    powershell -Command "(New-Object System.Net.WebClient).DownloadFile('%MAVEN_WRAPPER_URL%','%MAVEN_WRAPPER_JAR%')"
)
"%JAVA_HOME%\bin\java" -jar "%MAVEN_WRAPPER_JAR%" %*