@echo off
java -Xms10M -Xmx500M -XX:+UseG1GC -server -XX:+CMSClassUnloadingEnabled -XX:+ExplicitGCInvokesConcurrent -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -XX:MaxGCPauseMillis=150 -Xverify:none -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -cp build/classes/main;build/libs/bluestone-1.0-SNAPSHOT.jar;build/libs/bluestone-1.0-SNAPSHOT-all.jar com.khronodragon.bluestone.Start