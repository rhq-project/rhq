#!/bin/sh

if [ ! -d target ]
then
    mkdir target
fi

javac -sourcepath src/main/java -d target src/main/java/org/rhq/devUtil/zipCheck/*
java -cp target org.rhq.devUtil.zipCheck.ZipCheck $*