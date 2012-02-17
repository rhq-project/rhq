<?xml version="1.0" encoding="UTF-8"?>

${rootElement}

   <enterprise-beans>

      <!-- Entity Beans -->

<#list 1..entityBeanCount as index>
      <entity>
          <description>a very simple entity bean</description>
          <display-name>Simple Entity Bean</display-name>

          <ejb-name>${name}EntityBean${index?c}</ejb-name>
          <home>${package}.EntityEJBHome</home>
          <remote>${package}.EntityEJBObject</remote>
          <ejb-class>${package}.EntityBean</ejb-class>
          <persistence-type>Container</persistence-type>

          <prim-key-class>java.lang.Integer</prim-key-class>
          <reentrant>false</reentrant>
          <cmp-version>2.x</cmp-version>
          <abstract-schema-name>Entity${index?c}</abstract-schema-name>
          <cmp-field><field-name>key</field-name></cmp-field>
          <cmp-field><field-name>name</field-name></cmp-field>
          <primkey-field>key</primkey-field>

          <query>
            <query-method>
              <method-name>findAll</method-name>
              <method-params/>
            </query-method>
            <ejb-ql>Select OBJECT(e) From Entity${index?c} e</ejb-ql>
          </query>
          <query>
            <query-method>
              <method-name>findByName</method-name>
              <method-params>
                <method-param>java.lang.String</method-param>
              </method-params>
            </query-method>
            <ejb-ql>Select OBJECT(e) From Entity${index?c} e where e.name = ?1</ejb-ql>
          </query>
      </entity>
</#list>

      <!-- Stateless Session Beans -->

<#list 1..statelessSessionBeanCount as index>
      <session>
         <description>a very simple stateless session bean</description>
         <display-name>Simple Stateless Session Bean</display-name>

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
         <description>a very simple stateful session bean</description>
         <display-name>Simple Stateful Session Bean</display-name>

         <ejb-name>${name}StatefulSessionBean${index?c}</ejb-name>
         <home>${package}.StatefulSessionEJBHome</home>
         <remote>${package}.StatefulSessionEJBObject</remote>
         <ejb-class>${package}.StatefulSessionBean</ejb-class>
         <session-type>Stateful</session-type>
         <transaction-type>Container</transaction-type>
      </session>
</#list>

<!-- Stateful Session Beans -->

<#list 1..messageDrivenBeanCount as index>
      <message-driven>
         <description>a very simple message-driven bean</description>
         <display-name>Simple Message-Driven Bean</display-name>

         <ejb-name>${name}MessageDrivenBean${index?c}</ejb-name>
         <ejb-class>${package}.MessageDrivenBean</ejb-class>
         <transaction-type>Container</transaction-type>

${mdbElements}
<!--
         <resource-ref>
            <description>description</description>
            <res-ref-name>jms/myQueueConnectionFactory</res-ref-name>
            <res-type>javax.jms.QueueConnectionFactory</res-type>
            <res-auth>Application</res-auth>
            <res-sharing-scope>Shareable</res-sharing-scope>
         </resource-ref>
-->
<!--
         <resource-env-ref>
            <resource-env-ref-name>jms/persistentQueue</resource-env-ref-name>
            <resource-env-ref-type>javax.jms.Queue</resource-env-ref-type>
         </resource-env-ref>
-->
      </message-driven>
</#list>

   </enterprise-beans>
</ejb-jar>
