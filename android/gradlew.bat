@ECHO OFF
SETLOCAL
SET DIR=%~dp0
SET APP_BASE_NAME=%~n0
SET APP_HOME=%DIR%
SET CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
SET JAVA_EXE=java.exe
WHERE java >NUL 2>&1
IF ERRORLEVEL 1 (
  ECHO ERROR: Java not found on PATH.
  EXIT /B 1
)
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
EXIT /B %ERRORLEVEL%
