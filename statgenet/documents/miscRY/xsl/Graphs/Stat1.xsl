<?xml version="1.0" encoding="Shift_jis"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:template match="/">
  <html>
    <body>
      <xsl:apply-templates/>
    </body>
  </html>
</xsl:template>
<xsl:template match="Stat1">
  <xsl:apply-templates select="title"/>
  <xsl:apply-templates select="tbl"/>
</xsl:template>
<xsl:template match="title">
  <h3><xsl:value-of select="."/></h3>
</xsl:template>
<xsl:template match="tbl">
  <table border="3">
    <tr>
      <th>Iteration</th>
      <th>No.Node</th>

      <th>No.Root</th>
      <th>No.Mut</th>
      <th>No.Rec</th>
      <th>No.Edge</th>
      <th>No.Event</th>
    </tr>
    <xsl:variable name="t" select="document(@href)"/>
    <xsl:for-each select="$t//Graph">
      <tr>
        <td><xsl:value-of select="id"/></td>
        <td><xsl:value-of select="TotalNode"/></td>
        <td><xsl:value-of select="RootNode"/></td>
        <td><xsl:value-of select="MutNode"/></td>
        <td><xsl:value-of select="RecNode"/></td>

        <td><xsl:value-of select="Edge"/></td>
        <td><xsl:value-of select="RetroEvent"/></td>
      </tr>
    </xsl:for-each>
  </table>
</xsl:template>
</xsl:stylesheet>