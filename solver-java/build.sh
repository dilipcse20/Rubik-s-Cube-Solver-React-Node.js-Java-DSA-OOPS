#!/bin/sh
# Same as build.ps1 for macOS / Linux.
rm -rf out && mkdir out && javac -d out $(find src -name '*.java') && echo "Java solver compiled into the 'out' folder."
