@echo off
chcp 65001 > nul
if not defined JAVA_HOME (
    if exist "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot" (
        set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
    )
)
if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)
if exist "%LOCALAPPDATA%\Programs\apache-maven\apache-maven-3.9.6\bin" (
    set "PATH=%LOCALAPPDATA%\Programs\apache-maven\apache-maven-3.9.6\bin;%PATH%"
)
set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
call mvn compile exec:java
