@echo off
chcp 65001 > nul
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
set "PATH=%JAVA_HOME%\bin;%LOCALAPPDATA%\Programs\apache-maven\apache-maven-3.9.6\bin;%PATH%"
set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
call mvn compile exec:java
