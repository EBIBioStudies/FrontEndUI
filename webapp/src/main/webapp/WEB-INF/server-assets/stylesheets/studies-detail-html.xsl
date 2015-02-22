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
            <xsl:with-param name="pSearchInputValue"/>
            <xsl:with-param name="pExtraSearchFields"/>
            <xsl:with-param name="pTitleTrail">
                <xsl:value-of select="$vAccession"/>
                <xsl:text> &lt; Studies</xsl:text>
            </xsl:with-param>
            <xsl:with-param name="pExtraCSS">
                <link rel="stylesheet" href="{$context-path}/assets/stylesheets/bs-study-detail-1.0.150220.css"
                      type="text/css"/>
            </xsl:with-param>
            <xsl:with-param name="pBreadcrumbTrail">
                <a href="{$context-path}/studies/">Studies</a>
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
            <div id="ae-content">
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
        <h4>
            <!--
            <xsl:if test="not($pStudy/user/@id = '1')">
                <xsl:attribute name="class" select="'icon icon-functional'"/>
                <xsl:attribute name="data-icon" select="'L'"/>
            </xsl:if>
            -->
            <xsl:value-of select="$pStudy/accession"/>
            <xsl:text> - </xsl:text>
            <xsl:call-template name="highlight">
                <xsl:with-param name="pQueryId" select="$queryid"/>
                <xsl:with-param name="pText" select="fn:string-join($pStudy/title, ', ')"/>
                <xsl:with-param name="pFieldName"/>
            </xsl:call-template>
        </h4>
        <xsl:apply-templates select="$pStudy"/>
    </xsl:template>

    <xsl:template match="study">
        <!--
        <xsl:variable name="vFiles" select="ae:getMappedValue('ftp-folder', $vAccession)"/>
        -->
        <xsl:variable name="vQueryString" select="if ($query-string) then fn:concat('?', $query-string) else ''"/>

        <div id="ae-detail">
            <table cellpadding="0" cellspacing="0" border="0">
                <xsl:call-template name="study-status-section">
                    <xsl:with-param name="pIsGoogleBot" select="$vIsGoogleBot"/>
                    <xsl:with-param name="pIsPrivate" select="fn:false()"/>
                </xsl:call-template>
                <xsl:call-template name="study-attributes-section">
                    <xsl:with-param name="pQueryId" select="$queryid"/>
                    <xsl:with-param name="pAttributes" select="attribute"/>
                </xsl:call-template>
                <!--
                <xsl:call-template name="exp-organism-section">
                    <xsl:with-param name="pQueryId" select="$queryid"/>
                </xsl:call-template>

                <xsl:call-template name="exp-samples-section">
                    <xsl:with-param name="pQueryString" select="$vQueryString"/>
                    <xsl:with-param name="pQueryId" select="$queryid"/>
                    <xsl:with-param name="pBasePath" select="$context-path"/>
                    <xsl:with-param name="pFiles" select="$vFiles"/>
                </xsl:call-template>

                <xsl:call-template name="exp-arrays-section">
                    <xsl:with-param name="pQueryId" select="$queryid"/>
                    <xsl:with-param name="pBasePath" select="$context-path"/>
                    <xsl:with-param name="pAccession" select="$vAccession"/>
                </xsl:call-template>

                <xsl:call-template name="exp-protocols-section">
                    <xsl:with-param name="pBasePath" select="$context-path"/>
                </xsl:call-template>

                <xsl:call-template name="exp-description-section">
                    <xsl:with-param name="pQueryId" select="$queryid"/>
                </xsl:call-template>

                <xsl:call-template name="exp-keywords-section">
                    <xsl:with-param name="pQueryId" select="$queryid"/>
                </xsl:call-template>

                <xsl:call-template name="exp-contact-section">
                    <xsl:with-param name="pQueryId" select="$queryid"/>
                </xsl:call-template>

                <xsl:call-template name="exp-citation-section">
                    <xsl:with-param name="pQueryId" select="$queryid"/>
                </xsl:call-template>

                <xsl:call-template name="exp-minseqe-section"/>

                <xsl:call-template name="exp-miame-section"/>

                <xsl:if test="fn:not($userid)">
                    <xsl:call-template name="exp-experimental-factors-section">
                        <xsl:with-param name="pQueryId" select="$queryid"/>
                    </xsl:call-template>

                    <xsl:call-template name="exp-sample-attributes-section">
                        <xsl:with-param name="pQueryId" select="$queryid"/>
                    </xsl:call-template>
                </xsl:if>

                <xsl:call-template name="exp-files-section">
                    <xsl:with-param name="pBasePath" select="$context-path"/>
                    <xsl:with-param name="pFiles" select="$vFiles"/>
                </xsl:call-template>

                <xsl:call-template name="exp-links-section">
                    <xsl:with-param name="pQueryId" select="$queryid"/>
                    <xsl:with-param name="pBasePath" select="$context-path"/>
                </xsl:call-template>

                <xsl:if test="fn:not($userid) or (fn:not(fn:not($userid)) and fn:not($userid = '1') and (user/@id = 1))">
                    <xsl:call-template name="exp-stats-section"/>
                </xsl:if>
                -->

            </table>
        </div>
    </xsl:template>

</xsl:stylesheet>
