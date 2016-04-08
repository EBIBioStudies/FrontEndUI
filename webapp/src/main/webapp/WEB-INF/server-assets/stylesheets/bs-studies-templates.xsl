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

        <xsl:variable name="vDates" select="submissiondate | lastupdatedate | releasedate"/>
        <xsl:variable name="vAccession" select="accession"/>
        <xsl:choose>
            <xsl:when test="$pIsGoogleBot and fn:exists(@releaseTime) ">
                <xsl:call-template name="section">
                    <xsl:with-param name="pName" select="'Released on'"/>
                    <xsl:with-param name="pContent">
                        <releasedate>
                            <xsl:value-of select="(ae:formatDateGoogle(ae:unixTimeToDate(@releaseTime)))"/>
                        </releasedate>
                    </xsl:with-param>
                    <xsl:with-param name="pClass" select="('left')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <div id="ae-detail-release-date">
                    <xsl:choose>
                        <xsl:when test="@releaseTime!=9999999999 and contains(concat(' ',lower-case(access),' '),' public ')">
                            <text>Released </text><xsl:value-of select="ae:formatDateLong(ae:unixTimeToDate(@releaseTime))"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <span class="study-meta-data" data-icon="L">&#x1f512; private</span>
                            <span class="study-meta-data">Created <xsl:value-of select="ae:formatDateLong(ae:unixTimeToDate(@creationTime))"/></span>
                        </xsl:otherwise>
                    </xsl:choose>
                </div>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>
    <xsl:template name="study-download">
        <xsl:param name="pBasePath"/>
        <xsl:param name="pAccession"/>
        <div id="download-source">
            <a href="{$pBasePath}/files/{$pAccession}/{$pAccession}.json" target="_blank" title="Download Study as JSON"
               class="source-icon source-icon-json" data-icon="=">{JSON}</a>
            <a href="{$pBasePath}/files/{$pAccession}/{$pAccession}.xml" target="_blank" title="Download Study as XML"
               class="source-icon source-icon-xml" data-icon="=">&lt;XML&gt;</a>
            <a href="{$pBasePath}/files/{$pAccession}/{$pAccession}.pagetab.tsv" target="_blank"
               title="Download Study as PageTab" class="source-icon source-icon-pagetab"
               data-icon="=">→PageTab↲</a>
        </div>
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
                        <xsl:for-each select="./*[lower-case(name())='ontology']">
                            <span>
                                <xsl:attribute name="data-ontology">
                                    <xsl:value-of select="."/>
                                </xsl:attribute>
                                <xsl:attribute name="data-term-id">
                                    <xsl:value-of select="following-sibling::*[lower-case(name())='termid'][1]"/>
                                </xsl:attribute>
                                <xsl:attribute name="data-term-name">
                                    <xsl:value-of select="following-sibling::*[lower-case(name())='termname'][1]"/>
                                </xsl:attribute>
                            </span>
                        </xsl:for-each>
                        <xsl:call-template name="study-sub-attributes">
                            <xsl:with-param name="pSubAttributes" select="./*[lower-case(name())!='value' and lower-case(name())!='ontology' and lower-case(name())!='termid' and lower-case(name())!='termname']"/>
                        </xsl:call-template>
                        <xsl:if test="fn:position() != fn:last()">, </xsl:if>
                    </xsl:for-each>
                </xsl:with-param>
                <xsl:with-param name="pClass" select="'justify'"/>
            </xsl:call-template>
        </xsl:for-each-group>
    </xsl:template>

    <xsl:template name="study-sub-attributes">
        <xsl:param name="pSubAttributes"/>
        <xsl:if test="count($pSubAttributes)>0">
            <i class="fa fa-info-circle sub-attribute-info"></i>
            <span class="sub-attribute">
                <xsl:for-each select="$pSubAttributes">
                    <span class="sub-attribute-title"><xsl:value-of select="name()"/>:</span>
                    <span><xsl:value-of select="text()"/></span>
                    <br/>
                </xsl:for-each>
            </span>
        </xsl:if>
    </xsl:template>

    <xsl:template name="study-subsections">
        <xsl:param name="pQueryId"/>
        <xsl:param name="pNodes"/>
        <xsl:for-each select="$pNodes">
            <xsl:variable name="vSectionTitle" select="if (fn:exists(attributes/attribute[lower-case(@name)='title'])) then attributes/attribute[lower-case(@name)='title']/value else if (exists(attribute[lower-case(@name)='title'])) then attribute[lower-case(@name)='title'] else if (exists(@type)) then @type else 'Section'"/>
            <xsl:call-template name="section">
                <xsl:with-param name="pId" select="@acc"/>
                <xsl:with-param name="pName" select="$vSectionTitle"/>
                <xsl:with-param name="pContent">
                    <div>
                        <xsl:if test="descendant::section">
                            <xsl:attribute name="class" select="('has-child-section')"/>
                        </xsl:if>
                        <xsl:if test="ancestor::section">
                            <xsl:attribute name="class" select="('indented-section')"/>
                        </xsl:if>

                        <xsl:variable name="vContent" select="if (fn:exists(attributes/attribute[lower-case(@name)='description'])) then attributes/attribute[lower-case(@name)='description']/value else attribute[lower-case(@name)='description']/value"/>
                        <xsl:value-of select="$vContent"/>

                        <xsl:call-template name="study-attributes">
                            <xsl:with-param name="pNodes" select="attribute[lower-case(@name)!='title' and lower-case(@name)!='description']"/>
                            <xsl:with-param name="pQueryId" select="$pQueryId"/>
                        </xsl:call-template>

                        <xsl:call-template name="study-subsections">
                            <xsl:with-param name="pQueryId" select="$pQueryId"/>
                            <xsl:with-param name="pNodes" select="section | subsections/section"/>
                        </xsl:call-template>
                        <xsl:if test="fn:count(descendant::file)>0">
                            <div>
                            <a class="show-more toggle-files" data-total="{fn:count(descendant::file)}">show files in this section</a>
                                <xsl:for-each select="files/table">
                                    <div class="ae-section-files">
                                        <div class="ae-section-file-title">File Table: <xsl:value-of select="position()"/>
                                            <span class="fa fa-expand fa-icon table-expander" title="Click to expand"/>
                                        </div>
                                        <xsl:call-template name="file-table">
                                            <xsl:with-param name="pQueryId" select="$queryid"/>
                                            <xsl:with-param name="pNodes" select="file"/>
                                            <xsl:with-param name="pBasePath" select="$context-path"/>
                                            <xsl:with-param name="pAccession" select="ancestor::study/accession"/>
                                        </xsl:call-template>
                                    </div>
                                </xsl:for-each>
                                <xsl:if test="exists(file|files/file)">
                                    <div class="ae-section-files">
                                        <div class="ae-section-file-title">Section Files
                                            <span class="fa fa-expand fa-icon table-expander" title="Click to expand"/>
                                        </div>
                                        <xsl:call-template name="file-table">
                                            <xsl:with-param name="pQueryId" select="$queryid"/>
                                            <xsl:with-param name="pNodes" select="file|files/file"/>
                                            <xsl:with-param name="pBasePath" select="$context-path"/>
                                            <xsl:with-param name="pAccession" select="ancestor::study/accession"/>
                                        </xsl:call-template>
                                    </div>
                                </xsl:if>
                            </div>
                        </xsl:if>
                        <xsl:if test="fn:count(descendant::link)>0">
                            <div>
                                <a class="show-more toggle-links" data-total="{fn:count(descendant::link)}">show links in this section</a>
                                <xsl:for-each select="links/table">
                                    <div class="ae-section-links">
                                        <div class="ae-section-file-title">Link Table: <xsl:value-of select="position()"/>
                                            <span class="fa fa-expand fa-icon table-expander" title="Click to expand"/>
                                        </div>
                                        <xsl:call-template name="link-table">
                                            <xsl:with-param name="pQueryId" select="$queryid"/>
                                            <xsl:with-param name="pNodes" select="link"/>
                                        </xsl:call-template>
                                    </div>
                                </xsl:for-each>
                                <xsl:if test="exists(link|links/link)">
                                    <div class="ae-section-links">
                                        <div class="ae-section-file-title">Section Links
                                            <span class="fa fa-expand fa-icon table-expander" title="Click to expand"/>
                                        </div>
                                        <xsl:call-template name="link-table">
                                            <xsl:with-param name="pQueryId" select="$queryid"/>
                                            <xsl:with-param name="pNodes" select="link|links/link"/>
                                        </xsl:call-template>
                                    </div>
                                </xsl:if>
                            </div>
                        </xsl:if>

                        <xsl:if test="fn:count(table)>0">
                            <div>
                                <a class="show-more toggle-tables"  data-total="{fn:count(table)}">show tables in this section</a>
                                <div class="ae-section-tables">
                                    <xsl:for-each select="table">
                                        <div class="ae-section-file-title">
                                            Table: <xsl:value-of select="section[1]/@type"/>
                                            <span class="fa fa-expand fa-icon table-expander" title="Click to expand"/>
                                        </div>
                                        <xsl:call-template name="subsection-table">
                                            <xsl:with-param name="pQueryId" select="$queryid"/>
                                            <xsl:with-param name="pNodes" select="."/>
                                        </xsl:call-template>
                                    </xsl:for-each>
                                </div>
                            </div>
                        </xsl:if>

                    </div> <!--end content-->
                </xsl:with-param>
                <xsl:with-param name="pClass" select="('left')"/>
            </xsl:call-template>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="study-publications">
        <xsl:param name="pQueryId"/>
        <xsl:param name="pNodes"/>
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
                                        <xsl:if test="fn:exists(attribute[fn:lower-case(@name)='journal'])">
                                            <xsl:text> [</xsl:text>
                                        </xsl:if>
                                        <xsl:value-of select="attribute[fn:lower-case(@name)='publication date']"/>
                                        <xsl:if test="fn:exists(attribute[fn:lower-case(@name)='volume'])">
                                            <xsl:text>, </xsl:text>
                                            <xsl:value-of select="attribute[fn:lower-case(@name)='volume']"/>
                                        </xsl:if>
                                        <xsl:if test="fn:exists(attribute[fn:lower-case(@name)='pages'])">
                                            <xsl:text>:</xsl:text>
                                            <xsl:value-of select="attribute[fn:lower-case(@name)='pages']"/>
                                        </xsl:if>
                                        <xsl:if test="fn:exists(attribute[fn:lower-case(@name)='journal'])">
                                            <xsl:text>]</xsl:text>
                                        </xsl:if>
                                    </xsl:with-param>
                                </xsl:call-template>
                                <xsl:if test="exists(attribute[fn:lower-case(@name)='pmcid'])">
                                     <xsl:value-of select="concat(
                                        if (fn:exists(attribute[fn:lower-case(@name)='journal']))
                                              then '&#160;(' else '','PMCID:')"/>
                                    <a href="http://europepmc.org/articles/{attribute[fn:lower-case(@name)='pmcid']}" target="_blank">
                                        <xsl:call-template name="highlight">
                                            <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                            <xsl:with-param name="pCallHighlightingFunction" select="true()"/>
                                            <xsl:with-param name="pText" select="attribute[fn:lower-case(@name)='pmcid']"/>
                                        </xsl:call-template>
                                    </a>
                                    <xsl:text>)</xsl:text>
                                </xsl:if>
                                <xsl:if test="exists(attribute[fn:lower-case(@name)='pmid'])">
                                    <xsl:value-of select="concat(
                                        if (fn:exists(attribute[fn:lower-case(@name)='journal']))
                                              then '&#160;(' else '','PMID:')"/>
                                    <a href="http://europepmc.org/abstract/MED/{attribute[fn:lower-case(@name)='pmid']}" target="_blank">
                                        <xsl:call-template name="highlight">
                                            <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                            <xsl:with-param name="pCallHighlightingFunction" select="true()"/>
                                            <xsl:with-param name="pText" select="attribute[fn:lower-case(@name)='pmid']"/>
                                        </xsl:call-template>
                                    </a>
                                    <xsl:text>)</xsl:text>
                                </xsl:if>
                                <xsl:if test="exists(attribute[fn:lower-case(@name)='doi'])">
                                    <xsl:value-of select="concat(
                                        if (fn:exists(attribute[fn:lower-case(@name)='journal']))
                                              then '&#160;(' else '','doi:')"/>
                                    <a href="http://dx.doi.org/{attribute[fn:lower-case(@name)='doi']}" target="_blank">
                                        <xsl:call-template name="highlight">
                                            <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                            <xsl:with-param name="pCallHighlightingFunction" select="true()"/>
                                            <xsl:with-param name="pText" select="attribute[fn:lower-case(@name)='doi']"/>
                                        </xsl:call-template>
                                    </a>
                                    <xsl:text>)</xsl:text>
                                </xsl:if>
                            </xsl:for-each>
                            <xsl:if test="fn:count(.//file)>0">
                                <br/>
                                <a class="show-more toggle-files">show files in this section</a>
                                <div class="ae-section-files">
                                    <xsl:call-template name="file-table">
                                        <xsl:with-param name="pQueryId" select="$queryid"/>
                                        <xsl:with-param name="pNodes" select=".//file"/>
                                        <xsl:with-param name="pBasePath" select="$context-path"/>
                                        <xsl:with-param name="pAccession" select="ancestor::study/accession"/>
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
        <xsl:param name="pBasePath"/>
        <xsl:param name="pAccession"/>
        <xsl:if test="fn:count($pNodes)>0">
            <xsl:call-template name="widget">
                <xsl:with-param name="pTitleClass" select="'ae-detail-files-title'"/>
                <xsl:with-param name="pIconClass" select="'icon icon-functional padded-gray-icon'"/>
                <xsl:with-param name="pIconType" select="'='"/>
                <xsl:with-param name="pName" select="'Download data files'"/>
                <xsl:with-param name="pContent">
                    <div class="list-loader" >Loading...</div>
                    <div class="list-content">
                        <xsl:call-template name="file-table">
                            <xsl:with-param name="pNodes" select="$pNodes"/>
                            <xsl:with-param name="pQueryId" select="$pQueryId"/>
                            <xsl:with-param name="pBasePath" select="$pBasePath"/>
                            <xsl:with-param name="elementId" select="'file-list'"/>
                            <xsl:with-param name="pAccession" select="$pAccession"/>
                        </xsl:call-template>
                        <span id="selected-file-text"/> <a id="download-selected-files">Download all</a><br/><br/>
                    </div>
                </xsl:with-param>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <xsl:template name="file-table"><xsl:param name="pQueryId"/>
        <xsl:param name="pNodes"/>
        <xsl:param name="pBasePath"/>
        <xsl:param name="elementId" select="fn:concat('file-table-',../position())"/>
        <xsl:param name="pAccession"/>

        <xsl:variable name="vColumns" select="distinct-values($pNodes/attribute[@name!='Type']/@name)"/>
        <div class="table-wrapper file-table-wrapper">
        <table class="stripe compact hover file-list" style=" width: 100% " cellspacing="0" id="{$elementId}" >
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
                    <xsl:variable name="vPath" select="@path"/>
                    <xsl:variable name="isImage" select="matches(lower-case(tokenize($vName, '\.')[last()]),'bmp|jpg|wbmp|jpeg|png|gif|tif|tiff|pdf|docx|txt|csv|html|htm')"/>
                    <xsl:variable name="vSectionAcc" select="../@acc"/>
                    <xsl:variable name="hasAttributes" select="fn:exists(../@acc)"/>
                    <tr>
                        <xsl:if test="$elementId='file-list'">
                            <td class="disable-select file-check-box">
                                <input class="text-bottom" type="checkbox" data-name="{$vPath}"/>
                            </td>
                        </xsl:if>
                        <td class="file-list-file-name">
                            <a href="{$pBasePath}/files/{$pAccession}/{$vPath}" title="{$vName}" target="_blank">
                                <xsl:call-template name="highlight">
                                    <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                    <xsl:with-param name="pText" select="$vName"/>
                                    <xsl:with-param name="pCallHighlightingFunction" select="true()"/>
                                </xsl:call-template>
                            </a>
                            <xsl:if test="$hasAttributes"><span title="Show file details" class="attributes-icon fa fa-table" data-section-id="{$vSectionAcc}"></span></xsl:if>
                            <xsl:if test="$isImage">
                                <a href="{$pBasePath}/files/{$pAccession}/{$vPath}"  target="_blank"
                                   data-name="{$vName}" class="file-link">
                                    <xsl:attribute name="data-thumbnail"
                                                   select="concat($pBasePath,'/thumbnail/',$pAccession,'/',$vPath)"/>
                                    <i class="fa fa-file-image-o"></i><span  class="thumbnail-image"/>
                                    <img/>
                                </a>
                            </xsl:if>
                        </td>
                        <td class="align-right" data-order="{@size}">
                            <xsl:choose>
                                <xsl:when test="lower-case(@type)='directory'">
                                    <i class="fa fa-folder"></i>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:call-template name="file-size">
                                        <xsl:with-param name="size" select="@size"/>
                                    </xsl:call-template>
                                </xsl:otherwise>
                            </xsl:choose>
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
        </div>
    </xsl:template>
    <xsl:template name="file-size">
        <xsl:param name="size"/>
        <span class="ae-file-size">
            <xsl:choose>
                <xsl:when test="$size=''">
                </xsl:when>
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
    <xsl:param name="pBasePath"/>
    <xsl:param name="pLinks"/>
        <xsl:if test="fn:count($pLinks)>0">
            <xsl:variable name="totalTables" select="count(fn:distinct-values($pLinks/string-join(attribute/@name,' | ')))"/>
            <xsl:for-each-group select="$pLinks" group-by="string-join(attribute/@name,' | ')">
                <xsl:variable name="tableNumber" select="position()"/>
                <xsl:call-template name="widget">
                    <xsl:with-param name="pName" select="concat('Linked information', if ($totalTables>1) then concat(': Table ',$tableNumber) else '')"/>
                    <xsl:with-param name="pTitleClass" select="'ae-detail-links-title'"/>
                    <xsl:with-param name="pIconClass" select="'icon icon-generic padded-gray-icon'"/>
                    <xsl:with-param name="pIconType" select="'x'"/>
                    <xsl:with-param name="pClass" select="('left')"/>
                    <xsl:with-param name="pContent">
                        <div class="list-loader" >Loading...</div>
                        <div class="list-content">
                            <div class="link-filters">
                                Type Filter:
                                <xsl:for-each-group select="current-group()" group-by="ae:getTitleFor(attribute[@name='Type']/value)">
                                        <input type="checkbox" class="link-filter do-not-clear" checked="checked" id="{current-grouping-key()}" data-position="{$tableNumber}" />
                                        <label class="link-filter-label no-select" for="{current-grouping-key()}"><span class="checkmark"><xsl:value-of select="current-grouping-key()"/></span></label>
                                </xsl:for-each-group>
                            </div>
                            <xsl:call-template name="link-table">
                                <xsl:with-param name="pNodes" select="current-group()"/>
                                <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                <xsl:with-param name="pClass" select="('link-widget')"/>
                                <xsl:with-param name="elementId" select="concat('link-list-',position())"/>
                            </xsl:call-template>
                        </div>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:for-each-group>
        </xsl:if>
    </xsl:template>

    <xsl:template name="link-table">
        <xsl:param name="pQueryId"/>
        <xsl:param name="pNodes"/>
        <xsl:param name="elementId" select="generate-id()"/>
        <xsl:param name="pClass" select="''"/>
        <xsl:variable name="vColumns" select="distinct-values($pNodes/attribute[fn:lower-case(@name)!='type' and fn:lower-case(@name)!='description']/@name)"/>
        <div class="table-wrapper">
        <table class="stripe compact hover link-list {$pClass}" cellspacing="0" width="100%" id="{$elementId}" >
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
        </div>
    </xsl:template>

    <xsl:template name="subsection-table">
        <xsl:param name="pQueryId"/>
        <xsl:param name="pNodes"/>
        <xsl:variable name="vColumns" select="distinct-values($pNodes//attribute/name)"/>
        <div class="table-wrapper">
            <table class="stripe compact hover section-table" style=" width: 100% " cellspacing="0"  >
                <thead>
                    <xsl:for-each select="$vColumns">
                        <th><xsl:value-of select="."/></th>
                    </xsl:for-each>
                </thead>
                <xsl:for-each select="$pNodes/section">
                    <xsl:variable name="pRow" select="."/>
                    <tr>
                        <xsl:for-each select="$vColumns">
                            <xsl:variable name="vColumn" select="."/>
                            <td><xsl:value-of select="$pRow/attributes/attribute[name=$vColumn]/value"/></td>
                        </xsl:for-each>
                    </tr>
                </xsl:for-each>
            </table>
        </div>
    </xsl:template>


    <xsl:template name="study-funding">
        <xsl:param name="pQueryId"/>
        <xsl:param name="pNodes"/>
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
                            <xsl:with-param name="pAccession" select="ancestor::study/accession"/>
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
        <xsl:param name="pId" select="''"/>
        <xsl:param name="pName" select="''"/>
        <xsl:param name="pContent"/>
        <xsl:param name="pClass" as="xs:string*" select="''"/>
        <xsl:if test="fn:exists($pName) and fn:not(fn:matches(fn:string-join($pContent//text(), ''), '^\s*$'))">
             <div id="{$pId}" class="ae-detail-name"><xsl:value-of select="$pName"/></div>
        </xsl:if>
        <xsl:if test="fn:exists($pContent) and fn:not(fn:matches(fn:string-join($pContent//text(), ''), '^\s*$'))">
            <div>
                <xsl:attribute name="class" select="fn:string-join((('value'),$pClass), ' ')"/>
                <xsl:copy-of select="$pContent"/>
            </div>
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
                <a href="{$vUrl}" target="_blank" title="{$vUrl}">
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
            <xsl:when test="$type=''">
                <xsl:value-of select="'External'"/>
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
            <xsl:when test="$type = 'arrayexpress files'">
                <xsl:value-of select="fn:concat('http://www.ebi.ac.uk/arrayexpress/experiments/', $pId,'/files/')"/>
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
            <xsl:when test="$type = 'geo'">
                <xsl:value-of select="fn:concat('http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=', $pId)"/>
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
                <xsl:value-of select="fn:concat('http://www.ebi.ac.uk/ega/datasets/', $pId)"/>
            </xsl:when>
            <xsl:when test="fn:starts-with($pUrl,'http:') or fn:starts-with($pUrl,'https:') or fn:starts-with($pUrl,'ftp:')">
                <xsl:value-of select="$pUrl"/>
            </xsl:when>
        </xsl:choose>
    </xsl:function>

    <xsl:template name="study-suggestion">
        <xsl:call-template name="widget">
            <xsl:with-param name="pName" select="'Similar Studies'"/>
            <xsl:with-param name="pTitleClass" select="'ae-detail-links-title'"/>
            <xsl:with-param name="pIconClass" select="'icon icon-functional padded-gray-icon'"/>
            <xsl:with-param name="pIconType" select="'O'"/>
            <xsl:with-param name="pClass" select="('left')"/>
            <xsl:with-param name="pContent">
                <xsl:variable name="accessions" select="search:getQueryInfoParameter($queryid,'similarAccessions')"/>
                <xsl:variable name="titles" select="search:getQueryInfoParameter($queryid,'similarTitles')"/>
                <ul class="recommendations">
                    <xsl:for-each select="$accessions">
                        <xsl:variable name="index" select="position()"/>
                        <xsl:variable name="accession" select="."/>
                        <li class="browse-study-title">
                            <a href="{$context-path}/studies/{$accession}/{$vQueryString}"><xsl:value-of select="$titles[$index]"/></a>
                            <span class="browse-study-accession">
                                <xsl:value-of select="$accession"/>
                            </span>
                        </li>
                    </xsl:for-each>
                </ul>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
</xsl:stylesheet>