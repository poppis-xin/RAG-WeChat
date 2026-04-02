@echo off
setlocal

set "MAVEN_BIN=C:\Users\32616\tools\apache-maven-3.9.11\bin\mvn.cmd"

if not exist "%MAVEN_BIN%" (
  echo Maven not found: %MAVEN_BIN%
  exit /b 1
)

call "%MAVEN_BIN%" spring-boot:run
