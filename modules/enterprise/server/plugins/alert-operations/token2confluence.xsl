<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="text" indent="no" xml:space="default"/>
    <xsl:strip-space elements="*"/>

    <xsl:template match="/">
h2. Tokens for alert-operations sender

Those tokens can be used in operation definition properties like
'tokenclass.token' e.g. "alert.id" or "resource.name"
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="tokenClass">
h3. *Token Class '<xsl:value-of select="@name"/>'*  <xsl:value-of select="@description"/>
        <xsl:apply-templates >
            <xsl:sort select="@name"/>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="token">
* *<xsl:value-of select="@name"/>*: _<xsl:value-of select="descr"/>_<xsl:text/>
    </xsl:template>

    <xsl:template match="fullName"/>
    <xsl:template match="descr"/>

</xsl:stylesheet>