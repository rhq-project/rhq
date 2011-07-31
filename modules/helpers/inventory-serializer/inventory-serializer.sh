#!/bin/sh

java -cp 'target/dependency/*':target/inventory-serializer-4.1.0-SNAPSHOT.jar org.rhq.helpers.inventoryserializer.Main "$@"

