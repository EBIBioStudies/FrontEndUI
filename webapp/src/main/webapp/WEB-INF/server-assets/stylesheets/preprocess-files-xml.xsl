<?xml version="1.0" encoding="UTF-8"?>
<!--
 * Copyright 2009-2015 European Molecular Biology Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:ae="http://www.ebi.ac.uk/arrayexpress/XSLT/Extension"
                extension-element-prefixes="xs fn ae"
                exclude-result-prefixes="xs fn ae"
                version="2.0">

    <xsl:output method="xml" version="1.0" encoding="UTF8" indent="yes"/>

    <xsl:param name="rootFolder"/>
    
    <xsl:variable name="vRoot" select="$rootFolder"/>
    
    <xsl:function name="ae:isFolder" as="xs:boolean">
        <xsl:param name="pRow"/>
        <xsl:value-of select="fn:starts-with($pRow/col[1], 'd')"/>
    </xsl:function>
    
    <xsl:function name="ae:getAccess" as="xs:string">
        <xsl:param name="pRow"/>
        <xsl:value-of select="fn:substring($pRow/col[1], 2)"/>
    </xsl:function>
    
    <xsl:function name="ae:getOwner" as="xs:string">
        <xsl:param name="pRow"/>
        <xsl:value-of select="$pRow/col[3]"/>
    </xsl:function>
    
    <xsl:function name="ae:getGroup" as="xs:string">
        <xsl:param name="pRow"/>
        <xsl:value-of select="$pRow/col[4]"/>
    </xsl:function>
    
    <xsl:function name="ae:getSize" as="xs:integer">
        <xsl:param name="pRow"/>
        <xsl:value-of select="$pRow/col[5]"/>
    </xsl:function>
    
    <xsl:function name="ae:getModifyDate" as="xs:dateTime">
        <xsl:param name="pRow"/>
        <xsl:value-of select="fn:concat($pRow/col[6], 'T', $pRow/col[7], ':00')"/>
    </xsl:function>
    
    <xsl:function name="ae:getName">
        <xsl:param name="pRow"/>
        <xsl:value-of select="fn:replace($pRow/col[8], '.+/([^/]+)$', '$1')"/>
    </xsl:function>
    
    <xsl:function name="ae:getExtension" as="xs:string">
        <xsl:param name="pRow"/>
        <xsl:choose>
            <xsl:when test="fn:ends-with($pRow/col[8], '.tar.gz')">
                <xsl:text>tar.gz</xsl:text>    
            </xsl:when>
            <xsl:when test="fn:not(fn:contains($pRow/col[8], '.'))">
                <xsl:text/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="fn:replace($pRow/col[8], '.+[.]([^.]+)$', '$1')"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="ae:getFolder" as="xs:string">
        <xsl:param name="pRow"/>

        <xsl:value-of select="fn:replace($pRow/col[8], fn:concat('^(', $vRoot, '/[^/]+)/.*$'), '$1')"/>
    </xsl:function>

    <xsl:function name="ae:getLocation" as="xs:string">
        <xsl:param name="pRow"/>

        <xsl:value-of select="$pRow/col[8]"/>
    </xsl:function>

    <!--
    <xsl:function name="ae:getSubLocation" as="xs:string">
        <xsl:param name="pRow"/>

        <xsl:value-of select="fn:replace($pRow/col[8], '^.+/[aAeE]-\w{4}-\d+/(.*)$', '$1')"/>
    </xsl:function>
    -->
    <xsl:function name="ae:getAccession" as="xs:string">
        <xsl:param name="pPath"/>

        <xsl:value-of select="fn:replace($pPath, '^.*/([^/]+)$', '$1')"/>
    </xsl:function>

    <xsl:function name="ae:getRelativePath" as="xs:string">
        <xsl:param name="pPath" as="xs:string"/>
        <xsl:value-of select="fn:replace($pPath, fn:concat('^', $vRoot), '')"/>
    </xsl:function>
    
    <xsl:template match="table">
        <files root="{$vRoot}">
            <xsl:attribute name="updated" select="fn:current-dateTime()"/>
            <xsl:for-each-group select="row" group-by="ae:getFolder(.)">
                <xsl:variable name="vFolder" select="fn:current-group()[col[8] = fn:current-grouping-key()]"/>
                <xsl:if test="$vFolder">
                    <xsl:variable name="vLocation" select="ae:getRelativePath(current-grouping-key())"/>
                    <xsl:variable name="vAccession" select="ae:getAccession($vLocation)"/>
                    <folder
                        location="{ae:getRelativePath(current-grouping-key())}"
                        accession="{$vAccession}"
                        owner="{ae:getOwner($vFolder)}"
                        group="{ae:getGroup($vFolder)}"
                        access="{ae:getAccess($vFolder)}"
                        lastmodified="{ae:getModifyDate($vFolder)}">
                        <xsl:for-each select="fn:current-group()[not(ae:isFolder(.))]">
                            <file
                                name="{ae:getName(.)}"
                                extension="{ae:getExtension(.)}"
                                owner="{ae:getOwner(.)}"
                                group="{ae:getGroup(.)}"
                                access="{ae:getAccess(.)}"
                                size="{ae:getSize(.)}"
                                lastmodified="{ae:getModifyDate(.)}"
                                location="{ae:getName(.)}"/>
                        </xsl:for-each>
                    </folder>
                </xsl:if>    
            </xsl:for-each-group>
        </files>    
    </xsl:template>
 </xsl:stylesheet>
