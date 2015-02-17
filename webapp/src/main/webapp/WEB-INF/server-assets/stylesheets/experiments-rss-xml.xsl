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
                xmlns:ae="http://www.ebi.ac.uk/arrayexpress/XSLT/Extension"
                xmlns:search="http://www.ebi.ac.uk/arrayexpress/XSLT/SearchExtension"
                xmlns:saxon="http://saxon.sf.net/"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                extension-element-prefixes="ae xs fn saxon search"
                exclude-result-prefixes="ae xs fn saxon search"
                version="2.0">

    <xsl:param name="page"/>
    <xsl:param name="pagesize"/>

    <xsl:variable name="vPage" select="if ($page and $page castable as xs:integer) then $page cast as xs:integer else 1" as="xs:integer"/>
    <xsl:variable name="vPageSize" select="if ($pagesize and $pagesize castable as xs:integer) then $pagesize cast as xs:integer else 25" as="xs:integer"/>
    
    <xsl:param name="sortby"/>
    <xsl:param name="sortorder"/>

    <xsl:variable name="vSortBy" select="if ($sortby) then $sortby else 'releasedate'"/>
    <xsl:variable name="vSortOrder" select="if ($sortorder) then $sortorder else 'descending'"/>

    <xsl:param name="queryid"/>

    <xsl:param name="host"/>
    <xsl:param name="context-path"/>
    <xsl:param name="querystring"/>

    <xsl:variable name="vBaseUrl"><xsl:value-of select="$host"/><xsl:value-of select="$context-path"/></xsl:variable>

    <xsl:output omit-xml-declaration="no" method="xml" encoding="UTF-8" indent="no"/>

    <xsl:include href="ae-sort-experiments.xsl"/>
    <xsl:include href="ae-date-functions.xsl"/>

    <xsl:template match="/experiments">

        <xsl:variable name="vFilteredExperiments" select="search:queryIndex($queryid)"/>
        <xsl:variable name="vTotal" as="xs:integer" select="fn:count($vFilteredExperiments)"/>

        <rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom">
            <channel>
                <xsl:variable name="vCurrentDate" select="ae:dateTimeToRfc822(fn:current-dateTime())"/>
                <title>
                    <xsl:text>ArrayExpress - Experiments</xsl:text>
                    <xsl:if test="$vPageSize &lt; $vTotal">
                        <xsl:text> (first </xsl:text>
                        <xsl:value-of select="$vPageSize"/>
                        <xsl:text> of </xsl:text>
                        <xsl:value-of select="$vTotal"/>
                        <xsl:text>)</xsl:text>
                    </xsl:if>
                </title>
                <link>
                    <xsl:value-of select="$vBaseUrl"/>
                    <xsl:text>/browse.html?</xsl:text>
                    <xsl:value-of select="$querystring"/>
               </link>
                <description><xsl:text>The ArrayExpress is a database of functional genomics experiments including gene expression where you can query and download data collected to MIAME and MINSEQE standards</xsl:text></description>
                <language><xsl:text>en</xsl:text></language>
                <pubDate><xsl:value-of select="$vCurrentDate"/></pubDate>
                <lastBuildDate><xsl:value-of select="$vCurrentDate"/></lastBuildDate>
                <docs><xsl:text>http://blogs.law.harvard.edu/tech/rss</xsl:text></docs>
                <generator><xsl:text>ArrayExpress</xsl:text></generator>
                <managingEditor><xsl:text>arrayexpress@ebi.ac.uk (ArrayExpress Team)</xsl:text></managingEditor>
                <webMaster><xsl:text>arrayexpress@ebi.ac.uk (ArrayExpress Team)</xsl:text></webMaster>
                <atom:link href="{$vBaseUrl}/rss/experiments" rel="self" type="application/rss+xml" />
                <xsl:call-template name="ae-sort-experiments">
                    <xsl:with-param name="pExperiments" select="$vFilteredExperiments"/>
                    <xsl:with-param name="pFrom" select="1"/>
                    <xsl:with-param name="pTo" select="$vPageSize"/>
                    <xsl:with-param name="pSortBy" select="$vSortBy"/>
                    <xsl:with-param name="pSortOrder" select="$vSortOrder"/>
                </xsl:call-template>

            </channel>
        </rss>

    </xsl:template>

    <xsl:template match="experiment">
        <xsl:param name="pFrom"/>
        <xsl:param name="pTo"/>
        <xsl:if test="fn:position() &gt;= xs:integer($pFrom) and fn:position() &lt;= xs:integer($pTo)">
            <item>
                <title>
                    <xsl:value-of select="accession"/>
                    <xsl:text> - </xsl:text>
                    <xsl:choose>
                        <xsl:when test="fn:string-length(name) > 0">
                            <xsl:value-of select="name"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>Untitled experiment</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </title>
                <link>
                    <xsl:value-of select="$vBaseUrl"/>
                    <xsl:text>/experiments/</xsl:text>
                    <xsl:value-of select="accession"/>
                    <xsl:text>/</xsl:text>
                </link>
                <guid>
                    <xsl:attribute name="isPermaLink">true</xsl:attribute>
                    <xsl:value-of select="$vBaseUrl"/>
                    <xsl:text>/experiments/</xsl:text>
                    <xsl:value-of select="accession"/>
                    <xsl:text>/</xsl:text>
                </guid>

                <description>
                    <xsl:for-each select="description[fn:contains(text, '(Generated description)')]">
                        <xsl:value-of select="fn:replace(text, '[(]Generated description[)]', '', 'i')"/>
                        <xsl:if test="fn:position() != fn:last()">
                            <xsl:text>&lt;br/&gt;</xsl:text>
                        </xsl:if>
                    </xsl:for-each>
                    <xsl:if test="(fn:count(description[fn:contains(text, '(Generated description)')]) > 0)">
                        <xsl:text>&lt;br/&gt;&lt;br/&gt;</xsl:text>
                    </xsl:if>
                    <xsl:for-each select="description[fn:string-length(text) > 0 and fn:not(fn:contains(text, '(Generated description)'))]">
                        <xsl:sort select="id" data-type="number"/>
                        <xsl:value-of select="fn:replace(ae:serializeXml(text, 'UTF-8'), '&lt;/?text&gt;', '', 'i')"/>
                        <xsl:if test="position() != last()">
                            <xsl:text>&lt;br/&gt;</xsl:text>
                        </xsl:if>
                    </xsl:for-each>
                </description>
                <xsl:for-each select="experimentdesign">
                    <category domain="{$vBaseUrl}">
                        <xsl:value-of select="."/>
                    </category>
                </xsl:for-each>
                <xsl:if test="releasedate > ''">
                    <pubDate>
                        <xsl:value-of select="ae:dateTimeToRfc822(fn:dateTime(releasedate, xs:time('00:00:00')))"/>
                    </pubDate>
                </xsl:if>
            </item>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
