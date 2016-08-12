#!/bin/sh

tmpdir=`mktemp -d`

echo ''
echo 'Starting HENkaku server, CTRC+c to stop when you are done'
echo ''
javac android/app/src/main/java/com/psychowood/henkaku/HENprocess.java  android/app/src/main/java/com/psychowood/henkaku/HenkakuWebServer.java android/app/src/main/java/fi/iki/elonen/NanoHTTPD.java -d $tmpdir
java -cp $tmpdir:android/app/src/main/resources com.psychowood.henkaku.HenkakuWebServer android/app/src/main/assets/henkaku/ $1
echo ''
rm -R $tmpdir
echo "done" >&2
