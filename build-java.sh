#!/bin/sh

if [ "$#" -ne 2 ]; then
  echo "usage: $0 stage2_url pkg_prefix_url" >&2
  exit 1
fi

mkdir tmp
javac android/app/src/main/java/com/psychowood/henkaku/HENprocess.java -d tmp
cp loader.rop.bin host/stage1.bin

java -cp tmp com.psychowood.henkaku.HENprocess preprocess exploit.rop.bin host/stage2.bin
java -cp tmp com.psychowood.henkaku.HENprocess write_pkg_url host/stage1.bin "$1"
java -cp tmp com.psychowood.henkaku.HENprocess write_pkg_url host/stage2.bin "$2"
java -cp tmp com.psychowood.henkaku.HENprocess preprocess host/stage1.bin host/payload.js

rm -R tmp

echo "done." >&2
