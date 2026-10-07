@echo off
rem Baixa o Maven automaticamente (uma vez) e roda o comando. Uso:  run.cmd spring-boot:run -Dspring-boot.run.profiles=groq
setlocal
set MAVEN_VERSION=3.9.9
set LOCAL_DIR=%~dp0.mvn-local
set MAVEN_DIR=%LOCAL_DIR%\apache-maven-%MAVEN_VERSION%

where java >nul 2>nul
if errorlevel 1 (
  echo [ERRO] Java nao encontrado. Instale o JDK 21: winget install EclipseAdoptium.Temurin.21.JDK  e abra um terminal novo.
  exit /b 1
)

if not exist "%MAVEN_DIR%\bin\mvn.cmd" (
  echo Baixando Maven %MAVEN_VERSION% ... so na primeira vez.
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; New-Item -ItemType Directory -Force '%LOCAL_DIR%' | Out-Null; Invoke-WebRequest 'https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile '%LOCAL_DIR%\maven.zip'; Expand-Archive '%LOCAL_DIR%\maven.zip' -DestinationPath '%LOCAL_DIR%' -Force"
  if errorlevel 1 (
    echo [ERRO] Nao consegui baixar o Maven. Verifique a internet.
    exit /b 1
  )
)

call "%MAVEN_DIR%\bin\mvn.cmd" %*
