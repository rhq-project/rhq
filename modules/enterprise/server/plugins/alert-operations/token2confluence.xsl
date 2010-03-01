<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="text" indent="no" xml:space="default"/>

    <xsl:template match="/">
h2. Tokens for alert-operations sender
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="tokenClass">
h3. *<xsl:value-of select="@name"/>*: <xsl:value-of select="@description"/>
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="token">
* *<xsl:value-of select="@name"/>*: _<xsl:value-of select="descr"/>_
    </xsl:template>

    <xsl:template match="fullName"/>
    <xsl:template match="descr"/>

</xsl:stylesheet>