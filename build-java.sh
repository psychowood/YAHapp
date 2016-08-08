#!/bin/sh

if [ "$#" -ne 2 ]; then
  echo "usage: $0 stage2_url pkg_prefix_url" >&2
  exit 1
fi


javac HENprocess.java 
cp loader.rop.bin host/stage1.bin

python preprocess.py exploit.rop.bin host/stage2.bin
#java HENprocess preprocess exploit.rop.bin host/stage2.bin

#python write_pkg_url.py host/stage1.bin "$1"
java HENprocess write_pkg_url host/stage1.bin "$1"

#python write_pkg_url.py host/stage2.bin "$2"
java HENprocess write_pkg_url host/stage2.bin "$2"

python preprocess.py host/stage1.bin host/payload.js
#java HENprocess preprocess host/stage1.bin host/payload.js

rm HENprocess.class
echo "done." >&2
