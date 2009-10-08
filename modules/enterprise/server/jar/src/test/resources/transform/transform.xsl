<xsl:stylesheet version="1.0"  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
 xmlns:str="http://exslt.org/strings"
 extension-element-prefixes="str" >

	<xsl:output
	 method="xml"
	 indent="yes"
	 encoding="UTF-8" 
	/>
	
	<!-- if a parameter is not passed in, then it will be set to the 
	   value of the variable UNCHANGED -->
	<xsl:variable name="UNCHANGED" select="'~UNCHANGED~'"/>
	
	<xsl:param name="jndiName" select="$UNCHANGED"/>
	<xsl:param name="userName" select="$UNCHANGED"/>
	<xsl:param name="connectionProperty" select="$UNCHANGED"/>
	<xsl:param name="XADataSourceProperty" select="$UNCHANGED"/>
	<xsl:param name="connectionUrl" select="$UNCHANGED"/>
	<xsl:param name="minPoolSize" select="$UNCHANGED"/>
	<xsl:param name="maxPoolSize" select="$UNCHANGED"/>
	<xsl:param name="transactionIsolation" select="$UNCHANGED"/>
	<xsl:param name="validConnectionChecker" select="$UNCHANGED"/>
	<xsl:param name="useJavaContext" select="$UNCHANGED"/>
	<xsl:param name="domainAndApplicationSecurity" select="$UNCHANGED"/>
	<xsl:param name="applicationManagedSecurity" select="$UNCHANGED"/>
	<xsl:param name="securityDomain" select="$UNCHANGED"/>
    <xsl:param name="newConnectionSQL" select="$UNCHANGED"/>
    <xsl:param name="exceptionSorter" select="$UNCHANGED"/>
    <xsl:param name="password" select="$UNCHANGED"/>
    <xsl:param name="trackStatements" select="$UNCHANGED"/>
    <xsl:param name="checkValidConnectionSQL" select="$UNCHANGED"/>
	<xsl:param name="driverClass" select="$UNCHANGED"/>
	<xsl:param name="XADataSourceClass" select="$UNCHANGED"/>
	<xsl:param name="blockingTimeout" select="$UNCHANGED"/>
	<xsl:param name="preparedStatementCacheSize" select="$UNCHANGED"/>
	<xsl:param name="idleTimeout" select="$UNCHANGED"/>
	<xsl:param name="noTxSeparatePools" select="$UNCHANGED"/>
	<xsl:param name="isSameRMOverrideValue" select="$UNCHANGED"/>
	<xsl:param name="trackConnectionByTx" select="$UNCHANGED"/>
	
	<!-- supply defaults for testing -->
	<xsl:param name="datasourceType">test-type</xsl:param>
	<xsl:param name="nameValueSeparator">||</xsl:param>
	<xsl:param name="elementSeparator">|^|</xsl:param>
	
	<!-- TODO support multiple datasources inside a file -->
	
	<xsl:template match="/datasources">
	    <datasources>	    
	      	<xsl:apply-templates />
	    </datasources>
	</xsl:template>


	<xsl:template match="local-tx-datasource|xa-datasource|no-tx-datasource|dummy">
		<xsl:element name="{$datasourceType}">

		<!-- These should be ordered as specified in the DTD -->
		<xsl:call-template name="addElementIfSpecifiedAndNotPresent">
			<xsl:with-param name="element" select="jndi-name"/>
			<xsl:with-param name="elementName" select="'jndi-name'"/>
			<xsl:with-param name="parameterValue" select="$jndiName"/>
		</xsl:call-template>
			
		<xsl:call-template name="addElementIfSpecifiedAndNotPresent">
			<xsl:with-param name="element" select="connection-url"/>
			<xsl:with-param name="elementName" select="'connection-url'"/>
			<xsl:with-param name="parameterValue" select="$connectionUrl"/>
		</xsl:call-template>
			
		<xsl:call-template name="addElementIfSpecifiedAndNotPresent">
			<xsl:with-param name="element" select="min-pool-size"/>
			<xsl:with-param name="elementName" select="'min-pool-size'"/>
			<xsl:with-param name="parameterValue" select="$minPoolSize"/>
		</xsl:call-template>			

		<xsl:call-template name="addElementIfSpecifiedAndNotPresent">
			<xsl:with-param name="element" select="max-pool-size"/>
			<xsl:with-param name="elementName" select="'max-pool-size'"/>
			<xsl:with-param name="parameterValue" select="$maxPoolSize"/>
		</xsl:call-template>	
		
		<xsl:call-template name="addElementIfSpecifiedAndNotPresent">
			<xsl:with-param name="element" select="user-name"/>
			<xsl:with-param name="elementName" select="'user-name'"/>
			<xsl:with-param name="parameterValue" select="$userName"/>
		</xsl:call-template>				

		<xsl:call-template name="addElementIfSpecifiedAndNotPresent">
			<xsl:with-param name="element" select="transaction-isolation"/>
			<xsl:with-param name="elementName" select="'transaction-isolation'"/>
			<xsl:with-param name="parameterValue" select="$transactionIsolation"/>
		</xsl:call-template>				

		<xsl:call-template name="addElementIfSpecifiedAndNotPresent">
			<xsl:with-param name="element" select="valid-connection-checker-class-name"/>
			<xsl:with-param name="elementName" select="'valid-connection-checker-class-name'"/>
			<xsl:with-param name="parameterValue" select="$validConnectionChecker"/>
		</xsl:call-template>				

		<xsl:call-template name="addElementIfSpecifiedAndNotPresent">
			<xsl:with-param name="element" select="use-java-context"/>
			<xsl:with-param name="elementName" select="'use-java-context'"/>
			<xsl:with-param name="parameterValue" select="$useJavaContext"/>
		</xsl:call-template>	
		
		<xsl:call-template name="addElementIfSpecifiedAndNotPresent">
			<xsl:with-param name="element" select="security-domain-and-application"/>
			<xsl:with-param name="elementName" select="'security-domain-and-application'"/>
			<xsl:with-param name="parameterValue" select="$domainAndApplicationSecurity"/>
		</xsl:call-template>	

		<xsl:call-template name="addEmptyElementIfSpecifiedTrueAndNotPresent">
			<xsl:with-param name="element" select="application-managed-security"/>
			<xsl:with-param name="elementName" select="'application-managed-security'"/>
			<xsl:with-param name="parameterValue" select="$applicationManagedSecurity"/>
		</xsl:call-template>	
		
		<xsl:call-template name="addElementIfSpecifiedAndNotPresent">
			<xsl:with-param name="element" select="security-domain"/>
			<xsl:with-param name="elementName" select="'security-domain'"/>
			<xsl:with-param name="parameterValue" select="$securityDomain"/>
		</xsl:call-template>	

		<xsl:call-template name="addElementIfSpecifiedAndNotPresent">
			<xsl:with-param name="element" select="new-connection-sql"/>
			<xsl:with-param name="elementName" select="'new-connection-sql'"/>
			<xsl:with-param name="parameterValue" select="$newConnectionSQL"/>
		</xsl:call-template>	
				
		<xsl:call-template name="addElementIfSpecifiedAndNotPresent">
			<xsl:with-param name="element" select="exception-sorter-class-name"/>
			<xsl:with-param name="elementName" select="'exception-sorter-class-name'"/>
			<xsl:with-param name="parameterValue" select="$exceptionSorter"/>
		</xsl:call-template>	
		
	    <xsl:call-template name="addElementIfSpecifiedAndNotPresent">
			<xsl:with-param name="element" select="password"/>
			<xsl:with-param name="elementName" select="'password'"/>
			<xsl:with-param name="parameterValue" select="$password"/>
		</xsl:call-template>	
		
		<xsl:call-template name="addElementIfSpecifiedAndNotPresent">
			<xsl:with-param name="element" select="track-statements"/>
			<xsl:with-param name="elementName" select="'track-statements'"/>
			<xsl:with-param name="parameterValue" select="$trackStatements"/>
		</xsl:call-template>
		
		<xsl:call-template name="addElementIfSpecifiedAndNotPresent">
			<xsl:with-param name="element" select="check-valid-connection-sql"/>
			<xsl:with-param name="elementName" select="'check-valid-connection-sql'"/>
			<xsl:with-param name="parameterValue" select="$checkValidConnectionSQL"/>
		</xsl:call-template>
	
		<xsl:call-template name="addElementIfSpecifiedAndNotPresent">
			<xsl:with-param name="element" select="driver-class"/>
			<xsl:with-param name="elementName" select="'driver-class'"/>
			<xsl:with-param name="parameterValue" select="$driverClass"/>
		</xsl:call-template>

		<xsl:call-template name="addElementIfSpecifiedAndNotPresent">
			<xsl:with-param name="element" select="xa-datasource-class"/>
			<xsl:with-param name="elementName" select="'xa-datasource-class'"/>
			<xsl:with-param name="parameterValue" select="$XADataSourceClass"/>
		</xsl:call-template>

		<xsl:call-template name="addElementIfSpecifiedAndNotPresent">
			<xsl:with-param name="element" select="blocking-timeout-millis"/>
			<xsl:with-param name="elementName" select="'blocking-timeout-millis'"/>
			<xsl:with-param name="parameterValue" select="$blockingTimeout"/>
		</xsl:call-template>
		
		<xsl:call-template name="addElementIfSpecifiedAndNotPresent">
			<xsl:with-param name="element" select="prepared-statement-cache-size"/>
			<xsl:with-param name="elementName" select="'prepared-statement-cache-size'"/>
			<xsl:with-param name="parameterValue" select="$preparedStatementCacheSize"/>
		</xsl:call-template>

		<xsl:call-template name="addElementIfSpecifiedAndNotPresent">
			<xsl:with-param name="element" select="idle-timeout-minutes"/>
			<xsl:with-param name="elementName" select="'idle-timeout-minutes'"/>
			<xsl:with-param name="parameterValue" select="$idleTimeout"/>
		</xsl:call-template>		
		
		<xsl:call-template name="addElementIfSpecifiedAndNotPresent">
			<xsl:with-param name="element" select="no-tx-separate-pools"/>
			<xsl:with-param name="elementName" select="'no-tx-separate-pools'"/>
			<xsl:with-param name="parameterValue" select="$noTxSeparatePools"/>
		</xsl:call-template>		
		
		<xsl:call-template name="addElementIfSpecifiedAndNotPresent">
			<xsl:with-param name="element" select="isSameRM-override-value"/>
			<xsl:with-param name="elementName" select="'isSameRM-override-value'"/>
			<xsl:with-param name="parameterValue" select="$isSameRMOverrideValue"/>
		</xsl:call-template>		
	
		<xsl:call-template name="addElementIfSpecifiedAndNotPresent">
			<xsl:with-param name="element" select="track-connection-by-tx"/>
			<xsl:with-param name="elementName" select="'track-connection-by-tx'"/>
			<xsl:with-param name="parameterValue" select="$trackConnectionByTx"/>
		</xsl:call-template>		
		
		<!-- don't attempt to add elements if this property hasn't changed -->
		<xsl:if test="$connectionProperty != $UNCHANGED">	
			<xsl:variable name="currentConnectionProperties" select="connection-property" />
			<xsl:for-each select="str:split($connectionProperty, $elementSeparator)">
				<xsl:variable name="newNameAttribute" select="substring-before(., $nameValueSeparator)" />
				<!-- if there isn't an existing connection-property element with the
					 specified name attribute then go ahead and add one 
				-->
		      	<xsl:if test="not($currentConnectionProperties[@name = $newNameAttribute])">
			      	<xsl:call-template name="outputPropertyTemplate">
			      		<xsl:with-param name="nameAttribute" select="$newNameAttribute"/>
			      		<xsl:with-param name="elementName" select="'connection-property'"/>
   		      			<xsl:with-param name="propertyParameter" select="$connectionProperty"/>
			      	</xsl:call-template>
				</xsl:if>	   	
    		</xsl:for-each>
		</xsl:if>   
		
		<!-- don't attempt to add elements if this property hasn't changed -->
		<xsl:if test="$XADataSourceProperty != $UNCHANGED">	
		    <xsl:variable name="currentXADataSourceProperties" select="xa-datasource-property" />
			<xsl:for-each select="str:split($XADataSourceProperty, $elementSeparator)">
				<xsl:variable name="newNameAttribute" select="substring-before(., $nameValueSeparator)" />
				<!-- if there isn't an existing xa-datasource-property element with the
					 specified name attribute then go ahead and add one 
				-->
		      	<xsl:if test="not($currentXADataSourceProperties[@name = $newNameAttribute])">
		      	    <xsl:call-template name="outputPropertyTemplate">
				      	<xsl:with-param name="nameAttribute" select="$newNameAttribute"/>
				    	<xsl:with-param name="elementName" select="'xa-datasource-property'"/>
						<xsl:with-param name="propertyParameter" select="$XADataSourceProperty"/>
				    </xsl:call-template>
				</xsl:if>	      	
	    	</xsl:for-each>
		</xsl:if>
		
		<xsl:apply-templates />

		</xsl:element>
	</xsl:template>


	<xsl:template match="jndi-name">
		<xsl:call-template name="addElementIfSpecified">
				<xsl:with-param name="parameterValue" select="$jndiName"/>
		</xsl:call-template>
    </xsl:template>

	<xsl:template match="user-name">
		<xsl:call-template name="addElementIfSpecified">
			<xsl:with-param name="parameterValue" select="$userName"/>
		</xsl:call-template>
    </xsl:template>

	<xsl:template match="connection-url">
		<xsl:call-template name="addElementIfSpecified">
			<xsl:with-param name="parameterValue" select="$connectionUrl"/>
		</xsl:call-template>
    </xsl:template>    

	<xsl:template match="min-pool-size">
		<xsl:call-template name="addElementIfSpecified">
			<xsl:with-param name="parameterValue" select="$minPoolSize"/>
		</xsl:call-template>
    </xsl:template>    

	<xsl:template match="max-pool-size">
		<xsl:call-template name="addElementIfSpecified">
			<xsl:with-param name="parameterValue" select="$maxPoolSize"/>
		</xsl:call-template>
    </xsl:template>  

	<xsl:template match="transaction-isolation">
		<xsl:call-template name="addElementIfSpecified">
			<xsl:with-param name="parameterValue" select="$transactionIsolation"/>
		</xsl:call-template>
    </xsl:template>    

	<xsl:template match="valid-connection-checker-class-name">
		<xsl:call-template name="addElementIfSpecified">
			<xsl:with-param name="parameterValue" select="$validConnectionChecker"/>
		</xsl:call-template>
    </xsl:template>    
     
	<xsl:template match="use-java-context">
		<xsl:call-template name="addElementIfSpecified">
			<xsl:with-param name="parameterValue" select="$useJavaContext"/>
		</xsl:call-template>
    </xsl:template>    
  
    <xsl:template match="security-domain-and-application">
		<xsl:call-template name="addElementIfSpecified">
			<xsl:with-param name="parameterValue" select="$domainAndApplicationSecurity"/>
		</xsl:call-template>
    </xsl:template> 

    <xsl:template match="application-managed-security">
		<xsl:call-template name="addElementIfSpecified">
			<xsl:with-param name="parameterValue" select="$applicationManagedSecurity"/>
		</xsl:call-template>
    </xsl:template> 
   
    <xsl:template match="security-domain">
		<xsl:call-template name="addElementIfSpecified">
			<xsl:with-param name="parameterValue" select="$securityDomain"/>
		</xsl:call-template>
    </xsl:template> 
   
    <xsl:template match="new-connection-sql">
		<xsl:call-template name="addElementIfSpecified">
			<xsl:with-param name="parameterValue" select="$newConnectionSQL"/>
		</xsl:call-template>
    </xsl:template> 
    
    <xsl:template match="exception-sorter-class-name">
		<xsl:call-template name="addElementIfSpecified">
			<xsl:with-param name="parameterValue" select="$exceptionSorter"/>
		</xsl:call-template>
    </xsl:template> 

	<xsl:template match="password">
		<xsl:call-template name="addElementIfSpecified">
			<xsl:with-param name="parameterValue" select="$password"/>
		</xsl:call-template>
    </xsl:template> 
    
    <xsl:template match="track-statements">
		<xsl:call-template name="addElementIfSpecified">
			<xsl:with-param name="parameterValue" select="$trackStatements"/>
		</xsl:call-template>
    </xsl:template> 
    
    <xsl:template match="check-valid-connection-sql">
		<xsl:call-template name="addElementIfSpecified">
			<xsl:with-param name="parameterValue" select="$checkValidConnectionSQL"/>
		</xsl:call-template>
    </xsl:template> 
    
    <xsl:template match="driver-class">
		<xsl:call-template name="addElementIfSpecified">
			<xsl:with-param name="parameterValue" select="$driverClass"/>
		</xsl:call-template>
    </xsl:template> 
    
    <xsl:template match="xa-datasource-class">
		<xsl:call-template name="addElementIfSpecified">
			<xsl:with-param name="parameterValue" select="$XADataSourceClass"/>
		</xsl:call-template>
    </xsl:template> 
    
    <xsl:template match="blocking-timeout-millis">
		<xsl:call-template name="addElementIfSpecified">
			<xsl:with-param name="parameterValue" select="$blockingTimeout"/>
		</xsl:call-template>
    </xsl:template> 

    <xsl:template match="prepared-statement-cache-size">
		<xsl:call-template name="addElementIfSpecified">
			<xsl:with-param name="parameterValue" select="$preparedStatementCacheSize"/>
		</xsl:call-template>
    </xsl:template> 

    <xsl:template match="idle-timeout-minutes">
		<xsl:call-template name="addElementIfSpecified">
			<xsl:with-param name="parameterValue" select="$idleTimeout"/>
		</xsl:call-template>
    </xsl:template> 
    
    <xsl:template match="no-tx-separate-pools">
		<xsl:call-template name="addElementIfSpecified">
			<xsl:with-param name="parameterValue" select="$noTxSeparatePools"/>
		</xsl:call-template>
    </xsl:template> 

    <xsl:template match="isSameRM-override-value">
		<xsl:call-template name="addElementIfSpecified">
			<xsl:with-param name="parameterValue" select="$isSameRMOverrideValue"/>
		</xsl:call-template>
    </xsl:template> 

    <xsl:template match="track-connection-by-tx">
		<xsl:call-template name="addElementIfSpecified">
			<xsl:with-param name="parameterValue" select="$trackConnectionByTx"/>
		</xsl:call-template>
    </xsl:template> 

	<xsl:template match="connection-property">
        <xsl:call-template name="updatePropertyMapTemplate">
           	<xsl:with-param name="propertyParameter" select="$connectionProperty"/>
           	<xsl:with-param name="elementName" select="'connection-property'"/>
	    </xsl:call-template>     
	</xsl:template>

	<xsl:template match="xa-datasource-property">
        <xsl:call-template name="updatePropertyMapTemplate">
           	<xsl:with-param name="propertyParameter" select="$XADataSourceProperty"/>
           	<xsl:with-param name="elementName" select="'xa-datasource-property'"/>
	    </xsl:call-template>     	
	</xsl:template>
	

	<!-- OUTPUT TEMPLATES -->

    <!-- if this property is one we want to set, via the specified parameter,
    	 then update it, i.e. output the new element, otherwise delete it, i.e. don't output anything.
    	 If the parameter is unchanged then just output the original property unchanged
    -->
	<xsl:template name="updatePropertyMapTemplate">
		<xsl:param name="propertyParameter"/>
		<xsl:param name="elementName"/>
		<xsl:choose>
          <xsl:when test="$propertyParameter = $UNCHANGED">
            <xsl:copy>
	    		<xsl:apply-templates select="@*|node()"/>
	  		</xsl:copy>
          </xsl:when>
          <xsl:when test="contains($propertyParameter, concat(@name,$nameValueSeparator))">
            <xsl:call-template name="outputPropertyTemplate">
		      	<xsl:with-param name="nameAttribute" select="@name"/>
		    	<xsl:with-param name="elementName" select="$elementName"/>
				<xsl:with-param name="propertyParameter" select="$propertyParameter"/>
		    </xsl:call-template>
          </xsl:when>
          <!-- otherwise don't output anything -->
       </xsl:choose>
	 </xsl:template>
	
    <!-- add a new property element -->
	<xsl:template name="outputPropertyTemplate">
		<xsl:param name="nameAttribute"/>
		<xsl:param name="elementName"/>
		<xsl:param name="propertyParameter"/>
		<xsl:element name="{$elementName}">
			<xsl:attribute name="name">
				<xsl:value-of select="$nameAttribute" />
			</xsl:attribute>
			<xsl:value-of select="substring-before(
				substring-after($propertyParameter, concat($nameAttribute, $nameValueSeparator)),
				$elementSeparator)"/>
		</xsl:element>
	 </xsl:template>	


	<!-- if the specified 'element' is not present and the 'parameterValue' isn't
	  empty then output an element called 'elementName' with the value of 
	  'parameterValue' -->
	<xsl:template name="addElementIfSpecifiedAndNotPresent">
		<xsl:param name="element"/>
		<xsl:param name="elementName"/>
		<xsl:param name="parameterValue"/>
		<xsl:if test="not($element) and $parameterValue and $parameterValue != $UNCHANGED">
			<xsl:call-template name="outputElement">
				<xsl:with-param name="elementName" select="$elementName"/>
				<xsl:with-param name="elementValue" select="$parameterValue"/>
			</xsl:call-template>
    	</xsl:if>
    </xsl:template>
    
    <!-- this template exists to support generating empty elements.
     The parameterValue must be set to 'true' for the corresponding
     empty element to be created
    -->
    <xsl:template name="addEmptyElementIfSpecifiedTrueAndNotPresent">
		<xsl:param name="element"/>
		<xsl:param name="elementName"/>
		<xsl:param name="parameterValue"/>
		<xsl:if test="not($element) and $parameterValue = 'true'">
			<xsl:call-template name="outputElement">
				<xsl:with-param name="elementName" select="$elementName"/>
			</xsl:call-template>
    	</xsl:if>
    </xsl:template>
	
	<!-- if the parameter passed in hasn't been changed then output
	 the original elements, else if the parameter isn't empty then output
	 an element using it as the value -->
	<xsl:template name="addElementIfSpecified">
		<xsl:param name="parameterValue"/>
		<xsl:choose>
          <xsl:when test="$parameterValue = $UNCHANGED">
            <xsl:copy>
	    		<xsl:apply-templates select="@*|node()"/>
	  		</xsl:copy>
          </xsl:when>
          <xsl:when test="$parameterValue">
            <xsl:call-template name="outputElement">
				<xsl:with-param name="elementValue" select="$parameterValue"/>
			</xsl:call-template>
          </xsl:when>
          <!-- otherwise don't output anything -->
        </xsl:choose>
    </xsl:template>

	<!-- output an element with the specified name and value -->
	<xsl:template name="outputElement">
		<xsl:param name="elementName" select="name()"/>
		<xsl:param name="elementValue"/>
		<xsl:element name="{$elementName}">
       		<xsl:value-of select="$elementValue"/>
       	</xsl:element>
	</xsl:template>

	
	<!-- let everything we don't care about go through -->
	<xsl:template match="@*|node()" >
	  <xsl:copy>
	    <xsl:apply-templates select="@*|node()"/>
	  </xsl:copy>
	</xsl:template>
	
</xsl:stylesheet>