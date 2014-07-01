@ECHO OFF
REM
REM Startup script for jop, a JSON processor
REM
REM More info: http://www.rolandfg.net/2014/06/29/json-commandline-processor/
REM

SET NAME=jop
SET VER=0.6

SET BD=%~dp0
SET CP=%BD%\%NAME%-%VER%-all.jar

java -jar "%CP%" %*