#!/bin/bash
M2_REPO=~/.m2/repository
java -classpath $M2_REPO/net/augeas/augeas/0.0.1/augeas-0.0.1.jar:$M2_REPO//com/sun/jna/jna/3.0.9/jna-3.0.9.jar:$M2_REPO/org/augeas/augtree/1.0/augtree-1.0.jar AugeasTree $*
