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
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:ae="http://www.ebi.ac.uk/arrayexpress/XSLT/Extension"
                extension-element-prefixes="ae fn"
                exclude-result-prefixes="ae fn"
                version="2.0">
    <xsl:output method="xml" encoding="UTF-8" indent="yes"/>

    <xsl:include href="bs-parse-html-function.xsl"/>

    <xsl:template match="/submissions">
        <studies total="{fn:count(submission)}"> <!--  retrieved="{ae:fixRetrievedDateTimeFormat(@retrieved)}" -->

            <xsl:apply-templates select="submission">
                <!--
                <xsl:sort select="substring-before(releasedate, '-')" order="descending" data-type="number"/>
                <xsl:sort select="substring-before(substring-after(releasedate, '-'), '-')" order="descending" data-type="number"/>
                <xsl:sort select="substring-after(substring-after(releasedate, '-'), '-')" order="descending"  data-type="number"/>
                -->
            </xsl:apply-templates>
        </studies>
    </xsl:template>

    <xsl:template match="submission/section[1]">
        <study files="{fn:count(descendant::file)}" links="{fn:count(descendant::link)}">
            <accession><xsl:value-of select="@id"/></accession>
            <releasedate>2015-02-01</releasedate>
            <xsl:apply-templates select="attributes"/>
            <!-- <xsl:apply-templates select="*" mode="copy" /> -->
        </study>
    </xsl:template>

    <xsl:template match="*" mode="copy">
        <xsl:copy>
            <xsl:if test="@*">
                <xsl:for-each select="@*">
                    <xsl:element name="{fn:lower-case(fn:name())}">
                        <xsl:value-of select="." />
                    </xsl:element>
                </xsl:for-each>
            </xsl:if>
            <xsl:apply-templates mode="copy" />
        </xsl:copy>
    </xsl:template>

    <xsl:function name="ae:fixRetrievedDateTimeFormat">
        <xsl:param name="pInvalidDateTime"/>
        <xsl:value-of select="fn:replace($pInvalidDateTime,'T(\d{1,2})[:-](\d{1,2})[:-](\d{1,2})', 'T$1:$2:$3')"/>
    </xsl:function>
    
    <xsl:template match="text( )|@*"/>

    <xsl:template match="attribute[fn:lower-case(@name)='title']">
        <title><xsl:value-of select="value"/></title>    
    </xsl:template>
    
    <xsl:template match="attribute">
        <attribute name="{@name}">
            <xsl:value-of select="value"/>
        </attribute>    
    </xsl:template>
    
    <xsl:template match="attribute[fn:lower-case(@name)='linked information']">
        <link type="{valqual[@name='type']}"><xsl:value-of select="value"/></link>    
    </xsl:template>
    
</xsl:stylesheet>