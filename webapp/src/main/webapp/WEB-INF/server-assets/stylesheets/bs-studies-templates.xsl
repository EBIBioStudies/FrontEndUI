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

    <xsl:include href="bs-highlight.xsl"/>
    <xsl:include href="bs-file-functions.xsl"/>
    <xsl:include href="bs-date-functions.xsl"/>

    <xsl:template name="study-status">
        <xsl:param name="pIsGoogleBot" as="xs:boolean"/>
        <xsl:param name="pIsPrivate" as="xs:boolean"/>

        <xsl:variable name="vDates" select="submissiondate | lastupdatedate | releasedate"/>
        <xsl:variable name="vAccession" select="accession"/>
        <xsl:choose>
            <xsl:when test="$pIsGoogleBot">
                <xsl:call-template name="section">
                    <xsl:with-param name="pName" select="'Released on'"/>
                    <xsl:with-param name="pContent">
                        <releasedate>
                            <xsl:value-of select="(ae:formatDateGoogle(releasedate))"/>
                        </releasedate>
                    </xsl:with-param>
                    <xsl:with-param name="pClass" select="('left')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <div id="ae-detail-release-date">
                <xsl:text>Released </xsl:text><xsl:value-of select="ae:formatDate(./releasedate)"/>
                </div>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="study-attributes">
        <xsl:param name="pQueryId"/>
        <xsl:param name="pNodes"/>
        <xsl:for-each-group select="$pNodes" group-by="fn:lower-case(@name)">
            <xsl:call-template name="section">
                <xsl:with-param name="pName" select="fn:current-group()[1]/@name"/>
                <xsl:with-param name="pContent">
                    <xsl:for-each select="fn:current-group()">
                        <xsl:call-template name="highlight">
                            <xsl:with-param name="pQueryId" select="$pQueryId"/>
                            <xsl:with-param name="pText" select="value"/>
                            <xsl:with-param name="pCallHighlightingFunction" select="true()"/>
                        </xsl:call-template>
                        <xsl:if test="fn:position() != fn:last()">, </xsl:if>
                    </xsl:for-each>
                </xsl:with-param>
                <xsl:with-param name="pClass" select="'justify'"/>
            </xsl:call-template>
        </xsl:for-each-group>
    </xsl:template>

    <xsl:template name="study-subsections">
        <xsl:param name="pQueryId"/>
        <xsl:param name="pNodes"/>
        <xsl:param name="vFiles"/>
        <xsl:for-each select="$pNodes">
            <xsl:call-template name="section">
                <xsl:with-param name="pName" select="@type"/>
                <xsl:with-param name="pContent">
                    <xsl:value-of select="./*[not(fn:name()='file')]"/>
                </xsl:with-param>
                <xsl:with-param name="pClass" select="('left')"/>
            </xsl:call-template>
            <xsl:if test="fn:count(.//file)>0">
                <a class="show-more toggle-files">show files in this section</a>
                <div class="ae-section-files">
                    <xsl:call-template name="file-table">
                        <xsl:with-param name="pQueryId" select="$queryid"/>
                        <xsl:with-param name="pNodes" select=".//file"/>
                        <xsl:with-param name="pFiles" select="$vFiles"/>
                        <xsl:with-param name="pBasePath" select="$context-path"/>
                    </xsl:call-template>
                </div>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="study-publications">
        <xsl:param name="pQueryId"/>
        <xsl:param name="pTitle"/>
        <xsl:param name="pNodes"/>
        <xsl:param name="vFiles"/>
        <xsl:for-each-group select="$pNodes" group-by="fn:lower-case(@type)">
            <xsl:choose>
                <xsl:when test="fn:current-grouping-key()='publication'">
                    <xsl:call-template name="section">
                        <xsl:with-param name="pName" select="'Published In'"/>
                        <xsl:with-param name="pContent">
                            <xsl:for-each select="fn:current-group()">
                                <xsl:call-template name="highlight">
                                    <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                    <xsl:with-param name="pCallHighlightingFunction" select="true()"/>
                                    <xsl:with-param name="pText">
                                        <xsl:value-of select="attribute[fn:lower-case(@name)='journal']"/>
                                        <xsl:text> [</xsl:text>
                                        <xsl:value-of select="attribute[fn:lower-case(@name)='publication date']"/>
                                        <xsl:text>, </xsl:text>
                                        <xsl:value-of select="attribute[fn:lower-case(@name)='volume']"/>
                                        <xsl:text>:</xsl:text>
                                        <xsl:value-of select="attribute[fn:lower-case(@name)='pages']"/>
                                        <xsl:text>]</xsl:text>
                                    </xsl:with-param>
                                </xsl:call-template>
                                <xsl:text>&#160;(PMCID: </xsl:text>
                                <a href="http://europepmc.org/articles/{attribute[fn:lower-case(@name)='pmcid']}" target="_blank">
                                    <xsl:call-template name="highlight">
                                        <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                        <xsl:with-param name="pCallHighlightingFunction" select="true()"/>
                                        <xsl:with-param name="pText" select="attribute[fn:lower-case(@name)='pmcid']"/>
                                    </xsl:call-template>
                                </a>
                                <xsl:text>)</xsl:text>
                            </xsl:for-each>
                            <xsl:if test="fn:count(.//file)>0">
                                <a class="show-more toggle-files">show files in this section</a>
                                <div class="ae-section-files">
                                    <xsl:call-template name="file-table">
                                        <xsl:with-param name="pQueryId" select="$queryid"/>
                                        <xsl:with-param name="pNodes" select=".//file"/>
                                        <xsl:with-param name="pFiles" select="$vFiles"/>
                                        <xsl:with-param name="pBasePath" select="$context-path"/>
                                    </xsl:call-template>
                                </div>
                            </xsl:if>
                            <br/>
                            <xsl:if test="fn:exists(.//link)">
                                <a class="show-more toggle-links">show links in this section</a>
                                <div class="ae-section-links">
                                    <div class="ae-section-file-title">Links</div>
                                    <xsl:call-template name="link-table">
                                        <xsl:with-param name="pQueryId" select="$queryid"/>
                                        <xsl:with-param name="pNodes" select="$pNodes//link"/>
                                    </xsl:call-template>
                                </div>
                            </xsl:if>
                        </xsl:with-param>
                        <xsl:with-param name="pClass" select="('left')"/>
                    </xsl:call-template>
                </xsl:when>
                <xsl:otherwise/>
            </xsl:choose>
        </xsl:for-each-group>
    </xsl:template>

    <xsl:template name="study-authors">
        <xsl:param name="pQueryId"/>
        <xsl:param name="pTitle"/>
        <xsl:param name="pNodes"/>
        <xsl:for-each-group select="$pNodes" group-by="fn:lower-case(@type)">
            <xsl:if test="fn:current-grouping-key()='author'">
                <xsl:call-template name="section">
                    <xsl:with-param name="pClass" select="('justify')"/>
                    <xsl:with-param name="pContent">
                        <xsl:variable name="vOrgRefs">
                            <orgs>
                                <xsl:for-each
                                        select="fn:current-group()/attribute[fn:lower-case(@name)='affiliation']/value">
                                    <org acc="{fn:current()}">
                                        <xsl:value-of
                                                select="$pNodes[fn:lower-case(@type)='organization' and @acc=fn:current()]/attribute[fn:lower-case(@name)='name']/value"/>
                                    </org>
                                </xsl:for-each>
                            </orgs>
                        </xsl:variable>
                        <xsl:variable name="vUniqueRefs">
                            <orgs>
                                <xsl:copy-of select="$vOrgRefs/orgs/org[not(@acc=preceding-sibling::org/@acc)]"/>
                            </orgs>
                        </xsl:variable>
                        <xsl:for-each select="current-group()">
                            <xsl:call-template name="highlight">
                                <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                <xsl:with-param name="pCallHighlightingFunction" select="true()"/>
                                <xsl:with-param name="pText" select="attribute[fn:lower-case(@name)='name']/value"/>
                            </xsl:call-template>
                            <xsl:variable name="vAffiliationId" select="attribute[fn:lower-case(@name)='affiliation']/value"/>
                            <xsl:variable name="vAffiliation" select="$vUniqueRefs/orgs/org[@acc=$vAffiliationId]"/>
                            <xsl:if test="fn:count($vUniqueRefs/orgs/org) > 1 and $vAffiliation">
                                <sup>
                                    <a class="org-link">
                                        <xsl:attribute name="href" select="fn:concat('#affiliation',fn:count($vAffiliation/preceding-sibling::org) + 1)"></xsl:attribute>
                                        <xsl:value-of select="fn:count($vAffiliation/preceding-sibling::org) + 1"/>
                                    </a>
                                </sup>
                            </xsl:if>
                            <xsl:if test="fn:position() != fn:last()">
                                <xsl:text>, </xsl:text>
                            </xsl:if>
                        </xsl:for-each>
                        <xsl:if test="fn:count($vUniqueRefs/orgs/org)>0">
                            <xsl:variable name="vSize" select="fn:count($vUniqueRefs/orgs/org)"/>
                            <p class="orgs"  id="hidden-orgs" >
                                <xsl:for-each select="$vUniqueRefs/orgs/org[fn:position() = (1 to 10)]">
                                    <xsl:if test="fn:count($vUniqueRefs/orgs/org) > 1">
                                        <span class="ae-detail-affilliation"><xsl:attribute name="id" select="fn:concat('affiliation',position())"></xsl:attribute></span>
                                        <sup>
                                                <xsl:value-of select="position()"/>
                                        </sup>
                                    </xsl:if>
                                    <span class="org-name">
                                        <xsl:call-template name="highlight">
                                            <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                            <xsl:with-param name="pCallHighlightingFunction" select="true()"/>
                                            <xsl:with-param name="pText" select="."/>
                                        </xsl:call-template>
                                    </span>
                                    <xsl:if test="$vSize &gt; 10">
                                        <xsl:text>, </xsl:text>
                                    </xsl:if>
                                </xsl:for-each>
                                <xsl:if test="$vSize &gt; 10">
                                    <span class="hidden-values" size="{$vSize - 10}">
                                        <xsl:for-each select="$vUniqueRefs/orgs/org[fn:position() = (11 to $vSize)]">
                                            <xsl:if test="fn:count($vUniqueRefs/orgs/org) > 1">
                                                <span class="ae-detail-affilliation"><xsl:attribute name="id" select="fn:concat('affiliation',position()+10)"></xsl:attribute></span>
                                                <sup>
                                                    <xsl:value-of select="position()+10"/>
                                                </sup>
                                            </xsl:if>
                                            <span class="org-name">
                                                <xsl:call-template name="highlight">
                                                    <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                                    <xsl:with-param name="pText" select="."/>
                                                    <xsl:with-param name="pCallHighlightingFunction" select="true()"/>
                                                </xsl:call-template>
                                            </span>
                                            <xsl:if test="fn:position() != fn:last()">
                                                <xsl:text>, </xsl:text>
                                            </xsl:if>
                                        </xsl:for-each>
                                    </span>
                                </xsl:if>
                            </p>
                        </xsl:if>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:if>
        </xsl:for-each-group>
    </xsl:template>

    <xsl:template name="study-files">
        <xsl:param name="pQueryId"/>
        <xsl:param name="pNodes"/>
        <xsl:param name="pFiles"/>
        <xsl:param name="pBasePath"/>
        <xsl:if test="fn:count($pNodes)>0">
            <xsl:call-template name="widget">
                <xsl:with-param name="pTitleClass" select="'ae-detail-files-title'"/>
                <xsl:with-param name="pIconClass" select="'icon icon-functional padded-gray-icon'"/>
                <xsl:with-param name="pIconType" select="'='"/>
                <xsl:with-param name="pName" select="'Download data files'"/>
                <xsl:with-param name="pContent">
                    <xsl:call-template name="file-table">
                        <xsl:with-param name="pNodes" select="$pNodes"/>
                        <xsl:with-param name="pQueryId" select="$pQueryId"/>
                        <xsl:with-param name="pFiles" select="$pFiles"/>
                        <xsl:with-param name="pBasePath" select="$pBasePath"/>
                        <xsl:with-param name="elementId" select="'file-list'"/>
                    </xsl:call-template>
                    <span id="selected-file-text"/> <a id="download-selected-files">Download all</a><br/><br/>
                </xsl:with-param>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <xsl:template name="file-table"><xsl:param name="pQueryId"/>
        <xsl:param name="pNodes"/>
        <xsl:param name="pFiles"/>
        <xsl:param name="pBasePath"/>
        <xsl:param name="elementId" select="fn:concat('file-table-',../position())"/>
        <xsl:variable name="vColumns" select="distinct-values($pNodes/attribute[@name!='Type']/@name)"/>
        <table class="stripe compact hover file-list" cellspacing="0" width="100%" id="{$elementId}" >
            <thead>
                <tr>
                    <xsl:if test="$elementId='file-list'">
                        <th id="select-all-files-header">
                            <input type="checkbox" id="select-all-files"/>
                        </th>
                    </xsl:if>
                    <th>Name</th>
                    <th class="filesize">Size</th>
                    <xsl:for-each select="$vColumns">
                        <th>
                            <xsl:value-of select="."/>
                        </th>
                    </xsl:for-each>
                </tr>
            </thead>
            <tbody>
                <xsl:for-each select="$pNodes">
                    <xsl:variable name="aFile" select="."/>
                    <xsl:variable name="vName" select="@name"/>
                    <xsl:variable name="vFile" select="$pFiles/file[@name=$vName]"/>
                    <tr>
                        <xsl:if test="$elementId='file-list'">
                            <td class="disable-select">
                                <input class="text-bottom" type="checkbox"/>
                            </td>
                        </xsl:if>
                        <td class="file-list-file-name">
                            <a href="{$pBasePath}/files/{$pFiles/@accession}/{$vName}">
                                <xsl:call-template name="highlight">
                                    <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                    <xsl:with-param name="pText" select="$vName"/>
                                    <xsl:with-param name="pCallHighlightingFunction" select="true()"/>
                                </xsl:call-template>
                            </a>
                            <xsl:variable name="isImage" select="matches(lower-case(tokenize($vName, '\.')[last()]),'bmp|jpg|wbmp|jpeg|png|gif|tif|tiff|pdf|docx|txt|csv')"/>
                            <a href="{$pBasePath}/files/{$pFiles/@accession}/{$vName}" data-name="{$vName}" class="file-link">
                                <xsl:if test="$isImage">
                                    <xsl:attribute name="data-thumbnail" select="concat($pBasePath,'/thumbnail/',$pFiles/@accession,'/',$vName)"/>
                                </xsl:if>
                                <span class="thumbnail icon icon-functional" data-icon="4">
                                    <xsl:if test="not($isImage)">
                                        <xsl:attribute name="class" select="('invisible')"/>
                                    </xsl:if>
                                </span>
                            </a>

                        </td>
                        <td class="align-right">
                            <xsl:call-template name="file-size">
                                <xsl:with-param name="size" select="$vFile/@size"/>
                            </xsl:call-template>
                        </td>
                        <xsl:for-each select="$vColumns">
                            <xsl:variable name="vColumnName" select="."/>
                            <xsl:variable name="vColumn" select="$aFile/attribute[@name=$vColumnName]"/>
                            <td>
                                <xsl:choose>
                                    <xsl:when test="fn:exists($vColumn/url)">
                                        <xsl:variable name="text">
                                            <xsl:value-of select="$vColumn/value" disable-output-escaping="yes"/>
                                        </xsl:variable>
                                        <a href="{$vColumn/url}" target="_blank">
                                            <xsl:call-template name="highlight">
                                                <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                                <xsl:with-param name="pText" select="$text"/>
                                                <xsl:with-param name="pCallHighlightingFunction" select="true()"/>
                                            </xsl:call-template>
                                        </a>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:variable name="text">
                                            <xsl:value-of select="$vColumn/value" disable-output-escaping="yes"/>
                                        </xsl:variable>
                                        <xsl:call-template name="highlight">
                                            <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                            <xsl:with-param name="pText" select="$text"/>
                                            <xsl:with-param name="pCallHighlightingFunction" select="true()"/>
                                        </xsl:call-template>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </td>
                        </xsl:for-each>
                    </tr>
                </xsl:for-each>
            </tbody>
        </table>
    </xsl:template>
    <xsl:template name="file-size">
        <xsl:param name="size"/>
        <span class="ae-file-size">
            <xsl:choose>
                <xsl:when test="$size &gt;= 1073741824">
                    <xsl:value-of select="format-number($size div 1073741824,'#,###')"/>
                    <xsl:text> GB</xsl:text>
                </xsl:when>
                <xsl:when test="$size &gt;= 1048576">
                    <xsl:value-of select="format-number($size div 1048576,'#,###')"/>
                    <xsl:text> MB</xsl:text>
                </xsl:when>
                <xsl:when test="$size &gt;= 1024">
                    <xsl:value-of select="format-number($size div 1024,'#,###')"/>
                    <xsl:text> KB</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="format-number($size,'#,###')"/>
                    <xsl:text> bytes</xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </span>
    </xsl:template>

    <xsl:template name="study-links">
    <xsl:param name="pQueryId"/>
    <xsl:param name="pNodes"/>
        <xsl:if test="fn:count($pNodes)>0">
            <xsl:call-template name="widget">
            <xsl:with-param name="pName" select="'Linked information'"/>
            <xsl:with-param name="pTitleClass" select="'ae-detail-links-title'"/>
            <xsl:with-param name="pIconClass" select="'icon icon-generic padded-gray-icon'"/>
            <xsl:with-param name="pIconType" select="'x'"/>
            <xsl:with-param name="pClass" select="('left')"/>
            <xsl:with-param name="pContent">
                <xsl:call-template name="link-table">
                    <xsl:with-param name="pNodes" select="$pNodes"/>
                    <xsl:with-param name="pQueryId" select="$pQueryId"/>
                    <xsl:with-param name="elementId" select="'link-list'"/> <!-- as in a list of links, not a linked list -->
                </xsl:call-template>
            </xsl:with-param>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <xsl:template name="link-table">
        <xsl:param name="pQueryId"/>
        <xsl:param name="pNodes"/>
        <xsl:param name="elementId" select="fn:concat('link-table-',../position())"/>
        <xsl:variable name="vColumns" select="distinct-values($pNodes/attribute[fn:lower-case(@name)!='type' and fn:lower-case(@name)!='description']/@name)"/>
        <table class="stripe compact hover link-list" cellspacing="0" width="100%" id="{$elementId}" >
            <thead>
                <tr>
                    <th>Name</th>
                    <th>Type</th>
                    <xsl:for-each select="$vColumns">
                        <th>
                            <xsl:value-of select="."/>
                        </th>
                    </xsl:for-each>
                </tr>
            </thead>
            <tbody>
                <xsl:for-each select="$pNodes">
                    <xsl:variable name="aFile" select="."/>
                    <tr>
                        <td>
                            <xsl:call-template name="highlight-reference">
                                <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                <xsl:with-param name="pType" select="attribute[@name='Type']/value"/>
                                <xsl:with-param name="pText" select="if (attribute[fn:lower-case(@name)='description']) then attribute[fn:lower-case(@name)='description']/value else @url"/>
                                <xsl:with-param name="pUrl" select="@url" />
                                <xsl:with-param name="pCallHighlightingFunction" select="true()"/>
                            </xsl:call-template>
                        </td>
                        <td>
                            <xsl:call-template name="highlight">
                                <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                <xsl:with-param name="pText" select="ae:getTitleFor(attribute[@name='Type']/value)" />
                                <xsl:with-param name="pCallHighlightingFunction" select="true()"/>
                            </xsl:call-template>
                        </td>
                        <xsl:for-each select="$vColumns">
                            <xsl:variable name="vColumnName" select="."/>
                            <xsl:variable name="vColumn" select="$aFile/attribute[@name=$vColumnName]"/>
                            <td>
                                <xsl:choose>
                                    <xsl:when test="fn:exists($vColumn/url)">
                                        <xsl:variable name="text">
                                            <xsl:value-of select="$vColumn/value" disable-output-escaping="yes"/>
                                        </xsl:variable>
                                        <a href="{$vColumn/url}" target="_blank">
                                            <xsl:call-template name="highlight">
                                                <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                                <xsl:with-param name="pText" select="$text"/>
                                                <xsl:with-param name="pCallHighlightingFunction" select="true()"/>
                                            </xsl:call-template>
                                        </a>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:variable name="text">
                                            <xsl:value-of select="$vColumn/value" disable-output-escaping="yes"/>
                                        </xsl:variable>
                                        <xsl:call-template name="highlight">
                                            <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                            <xsl:with-param name="pText" select="$text"/>
                                            <xsl:with-param name="pCallHighlightingFunction" select="true()"/>
                                        </xsl:call-template>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </td>
                        </xsl:for-each>
                    </tr>
                </xsl:for-each>
            </tbody>
        </table>
    </xsl:template>

    <xsl:template name="study-funding">
        <xsl:param name="pQueryId"/>
        <xsl:param name="pNodes"/>
        <xsl:param name="vFiles" select="''"/>
        <xsl:call-template name="section">
            <xsl:with-param name="pName" select="'Funding'"/>
            <xsl:with-param name="pClass" select="('ae-detail-funding-list')"/>
            <xsl:with-param name="pContent">
                <ul class="ae-detail-list">
                    <xsl:for-each-group select="$pNodes" group-by="attribute[fn:lower-case(@name)='agency']">
                        <li>
                            <span class="ae-detail-group-heading">
                                <xsl:call-template name="highlight">
                                    <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                    <xsl:with-param name="pCallHighlightingFunction" select="true()"/>
                                    <xsl:with-param name="pText" select="fn:current-grouping-key()"/>
                                </xsl:call-template>
                                <xsl:if test="fn:current-group()/attribute[@name='grant_id']">
                                    <xsl:text>: </xsl:text>
                                </xsl:if>
                            </span>
                            <xsl:call-template name="highlighted-list">
                                <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                <xsl:with-param name="pCallHighlightingFunction" select="true()"/>
                                <xsl:with-param name="pType" select="fn:current-grouping-key()"/>
                                <xsl:with-param name="pList" select="fn:current-group()/attribute[@name='grant_id']"/>
                            </xsl:call-template>
                        </li>
                    </xsl:for-each-group>
                </ul>
                <xsl:if test="fn:exists($pNodes//file)">
                    <a class="show-more toggle-files">show files in this section</a>
                    <div class="ae-section-files">
                        <div class="ae-section-file-title">Files</div>
                        <xsl:call-template name="file-table">
                            <xsl:with-param name="pQueryId" select="$queryid"/>
                            <xsl:with-param name="pNodes" select="$pNodes//file"/>
                            <xsl:with-param name="pFiles" select="$vFiles"/>
                            <xsl:with-param name="pBasePath" select="$context-path"/>
                        </xsl:call-template>
                    </div>
                </xsl:if>
                <br/>
                <xsl:if test="fn:exists($pNodes//link)">
                    <a class="show-more toggle-links">show links in this section</a>
                    <div class="ae-section-links">
                        <div class="ae-section-link-title">Links</div>
                        <xsl:call-template name="link-table">
                            <xsl:with-param name="pQueryId" select="$queryid"/>
                            <xsl:with-param name="pNodes" select="$pNodes//link"/>
                        </xsl:call-template>
                    </div>
                </xsl:if>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="section">
        <xsl:param name="pName" select="''"/>
        <xsl:param name="pContent"/>
        <xsl:param name="pClass" as="xs:string*" select="''"/>
        <xsl:param name="queryid" select="''"/>
        <xsl:param name="pNodes" select="''"/>
        <xsl:param name="vFiles"/>
        <xsl:param name="context-path" select="''"/>
        <xsl:if test="fn:exists($pName) and fn:not(fn:matches(fn:string-join($pContent//text(), ''), '^\s*$'))">
            <xsl:if test="fn:exists($pName) and fn:matches($pName,'[^\s*]')">
                <div class="ae-detail-name"><xsl:value-of select="$pName"/></div>
            </xsl:if>
            <div>
                <xsl:attribute name="class" select="fn:string-join((('value'),$pClass), ' ')"/>
                <xsl:copy-of select="$pContent"/>
            </div>
        </xsl:if>
        <xsl:if test="fn:exists($vFiles) and $vFiles!=''">
            <div class="ae-section-file-title">Files</div>
            <xsl:call-template name="file-table">
                <xsl:with-param name="pQueryId" select="$queryid"/>
                <xsl:with-param name="pNodes" select="$pNodes//file"/>
                <xsl:with-param name="pFiles" select="$vFiles"/>
                <xsl:with-param name="pBasePath" select="$context-path"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <xsl:template name="widget">
        <xsl:param name="pName" select="''"/>
        <xsl:param name="pContent"/>
        <xsl:param name="pClass" as="xs:string*" select="''"/>
        <xsl:param name="pTitleClass" as="xs:string*" select="''"/>
        <xsl:param name="pIconClass" as="xs:string*" select="''"/>
        <xsl:param name="pIconType" as="xs:string*" select="''"/>
        <xsl:if test="fn:exists($pName) and fn:not(fn:matches(fn:string-join($pContent//text(), ''), '^\s*$'))">
            <xsl:if test="fn:exists($pName) and fn:matches($pName,'[^\s*]')">
                <div class="ae-detail-name">
                    <xsl:if test="fn:exists($pTitleClass)  and fn:not(fn:matches(fn:string-join($pTitleClass, ''), '^\s*$'))">
                        <xsl:attribute name="class" select="fn:string-join($pTitleClass, ' ')"/>
                    </xsl:if>
                    <xsl:if test="fn:exists($pIconClass)   and fn:not(fn:matches(fn:string-join($pIconClass, ''), '^\s*$'))">
                        <span>
                            <xsl:attribute name="class" select="fn:string-join($pIconClass, ' ')"/>
                            <xsl:attribute name="data-icon" select="fn:string-join($pIconType, ' ')"/>
                        </span>
                    </xsl:if>
                    <xsl:value-of select="$pName"/>
                </div>
            </xsl:if>
            <div>
                <xsl:attribute name="class" select="fn:string-join((('value'),$pClass), ' ')"/>
                <xsl:copy-of select="$pContent"/>
            </div>
        </xsl:if>
    </xsl:template>

    <xsl:template name="general-highlighted-list">
        <xsl:param name="pQueryId"/>
        <xsl:param name="pList"/>
        <xsl:param name="pSize"/>
        <xsl:variable name="vSize" select="fn:count($pList)"/>
        <xsl:for-each select="$pList[fn:position() = (1 to $pSize)]">
            <xsl:call-template name="highlight">
                <xsl:with-param name="pQueryId" select="$pQueryId"/>
                <xsl:with-param name="pText" select="."/>
                <xsl:with-param name="pFieldName" select="''"/>
            </xsl:call-template>
            <xsl:if test="fn:position() != fn:last() or $vSize &gt; $pSize">
                <xsl:text>, </xsl:text>
            </xsl:if>
        </xsl:for-each>
        <xsl:if test="$vSize &gt; $pSize">
            <span class="hidden-values" size="{$vSize - $pSize}">
                <xsl:for-each select="$pList[fn:position() = ( ($pSize + 1) to $vSize)]">
                    <xsl:call-template name="highlight">
                        <xsl:with-param name="pQueryId" select="$pQueryId"/>
                        <xsl:with-param name="pText" select="."/>
                        <xsl:with-param name="pFieldName" select="''"/>
                    </xsl:call-template>
                    <xsl:if test="fn:position() != fn:last()">
                        <xsl:text>, </xsl:text>
                    </xsl:if>
                </xsl:for-each>
                <xsl:text> </xsl:text>
            </span>
        </xsl:if>
    </xsl:template>

    <xsl:template name="highlighted-list">
        <xsl:param name="pQueryId"/>
        <xsl:param name="pList"/>
        <xsl:param name="pType"/>
        <xsl:param name="pCallHighlightingFunction" as="xs:boolean?" select="false()"/>
        <xsl:variable name="vSize" select="fn:count($pList)"/>
        <xsl:for-each select="$pList[fn:position() = (1 to 20)]">
            <xsl:call-template name="highlight-reference">
                <xsl:with-param name="pQueryId" select="$pQueryId"/>
                <xsl:with-param name="pCallHighlightingFunction" select="$pCallHighlightingFunction"/>
                <xsl:with-param name="pText" select="."/>
                <xsl:with-param name="pType" select="$pType"/>
            </xsl:call-template>
            <xsl:if test="fn:position() != fn:last() or $vSize &gt; 20">
                <xsl:text>, </xsl:text>
            </xsl:if>
        </xsl:for-each>
        <xsl:if test="$vSize &gt; 20">
            <span class="hidden-values" size="{$vSize - 20}">
                <xsl:for-each select="$pList[fn:position() = (21 to $vSize)]">
                    <xsl:call-template name="highlight-reference">
                        <xsl:with-param name="pQueryId" select="$pQueryId"/>
                        <xsl:with-param name="pCallHighlightingFunction" select="$pCallHighlightingFunction"/>
                        <xsl:with-param name="pText" select="."/>
                        <xsl:with-param name="pType" select="$pType"/>
                    </xsl:call-template>
                    <xsl:if test="fn:position() != fn:last()">
                        <xsl:text>, </xsl:text>
                    </xsl:if>
                </xsl:for-each>
                <xsl:text> </xsl:text>
            </span>
        </xsl:if>
    </xsl:template>

    <xsl:template name="highlight-reference">
        <xsl:param name="pQueryId"/>
        <xsl:param name="pText"/>
        <xsl:param name="pType"/>
        <xsl:param name="pUrl" select="''"/>
        <xsl:param name="pCallHighlightingFunction" as="xs:boolean?"  select="false()"/>
        <xsl:variable name="vUrl" select="ae:getUrlFor($pType, $pText, $pUrl)"/>
        <xsl:choose>
            <xsl:when test="$vUrl != ''">
                <a href="{$vUrl}" target="_blank">
                    <xsl:call-template name="highlight">
                        <xsl:with-param name="pQueryId" select="$pQueryId"/>
                        <xsl:with-param name="pCallHighlightingFunction" select="$pCallHighlightingFunction"/>
                        <xsl:with-param name="pText" select="$pText"/>
                    </xsl:call-template>
                </a>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="highlight">
                    <xsl:with-param name="pQueryId" select="$pQueryId"/>
                    <xsl:with-param name="pCallHighlightingFunction" select="$pCallHighlightingFunction"/>
                    <xsl:with-param name="pText" select="$pText"/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:function name="ae:getTitleFor">
        <xsl:param name="pType"/>
        <xsl:variable name="type" select="fn:lower-case($pType)"/>
        <xsl:choose>
            <xsl:when test="$type = 'sprot'">
                <xsl:value-of select="'UniProt'"/>
            </xsl:when>
            <xsl:when test="$type = 'gen'">
                <xsl:value-of select="'ENA'"/>
            </xsl:when>
            <xsl:when test="$type = 'arrayexpress'">
                <xsl:value-of select="'ArrayExpress'"/>
            </xsl:when>
            <xsl:when test="$type = 'refsnp'">
                <xsl:value-of select="'dbSNP'"/>
            </xsl:when>
            <xsl:when test="$type = 'pdb'">
                <xsl:value-of select="'PDBe'"/>
            </xsl:when>
            <xsl:when test="$type = 'pfam'">
                <xsl:value-of select="'Pfam'"/>
            </xsl:when>
            <xsl:when test="$type = 'omim'">
                <xsl:value-of select="'OMIM'"/>
            </xsl:when>
            <xsl:when test="$type = 'interpro'">
                <xsl:value-of select="'InterPro'"/>
            </xsl:when>
            <xsl:when test="$type = 'refseq'">
                <xsl:value-of select="'Nucleotide'"/>
            </xsl:when>
            <xsl:when test="$type = 'ensembl'">
                <xsl:value-of select="'Ensembl'"/>
            </xsl:when>
            <xsl:when test="$type = 'doi'">
                <xsl:value-of select="'DOI'"/>
            </xsl:when>
            <xsl:when test="$type = 'intact'">
                <xsl:value-of select="'IntAct'"/>
            </xsl:when>
            <xsl:when test="$type = 'chebi'">
                <xsl:value-of select="'ChEBI'"/>
            </xsl:when>
            <xsl:when test="$type = 'ega'">
                <xsl:value-of select="'EGA'"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$pType"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="ae:getUrlFor">
        <xsl:param name="pType"/>
        <xsl:param name="pId"/>
        <xsl:param name="pUrl"/>
        <xsl:variable name="type" select="fn:lower-case($pType)"/>
        <xsl:choose>
            <xsl:when test="$type = 'sprot'">
                <xsl:value-of select="fn:concat('http://www.uniprot.org/uniprot/', $pId)"/>
            </xsl:when>
            <xsl:when test="$type = 'gen'">
                <xsl:value-of select="fn:concat('http://www.ebi.ac.uk/ena/data/view/', $pId)"/>
            </xsl:when>
            <xsl:when test="$type = 'arrayexpress'">
                <xsl:value-of select="fn:concat('http://www.ebi.ac.uk/arrayexpress/experiments/', $pId)"/>
            </xsl:when>
            <xsl:when test="$type = 'refsnp'">
                <xsl:value-of select="fn:concat('http://www.ncbi.nlm.nih.gov/SNP/snp_ref.cgi?rs=', $pId)"/>
            </xsl:when>
            <xsl:when test="$type = 'pdb'">
                <xsl:value-of select="fn:concat('http://www.ebi.ac.uk/pdbe-srv/view/entry/', $pId, '/summary')"/>
            </xsl:when>
            <xsl:when test="$type = 'pfam'">
                <xsl:value-of select="fn:concat('http://pfam.xfam.org/family/', $pId)"/>
            </xsl:when>
            <xsl:when test="$type = 'omim'">
                <xsl:value-of select="fn:concat('http://omim.org/entry/', $pId)"/>
            </xsl:when>
            <xsl:when test="$type = 'interpro'">
                <xsl:value-of select="fn:concat('http://www.ebi.ac.uk/interpro/entry/', $pId)"/>
            </xsl:when>
            <xsl:when test="$type = 'refseq'">
                <xsl:value-of select="fn:concat('http://www.ncbi.nlm.nih.gov/nuccore/', $pId)"/>
            </xsl:when>
            <xsl:when test="$type = 'doi'">
                <xsl:value-of select="fn:concat('http://dx.doi.org/', $pId)"/>
            </xsl:when>
            <xsl:when test="$type = 'intact'">
                <xsl:value-of select="fn:concat('http://www.ebi.ac.uk/intact/pages/details/details.xhtml?experimentAc=', $pId)"/>
            </xsl:when>
            <xsl:when test="$type = 'chebi'">
                <xsl:value-of select="fn:concat('http://www.ebi.ac.uk/chebi/searchId.do?chebiId=', fn:replace($pId, '[:]', '%3A'))"/>
            </xsl:when>
            <xsl:when test="$type = 'ega'">
                <xsl:value-of select="fn:concat('https://ega.crg.eu/datasets/', $pId)"/>
            </xsl:when>
            <xsl:when test="fn:starts-with($pUrl,'http:') or fn:starts-with($pUrl,'https:') or fn:starts-with($pUrl,'ftp:')">
                <xsl:value-of select="$pUrl"/>
            </xsl:when>
        </xsl:choose>
    </xsl:function>
</xsl:stylesheet>