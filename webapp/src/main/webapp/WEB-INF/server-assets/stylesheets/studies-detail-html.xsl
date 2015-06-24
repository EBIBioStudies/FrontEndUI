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
                xmlns:search="http://www.ebi.ac.uk/arrayexpress/XSLT/SearchExtension"
                xmlns:html="http://www.w3.org/1999/xhtml"
                extension-element-prefixes="xs fn ae search html"
                exclude-result-prefixes="xs fn ae search html"
                version="2.0">

    <xsl:param name="queryid"/>
    <xsl:param name="accession"/>
    <xsl:param name="user-agent"/>

    <xsl:variable name="vAccession" select="fn:upper-case($accession)"/>
    <xsl:variable name="vIsGoogleBot" select="fn:matches($user-agent, '.*Googlebot.*')"/>

    <xsl:include href="bs-html-page.xsl"/>
    <xsl:include href="bs-studies-templates.xsl"/>

    <xsl:template match="/">
        <xsl:call-template name="bs-page">
            <xsl:with-param name="pIsSearchVisible" select="fn:true()"/>
            <xsl:with-param name="pExtraSearchFields"/>
            <xsl:with-param name="pTitleTrail">
                <xsl:value-of select="$vAccession"/>
                <xsl:text> &lt; Studies</xsl:text>
                <xsl:if test="$keywords != ''">
                    <xsl:text> matching "</xsl:text><xsl:value-of select="$keywords"/><xsl:text>"</xsl:text>
                </xsl:if>
            </xsl:with-param>
            <xsl:with-param name="pExtraCSS">
                <link rel="stylesheet" href="{$context-path}/assets/stylesheets/bs-study-detail-1.0.150301.css"
                      type="text/css"/>
            </xsl:with-param>
            <xsl:with-param name="pBreadcrumbTrail">
                <xsl:choose>
                    <xsl:when test="$keywords != ''">
                        <a href="{$context-path}/studies/search.html?query={$keywords}">Studies matching "<xsl:value-of select="$keywords"/>"</a>
                    </xsl:when>
                    <xsl:otherwise>
                        <a href="{$context-path}/studies/">Studies</a>
                    </xsl:otherwise>
                </xsl:choose>
                >
                <xsl:value-of select="$vAccession"/>
            </xsl:with-param>
            <xsl:with-param name="pEBISearchWidget"/>
            <xsl:with-param name="pExtraJS"/>
            <xsl:with-param name="pExtraBodyClasses"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="bs-content-section">
        <xsl:variable name="vStudy" select="search:queryIndex($queryid)[accession = $vAccession]"/>
        <section>
            <div id="ae-content" class="persist-area">
                <xsl:choose>
                    <xsl:when test="exists($vStudy)">
                        <xsl:call-template name="block-study">
                            <xsl:with-param name="pStudy" select="$vStudy"/>
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="ae:httpStatus(404)"/>
                    </xsl:otherwise>
                </xsl:choose>
            </div>
        </section>
    </xsl:template>

    <xsl:template name="block-study">
        <xsl:param name="pStudy"/>
        <xsl:apply-templates select="$pStudy"/>
    </xsl:template>

    <xsl:template match="study">
        <xsl:variable name="vFiles" select="ae:getMappedValue('accession-folder', $vAccession)"/>
        <xsl:variable name="vQueryString" select="if ($query-string) then fn:concat('?', $query-string) else ''"/>
        <xsl:call-template name="study-status">
            <xsl:with-param name="pIsGoogleBot" select="$vIsGoogleBot"/>
            <xsl:with-param name="pIsPrivate" select="fn:false()"/>
        </xsl:call-template>
        <div id="ae-detail">
            <div id="ae-detail-left-column">
                <div class="persist-header">
                    <h4 id="ae-detail-title">
                        <xsl:call-template name="highlight">
                            <xsl:with-param name="pQueryId" select="$queryid"/>
                            <xsl:with-param name="pText" select="fn:string-join(title, ', ')"/>
                            <xsl:with-param name="pFieldName"/>
                        </xsl:call-template>
                    </h4>
                </div>
                <xsl:call-template name="study-authors">
                    <xsl:with-param name="pQueryId" select="$queryid"/>
                    <xsl:with-param name="pTitle" select="title"/>
                    <xsl:with-param name="pNodes" select="section"/>
                </xsl:call-template>
                <xsl:call-template name="study-attributes">
                    <xsl:with-param name="pQueryId" select="$queryid"/>
                    <xsl:with-param name="pNodes" select="attribute"/>
                </xsl:call-template>
                <xsl:call-template name="study-publications">
                    <xsl:with-param name="pQueryId" select="$queryid"/>
                    <xsl:with-param name="pTitle" select="title"/>
                    <xsl:with-param name="pNodes" select="section"/>
                </xsl:call-template>
                <xsl:call-template name="section">
                    <xsl:with-param name="pName" select="'Accession Number'"/>
                    <xsl:with-param name="pContent"><xsl:value-of select="$vAccession"/></xsl:with-param>
                </xsl:call-template>
            </div>
            <div id="ae-detail-right-column">
                <xsl:choose>
                    <xsl:when test="fn:count(descendant::file)=0 and fn:count(descendant::link)=0">
                        <xsl:value-of select="'No data'"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:call-template name="study-files">
                            <xsl:with-param name="pQueryId" select="$queryid"/>
                            <xsl:with-param name="pNodes" select="descendant::file"/>
                            <xsl:with-param name="pFiles" select="$vFiles"/>
                            <xsl:with-param name="pBasePath" select="$context-path"/>
                        </xsl:call-template>
                        <xsl:call-template name="study-links">
                            <xsl:with-param name="pQueryId" select="$queryid"/>
                            <xsl:with-param name="pNodes" select="descendant::link"/>
                        </xsl:call-template>
                    </xsl:otherwise>
                </xsl:choose>
            </div>
            <div class="clearboth"></div>
        </div>
    </xsl:template>

</xsl:stylesheet>
