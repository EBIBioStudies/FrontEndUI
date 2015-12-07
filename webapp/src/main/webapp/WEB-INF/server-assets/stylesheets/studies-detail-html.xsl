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

    <xsl:variable name="vIsGoogleBot" select="fn:matches($user-agent, '.*Googlebot.*')"/>
    <xsl:variable name="vAccessionNumber" select="search:getQueryInfoParameter($queryid,'accessionNumber')"/>
    <xsl:variable name="vAccession" select="if ($vAccessionNumber) then fn:upper-case($vAccessionNumber) else fn:upper-case($accession)"/>
    <xsl:variable name="vQueryString" select="if ($query-string) then fn:concat('?', $query-string) else ''"/>
    <xsl:variable name="projectLink" select="if ($project!='') then concat('/',$project) else '' "/>

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
                <link rel="stylesheet" href="{$context-path}/assets/stylesheets/bs-study-detail-1.0.150301.css" type="text/css"/>
                <link rel="stylesheet" href="{$context-path}/assets/stylesheets/jquery.dataTables.css" type="text/css"/>
            </xsl:with-param>
            <xsl:with-param name="pBreadcrumbTrail">
                <xsl:choose>
                    <xsl:when test="$keywords != ''">
                        <a href="{$context-path}{$projectLink}/studies/search.html?query={$keywords}">Studies matching "<xsl:value-of select="$keywords"/>"</a>
                    </xsl:when>
                    <xsl:otherwise>
                        <a href="{$context-path}{$projectLink}/studies/"><xsl:value-of select="if ($project='') then 'Studies' else $project "/></a>
                    </xsl:otherwise>
                </xsl:choose>
                >
                <xsl:value-of select="$vAccession"/>
                <span id="search-iterator">
                    <xsl:variable name="previousAccession" select="search:getQueryInfoParameter($queryid,'previousAccession')"/>
                    <xsl:variable name="nextAccession" select="search:getQueryInfoParameter($queryid,'nextAccession')"/>
                    <xsl:variable name="accessionIndex" select="xs:integer(search:getQueryInfoParameter($queryid,'accessionIndex'))"/>
                    <xsl:if test="$previousAccession">
                        <a href="{$context-path}{$projectLink}/studies/{$previousAccession}/{fn:replace($vQueryString,'n=\d+',concat('n=',$accessionIndex))}">
                            <span class="icon icon-functional" data-icon="&lt;"></span>Previous
                        </a>
                    </xsl:if>
                    <xsl:if test="$nextAccession">
                        <a href="{$context-path}{$projectLink}/studies/{$nextAccession}/{fn:replace($vQueryString,'n=\d+',concat('n=',$accessionIndex+2))}">
                            Next<span class="icon icon-functional" data-icon="&gt;"></span>
                        </a>
                    </xsl:if>
                </span>
            </xsl:with-param>
            <xsl:with-param name="pEBISearchWidget"/>
            <xsl:with-param name="pExtraJS">
                <script src="{$context-path}/assets/scripts/jquery.bs-studies-detail-1.0.150708.js" type="text/javascript"/>
                <script src="{$context-path}/assets/scripts/jquery.dataTables.min.js" type="text/javascript"/>
            </xsl:with-param>
            <xsl:with-param name="pExtraBodyClasses"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="bs-content-section">
        <section>
            <div id="ae-content" class="persist-area">
                <xsl:choose>
                    <xsl:when test="exists(descendant::study)">
                        <xsl:apply-templates/>
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
        <div>
            <div id="ae-detail-left-column">
                <div id="ae-detail">
                    <xsl:call-template name="study-status">
                        <xsl:with-param name="pIsGoogleBot" select="$vIsGoogleBot"/>
                    </xsl:call-template>
                    <xsl:call-template name="study-download">
                        <xsl:with-param name="pBasePath" select="$context-path"/>
                        <xsl:with-param name="pAccession" select="$vAccession"/>
                    </xsl:call-template>
                    <div class="persist-header">
                        <h4 id="ae-detail-title">
                            <xsl:call-template name="highlight">
                                <xsl:with-param name="pQueryId" select="$queryid"/>
                                <xsl:with-param name="pText" select="fn:string-join(title, ', ')"/>
                                <xsl:with-param name="pCallHighlightingFunction" select="true()"/>
                            </xsl:call-template>
                        </h4>
                    </div>
                    <xsl:call-template name="study-authors">
                        <xsl:with-param name="pQueryId" select="$queryid"/>
                        <xsl:with-param name="pTitle" select="title"/>
                        <xsl:with-param name="pNodes" select="section"/>
                    </xsl:call-template>
                    <xsl:call-template name="section">
                        <xsl:with-param name="pName" select="'Accession Number'"/>
                        <xsl:with-param name="pContent">
                            <xsl:call-template name="highlight">
                                <xsl:with-param name="pQueryId" select="$queryid"/>
                                <xsl:with-param name="pText" select="$vAccession"/>
                                <xsl:with-param name="pCallHighlightingFunction" select="true()"/>
                            </xsl:call-template>
                        </xsl:with-param>
                        <xsl:with-param name="pClass" select="('accessionNumber')"/>
                    </xsl:call-template>
                    <xsl:call-template name="study-attributes">
                        <xsl:with-param name="pQueryId" select="$queryid"/>
                        <xsl:with-param name="pNodes" select="attribute"/>
                    </xsl:call-template>
                    <xsl:call-template name="study-publications">
                        <xsl:with-param name="pQueryId" select="$queryid"/>
                        <xsl:with-param name="pNodes" select="section[fn:lower-case(@type)='publication']"/>
                    </xsl:call-template>
                    <xsl:call-template name="study-subsections">
                        <xsl:with-param name="pQueryId" select="$queryid"/>
                        <xsl:with-param name="pNodes" select="section[fn:lower-case(@type)!='funding' and fn:lower-case(@type)!='publication' and fn:lower-case(@type)!='author' and fn:lower-case(@type)!='organization']"/>
                    </xsl:call-template>
                    <xsl:call-template name="study-funding">
                        <xsl:with-param name="pQueryId" select="$queryid"/>
                        <xsl:with-param name="pNodes" select="descendant::section[fn:lower-case(@type)='funding']"/>
                    </xsl:call-template>
                </div>
            </div>
            <div id="ae-detail-right-column">
                <span class="fa fa-expand fa-icon fa-icon-large" id="right-column-expander" title="Click to expand"/>
                <xsl:choose>
                    <xsl:when test="fn:count(descendant::file)=0 and fn:count(descendant::link)=0 and search:getQueryInfoParameter($queryid,'similarAccessions')=''">
                        <xsl:value-of select="'No data'"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:call-template name="study-files">
                            <xsl:with-param name="pQueryId" select="$queryid"/>
                            <xsl:with-param name="pNodes" select="descendant::file"/>
                            <xsl:with-param name="pBasePath" select="$context-path"/>
                            <xsl:with-param name="pAccession" select="$vAccession"/>
                        </xsl:call-template>
                        <xsl:call-template name="study-links">
                            <xsl:with-param name="pQueryId" select="$queryid"/>
                            <xsl:with-param name="pLinks" select="descendant::link"/>
                            <xsl:with-param name="pBasePath" select="$context-path"/>
                        </xsl:call-template>
                        <xsl:call-template name="study-suggestion"/>
                    </xsl:otherwise>
                </xsl:choose>
            </div>
            <div class="clearboth"></div>
        </div>
    </xsl:template>

</xsl:stylesheet>
