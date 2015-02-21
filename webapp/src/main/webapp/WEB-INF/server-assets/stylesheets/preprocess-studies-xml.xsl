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
        <study files="{fn:count(descendant::file)}"
               links="{fn:count(descendant::link)}">
            <accession><xsl:value-of select="@id"/></accession>
            <releasedate>2015-02-01</releasedate>
            <xsl:apply-templates select="attributes" mode="attributes"/>
            <xsl:apply-templates select="subsections" mode="section"/>
            <xsl:apply-templates select="files" mode="files"/>
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

    <xsl:template match="text()|@*"/>
    <xsl:template match="text()|@*" mode="attributes"/>
    <xsl:template match="text()|@*" mode="attribute"/>
    <xsl:template match="text()|@*" mode="section"/>
    <xsl:template match="text()|@*" mode="files"/>
    <!--
    <xsl:template match="attributes">
        <xsl:for-each-group select="attribute" group-by="fn:lower-case(@name)">
            <attribute name="{@name}">
                <xsl:for-each select="current-group()">
                    <xsl:copy-of select="*"/>
                </xsl:for-each>
            </attribute>
        </xsl:for-each-group>
    </xsl:template>
    -->

    <xsl:template match="attribute[fn:lower-case(@name)='title']" mode="attributes">
        <title>
            <xsl:value-of select="ae:trimTrailingDot(value)"/>
        </title>
    </xsl:template>

    <!--
    <xsl:template match="attribute[fn:lower-case(@name)='linked information']" mode="attributes">
        <link>
            <xsl:attribute name="url">
                <xsl:choose>
                    <xsl:when test="valqual[@name='type']='MSD'">
                        <xsl:text>https://www.ebi.ac.uk/pdbe-srv/view/entry/</xsl:text>
                        <xsl:value-of select="value"/>
                        <xsl:text>/summary</xsl:text>
                    </xsl:when>
                </xsl:choose>
            </xsl:attribute>    
        </link>
    </xsl:template>
    -->
    <xsl:template match="attribute" mode="attributes">
        <attribute name="{@name}">
            <xsl:apply-templates mode="attribute"/>
        </attribute>    
    </xsl:template>

    <xsl:template match="valqual" mode="attribute">
        <xsl:element name="{@name}">
            <xsl:value-of select="text()"/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="value" mode="attribute">
        <value>
            <xsl:value-of select="text()"/>
        </value>
    </xsl:template>

    <xsl:template match="section" mode="section">
        <section type="{@type}" id="{@id}">
            <xsl:apply-templates select="attributes" mode="attributes"/>
            <xsl:apply-templates select="subsections" mode="section"/>
            <xsl:apply-templates select="files" mode="files"/>
        </section>
    </xsl:template>

    <xsl:template match="file" mode="files">
        <file name="{@name}">
            <xsl:apply-templates select="attributes" mode="attributes"/>
        </file>
    </xsl:template>

    <xsl:function name="ae:trimTrailingDot">
        <xsl:param name="pString" as="xs:string"/>
        <xsl:value-of select="$pString"/>
    </xsl:function>
</xsl:stylesheet>