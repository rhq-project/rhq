#!/bin/sh

for i in lib/*.jar
do
  CP=$CP:$i
done
java -cp $CP org.rhq.core.pc.plugin.PluginValidator -i $*
