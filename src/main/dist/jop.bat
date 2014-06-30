@ECHO OFF

REM
REM Startup script for jsonprocessor
REM More info: http://www.rolandfg.net/2014/06/29/json-commandline-processor/
REM

SET BD=%~dp0
SET VER=0.6
SET NAME=jop
SET CP=%BD%\%NAME%-%VER%-all.jar

java -jar "%CP%" %*