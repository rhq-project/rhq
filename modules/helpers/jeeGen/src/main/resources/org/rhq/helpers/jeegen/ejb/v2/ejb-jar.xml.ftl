<?xml version="1.0" encoding="UTF-8"?>

<ejb-jar id="ejb-jar_ID" version="2.1"
 xmlns="http://java.sun.com/xml/ns/j2ee"
 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd">

   <enterprise-beans>

      <!-- Stateless Session Beans -->

<#list 1..statelessSessionBeanCount as index>
      <session>
         <ejb-name>${name}StatelessSessionBean${index?c}</ejb-name>
         <home>${package}.StatelessSessionEJBHome</home>
         <remote>${package}.StatelessSessionEJBObject</remote>
         <ejb-class>${package}.StatelessSessionBean</ejb-class>
         <session-type>Stateless</session-type>
         <transaction-type>Container</transaction-type>
      </session>
</#list>

      <!-- Stateful Session Beans -->

<#list 1..statefulSessionBeanCount as index>
      <session>
         <ejb-name>${name}StatefulSessionBean${index?c}</ejb-name>
         <home>${package}.StatefulSessionEJBHome</home>
         <remote>${package}.StatefulSessionEJBObject</remote>
         <ejb-class>${package}.StatefulSessionBean</ejb-class>
         <session-type>Stateful</session-type>
         <transaction-type>Container</transaction-type>
      </session>
</#list>

   </enterprise-beans>
</ejb-jar>
