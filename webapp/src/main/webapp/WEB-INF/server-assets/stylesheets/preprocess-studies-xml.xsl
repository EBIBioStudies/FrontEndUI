<?xml version="1.0" encoding="UTF-8"?>
<!--
 * Copyright 2009-2016 European Molecular Biology Laboratory
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
                exclude-result-prefixes="ae xs fn"
                version="2.0">
    <xsl:output method="xml" encoding="UTF-8" indent="yes"/>

    <xsl:template match="/pmdocument/submissions">
        <studies total="{fn:count(submission)}"> <!--  retrieved="{ae:fixRetrievedDateTimeFormat(@retrieved)}" -->

            <xsl:apply-templates select="submission">
                <!-- remains here until when we get release info in the source
                <xsl:sort select="substring-before(releasedate, '-')" order="descending" data-type="number"/>
                <xsl:sort select="substring-before(substring-after(releasedate, '-'), '-')" order="descending" data-type="number"/>
                <xsl:sort select="substring-after(substring-after(releasedate, '-'), '-')" order="descending"  data-type="number"/>
                -->
            </xsl:apply-templates>
        </studies>
    </xsl:template>

    <xsl:template match="submission">
        <xsl:variable name="vAccess" select="fn:replace(fn:replace(@access,';',' '),'~','')"/>
        <xsl:variable name="vNow"  select="ae:now()"/>
        <study files="{fn:count(.//file)}"
               links="{fn:count(.//link)}"
               relPath="{@relPath}"
               type="{lower-case(if (section[1]/@type='') then 'study' else section[1]/@type)}"
               releaseTime="{if (exists(@rtime)) then @rtime else if (fn:contains(lower-case($vAccess),'public')) then ae:now() else 9999999999}"> <!-- Keeping a future date for unreleased submissions -->
            <xsl:if test="fn:exists(@ctime)">
                <xsl:attribute name="creationTime"><xsl:value-of select="@ctime"/></xsl:attribute>
            </xsl:if>
            <xsl:apply-templates/>
        </study>
    </xsl:template>

    <xsl:template match="submission/section[1]">
        <xsl:variable name="vAccession" select="../@acc"/>
        <accession><xsl:value-of select="$vAccession"/></accession>
        <access><xsl:value-of select="fn:replace(fn:replace(../@access,';',' '),'~','')"/></access>
        <project><xsl:value-of select="../attributes/attribute[fn:lower-case(name)='attachto']/value"/></project>
        <releasedate><xsl:value-of select="../attributes/attribute[fn:lower-case(name)='releasedate']/value"/></releasedate>
        <title>
            <xsl:choose>
                <xsl:when test="fn:exists(attributes/attribute[fn:lower-case(normalize-space(name))='title'])">
                    <xsl:value-of select="fn:replace(attributes/attribute[fn:lower-case(normalize-space(name))='title']/value, '[.]\s*$', '')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="../attributes/attribute[fn:lower-case(normalize-space(name))='title']/value"/>
                </xsl:otherwise>
            </xsl:choose>
        </title>
        <xsl:for-each select="subsections/section[fn:lower-case(@type)='author']">
            <!--xsl:if test="fn:position() = 1 or fn:position() = fn:last()" -->
            <author index="{fn:position()}">
                <xsl:value-of select="attributes/attribute[fn:lower-case(name)='name']/value"/>
            </author>
            <!--/xsl:if-->
        </xsl:for-each>
        <xsl:apply-templates select="attributes" mode="attributes"/>
        <xsl:apply-templates select="section | subsections/section" mode="section"/>
        <xsl:apply-templates select=".//files[not(ancestor::subsections)]" mode="files"/>
        <xsl:apply-templates select=".//links[not(ancestor::subsections)]" mode="links"/>
    </xsl:template>

    <xsl:template match="submission/section[position()>1]">
        <xsl:apply-templates select="." mode="section"/>
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

    <xsl:template match="text()|@*"/>
    <xsl:template match="text()|@*" mode="attributes"/>
    <xsl:template match="text()|@*" mode="attribute"/>
    <xsl:template match="text()|@*" mode="section"/>
    <xsl:template match="text()|@*" mode="files"/>

    <xsl:template match="attribute" mode="attributes">
        <attribute name="{name}">
            <xsl:apply-templates mode="attribute"/>
        </attribute>
    </xsl:template>

    <xsl:template match="valqual" mode="attribute">
        <xsl:element name="{string-join(for $s in tokenize(name,'\W+') return concat(upper-case(substring($s,1,1)),substring($s,2)),'')}">
            <xsl:value-of select="value"/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="url" mode="attribute">
        <url>
            <xsl:value-of select="text()"/>
            <xsl:value-of select="text()"/>
        </url>
    </xsl:template>

    <xsl:template match="value" mode="attribute">
        <value>
            <xsl:value-of select="text()"/>
        </value>
    </xsl:template>

    <xsl:template match="section" mode="section">
        <section type="{@type}" id="{@id}">
            <xsl:if test="fn:exists(@acc)">
                <xsl:attribute name="acc" select="@acc"/>
            </xsl:if>
            <xsl:if test="fn:lower-case(@type)='publication' and fn:lower-case(fn:substring(@acc,1,3))='pmc'">
                <attribute name="PMCID">
                    <value><xsl:value-of select="fn:upper-case(@acc)"/></value>
                </attribute>
            </xsl:if>
            <xsl:if test="fn:lower-case(@type)='publication'
                and matches(@acc,'^10.\d{4,9}/.+$')">
                <attribute name="DOI">
                    <value><xsl:value-of select="@acc"/></value>
                </attribute>
            </xsl:if>
            <xsl:apply-templates select="attributes" mode="attributes"/>
            <xsl:copy-of select="./*[not(name()='section' or name()='subsections' or name()='file' or name()='files' or name()='link' or name()='links' or  name()='attribute' or name()='attributes')]"/>
            <xsl:apply-templates select="section | subsections/section" mode="section"/>
            <xsl:apply-templates select="file|files/table/file|files/file" mode="files"/>
            <xsl:apply-templates select="link|links/table/link|links/link" mode="links"/>
            <xsl:copy-of select="subsections/table"  />
        </section>
    </xsl:template>


    <xsl:template match="file" mode="files">
        <file name="{fn:replace(path, '.+/([^/]+)$', '$1')}" path="{path}" size="{@size}">
            <xsl:apply-templates select="attributes" mode="attributes"/>
        </file>
    </xsl:template>

    <xsl:template match="link" mode="links">
        <link url="{url}">
            <xsl:apply-templates select="attributes" mode="attributes"/>
        </link>
    </xsl:template>

    <xsl:function name="ae:fixRetrievedDateTimeFormat">
        <xsl:param name="pInvalidDateTime"/>
        <xsl:value-of select="fn:replace($pInvalidDateTime,'T(\d{1,2})[:-](\d{1,2})[:-](\d{1,2})', 'T$1:$2:$3')"/>
    </xsl:function>

    <xsl:function name="ae:now">
        <xsl:variable name="epoch" select="xs:dateTime('1970-01-01T00:00:00Z')"/>
        <xsl:variable name="diff"  select="current-dateTime() - $epoch"/>
        <xsl:sequence select="format-number(round(number($diff div xs:dayTimeDuration('PT1S'))),'############')"/>
    </xsl:function>

</xsl:stylesheet>