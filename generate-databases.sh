#!/bin/bash
#
# Generates the data bases required for the android app (reduced column count,
# adjusted names)
#
cd database
./dicziunari.py -v -s -c "{m:wort,T:beugung,W:geschlecht,I:bereich,n:pled,L:gener,R:chomp}"
./dicziunari.py -p -s -c "{m:wort,T:beugung,W:geschlecht,I:bereich,n:pled,L:gener,R:chomp}"
cd ..
