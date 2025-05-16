rem set PR_PATH=%CD%
rem SET PR_SERVICE_NAME=QueryNotificationScheduler
rem SET PR_JAR=QueryNotificationScheduler.jar

set ARG=%1%
set SERVICE_NAME=QueryNotificationScheduler
set BASE_DIR=%CD%
set PR_INSTALL=%BASE_DIR%\commons_daemon\prunsrv.exe

REM Service log configuration
set PR_LOGPREFIX=%SERVICE_NAME%
set PR_LOGPATH=%BASE_DIR%
set PR_STDOUTPUT=%BASE_DIR%stdout.txt
set PR_STDERROR=%BASE_DIR%stderr.txt
rem set PR_LOGLEVEL=Error
set PR_LOGLEVEL=Info

REM Path to java installation
set PR_JVM=auto
set PR_CLASSPATH=%BASE_DIR%\%SERVICE_NAME%.jar

REM Startup configuration
set PR_STARTUP=auto
rem set PR_STARTIMAGE=c:\Program Files\Java\jre7\bin\java.exe 
set PR_STARTIMAGE=%JAVA_HOME%\bin\java.exe 
set PR_STARTMODE=exe
set PR_STARTPARAMS=-jar#%PR_CLASSPATH%

REM Shutdown configuration
set PR_STOPMODE=java
set PR_STOPCLASS=us.akcourts.querynotification.Scheduler
set PR_STOPMETHOD=stop

REM JVM configuration
set PR_JVMMS=64
set PR_JVMMX=256

REM Install service
rem %PR_INSTALL% //IS//%SERVICE_NAME%
rem "%PR_INSTALL%" //IS//%SERVICE_NAME% %1%
rem problem below?
"%PR_INSTALL%" %ARG% %SERVICE_NAME%
