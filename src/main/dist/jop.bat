@ECHO OFF

REM
REM Startup script for jsonprocessor
REM More info: http://www.rolandfg.net/2014/06/29/json-commandline-processor/
REM

SET BD=%~dp0
SET VER=1.0
SET NAME=jsonprocessor
SET CP=%BD%\%NAME%-%VER%-all.jar

java -jar "%CP%" %*