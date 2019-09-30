@echo off

start "MetaPlayer" javaw -classpath "%~dp0\bin" -Xms16m -Xmx1024m com.asofterspace.metaPlayer.Main %*
