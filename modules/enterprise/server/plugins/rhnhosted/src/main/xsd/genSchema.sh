#Script relies on: apache project xmlbeans
#http://xmlbeans.apache.org/sourceAndBinaries/index.html

/opt/xmlbeans-2.4.0/bin/inst2xsd -design vb -simple-content-types string -enumerations never ./sample/*.xml 
