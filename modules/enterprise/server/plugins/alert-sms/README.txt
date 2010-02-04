
This SMS sender works with the OpenAPI from Deutsche Telekom's
developergarden / SendSMS service.

In order to use it - even for testing, you need to register
at www.developergarden.com

NOTE: There is currently an issue with the classloading of the alert senders,
which prevents the automatic selection of some classes from the RestEasy
package.
To work around it, you can copy the following jars to
jbossas/server/default/lib/

    <artifactItem>
         <groupId>org.jboss.resteasy</groupId>
         <artifactId>resteasy-jaxrs</artifactId>
         <version>${resteasy.version}</version>
     </artifactItem>
     <artifactItem>
         <groupId>org.jboss.resteasy</groupId>
         <artifactId>jaxrs-api</artifactId>
         <version>${resteasy.version}</version>
     </artifactItem>
     <artifactItem>
         <groupId>org.slf4j</groupId>
         <artifactId>slf4j-simple</artifactId>
         <version>1.5.6</version>
     </artifactItem>
     <artifactItem>
         <groupId>org.slf4j</groupId>
         <artifactId>slf4j-api</artifactId>
         <version>1.5.6</version>
     </artifactItem>
     