@echo off
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
set "PATH=%JAVA_HOME%\bin;%LOCALAPPDATA%\Programs\apache-maven\apache-maven-3.9.6\bin;%PATH%"
call mvn compile exec:java
