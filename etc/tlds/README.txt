IntelliJ IDEA Users
-------------------
To get auto-completion working for the JSTL Core, Seam, Ajax4JSF, and RichFaces tag libraries when editing XHTML files in IntelliJ, do the following:

1) Select File > Settings > Project Settings > Resources from the menu.

2) Add the following external resources (where RHQ is the root directory of the RHQ source code): 

    URI                                      Location
   -------------------------------------------------------------
    http://java.sun.com/jstl/core            RHQ/etc/tlds/c.tld
    http://jboss.com/products/seam/taglib    RHQ/etc/tlds/s.tld
    http://richfaces.org/a4j                 RHQ/etc/tlds/a4j.tld
    https://ajax4jsf.dev.java.net/ajax       RHQ/etc/tlds/a4j.tld
    http://richfaces.org/rich                RHQ/etc/tlds/rich.tld
    http://richfaces.ajax4jsf.org/rich       RHQ/etc/tlds/rich.tld
    
3) While you're adding external resources, you may want to also add mappings for the various RHQ schemas to enable auto-completion when editing XML documents using those schemas, e.g.:

    URI                                      Location
   -------------------------------------------------------------
    urn:xmlns:rhq-configuration              RHQ/modules/core/client-api/src/main/resources/rhq-configuration.xsd
    urn:xmlns:rhq-plugin                     RHQ/modules/core/client-api/src/main/resources/rhq-plugin.xsd

Happy auto-completing!

