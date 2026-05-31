@ECHO OFF
SETLOCAL

REM ── StudentVault Maven Wrapper ─────────────────────────────────────────────
REM Uses the Maven downloaded by the wrapper at first run.
REM If Maven wrapper has already downloaded, use it directly.

SET "BASEDIR=%~dp0"
SET "WRAPPER_JAR=%BASEDIR%.mvn\wrapper\maven-wrapper.jar"
SET "WRAPPER_PROPS=%BASEDIR%.mvn\wrapper\maven-wrapper.properties"
SET "MAVEN_WRAPPER_DIST=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.6-bin"

REM Try to find already-downloaded Maven first
FOR /D %%G IN ("%MAVEN_WRAPPER_DIST%\*") DO (
    IF EXIST "%%G\apache-maven-3.9.6\bin\mvn.cmd" (
        "%%G\apache-maven-3.9.6\bin\mvn.cmd" %*
        EXIT /B %ERRORLEVEL%
    )
)

REM Fall back to wrapper bootstrap
IF "%JAVA_HOME%"=="" (
    SET "JAVA_EXE=java"
) ELSE (
    SET "JAVA_EXE=%JAVA_HOME%\bin\java"
)

"%JAVA_EXE%" %MAVEN_OPTS% ^
  -classpath "%WRAPPER_JAR%" ^
  "-Dmaven.multiModuleProjectDirectory=%BASEDIR%" ^
  org.apache.maven.wrapper.MavenWrapperMain %*

SET ERROR_CODE=%ERRORLEVEL%
ENDLOCAL & EXIT /B %ERROR_CODE%
