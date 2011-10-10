#!/bin/sh

# Set the DEV_CONTAINER to your dev container location
DEV_CONTAINER=/im/dev-container


cd $DEV_CONTAINER/jbossas/server/default/lib
cp ~/.m2/repository/org/jboss/resteasy/resteasy-jaxrs/2.2.1.GA/resteasy-jaxrs-2.2.1.GA.jar .
cp ~/.m2/repository/org/jboss/resteasy/resteasy-links/2.2.0.GA/resteasy-links-2.2.0.GA.jar .
cp ~/.m2/repository/org/jboss/resteasy/resteasy-jettison-provider/2.2.0.GA/resteasy-jettison-provider-2.2.0.GA.jar .
cp ~/.m2/repository/org/jboss/resteasy/resteasy-jaxb-provider/2.2.1.GA/resteasy-jaxb-provider-2.2.1.GA.jar .
cp ~/.m2/repository/org/scannotation/scannotation/1.0.3/scannotation-1.0.3.jar .
cp ~/.m2/repository/net/jcip/jcip-annotations/1.0/jcip-annotations-1.0.jar .
cp ~/.m2/repository/org/codehaus/jettison/1.2/jessison-1.2.jar .
cp ~/.m2/repository/org/codehaus/jettison/jettison/1.2/jettison-1.2.jar .
cp ~/.m2/repository/org/jboss/el/jboss-el/2.0.1.GA/jboss-el-2.0.1.GA.jar .
cp ~/.m2/repository/org/freemarker/freemarker/2.3.18/freemarker-2.3.18.jar .
