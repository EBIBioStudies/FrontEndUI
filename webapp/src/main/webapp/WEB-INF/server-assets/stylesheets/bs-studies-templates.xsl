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
                <xsl:call-template name="section">
                    <xsl:with-param name="pName" select="'Status'"/>
                    <xsl:with-param name="pContent">
                        <xsl:for-each select="$vDates">
                            <xsl:sort select="fn:translate(text(),'-','')" data-type="number"/>
                            <xsl:sort select="fn:translate(fn:substring(fn:name(), 1, 1), 'slr', 'abc')"/>

                            <xsl:variable name="vLabel">
                                <xsl:if test="ae:isFutureDate(text())">will be</xsl:if>
                                <xsl:choose>
                                    <xsl:when test="fn:name() = 'submissiondate'">submitted</xsl:when>
                                    <xsl:when test="fn:name() = 'lastupdatedate'">
                                        <xsl:if test="not(ae:isFutureDate(text()))">last</xsl:if>
                                        <xsl:text>updated</xsl:text>
                                    </xsl:when>
                                    <xsl:otherwise>released</xsl:otherwise>
                                </xsl:choose>
                            </xsl:variable>
                            <xsl:choose>
                                <xsl:when test="fn:position() = 1">
                                    <xsl:value-of select="fn:upper-case(fn:substring($vLabel, 1, 1))"/>
                                    <xsl:value-of select="fn:substring($vLabel, 2)"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:text>, </xsl:text>
                                    <xsl:value-of select="$vLabel"/>
                                </xsl:otherwise>
                            </xsl:choose>
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="ae:formatDate(text())"/>
                            <!--
                            <xsl:if test="(fn:name() = 'releasedate') and $pIsPrivate"> (<a href="/fg/acext?acc={$vAccession}">change release date</a>&#160;<span class="new">new!</span>)</xsl:if>
                            -->
                        </xsl:for-each>
                    </xsl:with-param>
                    <xsl:with-param name="pClass" select="('left')"/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template name="study-attributes">
        <xsl:param name="pQueryId"/>
        <xsl:param name="pNodes"/>
        <xsl:for-each-group select="$pNodes" group-by="fn:lower-case(@name)">
            <xsl:variable name="vIsLinkedInfo" select="fn:current-grouping-key() = 'linked information'"/>
            <xsl:call-template name="section">
                <xsl:with-param name="pName" select="fn:current-group()[1]/@name"/>
                <xsl:with-param name="pContent">
                    <xsl:choose>
                        <xsl:when test="$vIsLinkedInfo">
                            <xsl:for-each-group select="fn:current-group()" group-by="type">
                                <xsl:value-of select="ae:getTitleFor(type)"/>
                                <xsl:text>: </xsl:text>
                                <xsl:call-template name="highlighted-list">
                                    <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                    <xsl:with-param name="pType" select="type"/>
                                    <xsl:with-param name="pList"
                                                    select="fn:current-group()/value"/>
                                </xsl:call-template>
                                <xsl:if test="fn:position() != fn:last()">
                                    <br/>
                                </xsl:if>
                            </xsl:for-each-group>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:for-each select="fn:current-group()">
                                <xsl:call-template name="highlight">
                                    <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                    <xsl:with-param name="pText" select="value"/>
                                </xsl:call-template>
                                <xsl:if test="fn:position() != fn:last()">
                                    <br/>
                                </xsl:if>
                            </xsl:for-each>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:with-param>
                <xsl:with-param name="pClass" select="if ($vIsLinkedInfo) then ('left') else ('justify')"/>
            </xsl:call-template>
        </xsl:for-each-group>
    </xsl:template>
    <xsl:template name="study-sections">
        <xsl:param name="pQueryId"/>
        <xsl:param name="pTitle"/>
        <xsl:param name="pNodes"/>
        <xsl:for-each-group select="$pNodes" group-by="fn:lower-case(@type)">
            <xsl:choose>
                <xsl:when test="fn:current-grouping-key()='publication'">
                    <xsl:call-template name="section">
                        <xsl:with-param name="pName" select="@type"/>
                        <xsl:with-param name="pContent">
                            <xsl:for-each select="fn:current-group()">
                                <xsl:call-template name="highlight">
                                    <xsl:with-param name="pQueryId" select="$pQueryId"/>
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
                                        <xsl:with-param name="pText" select="attribute[fn:lower-case(@name)='pmcid']"/>
                                    </xsl:call-template>
                                </a>
                                <xsl:text>)</xsl:text>
                            </xsl:for-each>
                        </xsl:with-param>
                        <xsl:with-param name="pClass" select="('left')"/>
                    </xsl:call-template>
                </xsl:when>
                <xsl:when test="fn:current-grouping-key()='author'">
                    <xsl:call-template name="section">
                        <xsl:with-param name="pName"
                                        select="fn:concat('Author', (if (fn:count(fn:current-group())>1) then 's' else ''))"/>
                        <xsl:with-param name="pContent">
                            <xsl:variable name="vOrgRefs">
                                <orgs>
                                    <xsl:for-each
                                            select="fn:current-group()/attribute[fn:lower-case(@name)='affiliation']/value">
                                        <org id="{fn:current()}">
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
                                    <xsl:with-param name="pText"
                                                    select="attribute[fn:lower-case(@name)='name']/value"/>
                                </xsl:call-template>
                                <xsl:variable name="vAffiliationId"
                                              select="attribute[fn:lower-case(@name)='affiliation']/value"/>
                                <xsl:variable name="vAffiliation" select="$vUniqueRefs/orgs/org[@acc=$vAffiliationId]"/>
                                <xsl:if test="fn:count($vUniqueRefs/orgs/org) > 1 and $vAffiliation">
                                    <sup>
                                        <xsl:value-of select="fn:count($vAffiliation/preceding-sibling::org) + 1"/>
                                    </sup>
                                </xsl:if>
                                <xsl:if test="fn:position() != fn:last()">
                                    <xsl:text>, </xsl:text>
                                </xsl:if>
                            </xsl:for-each>
                            <p class="orgs">
                                <xsl:for-each select="$vUniqueRefs/orgs/org">
                                    <xsl:if test="fn:count($vUniqueRefs/orgs/org) > 1">
                                        <sup>
                                            <xsl:value-of select="position()"/>
                                        </sup>
                                    </xsl:if>
                                    <xsl:call-template name="highlight">
                                        <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                        <xsl:with-param name="pText"
                                                        select="."/>
                                    </xsl:call-template>
                                    <xsl:if test="fn:position() != fn:last()">
                                        <xsl:text>, </xsl:text>
                                    </xsl:if>
                                </xsl:for-each>
                            </p>
                        </xsl:with-param>
                        <xsl:with-param name="pClass" select="('left')"/>
                    </xsl:call-template>
                </xsl:when>
                <xsl:otherwise/>
            </xsl:choose>
        </xsl:for-each-group>
    </xsl:template>
    <xsl:template name="study-files">
        <xsl:param name="pQueryId"/>
        <xsl:param name="pNodes"/>
        <xsl:param name="pFiles"/>
        <xsl:param name="pBasePath"/>
        <xsl:call-template name="section">
            <xsl:with-param name="pName" select="'Files'"/>
            <xsl:with-param name="pContent">
                <xsl:for-each select="$pNodes">
                    <xsl:variable name="vName" select="@name"/>
                    <xsl:variable name="vFile" select="$pFiles/file[@name=$vName]"/>
                    <xsl:if test="$vFile">
                        <a href="{$pBasePath}/files/{$pFiles/@accession}/{$vName}">
                            <xsl:call-template name="highlight">
                                <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                <xsl:with-param name="pText" select="$vName"/>
                            </xsl:call-template>
                        </a>
                    </xsl:if>
                    <xsl:if test="fn:position() != fn:last()">
                        <br/>
                    </xsl:if>
                </xsl:for-each>
            </xsl:with-param>
            <xsl:with-param name="pClass" select="('left')"/>
        </xsl:call-template>
    </xsl:template>
    <xsl:template name="study-links">
        <xsl:param name="pQueryId"/>
        <xsl:param name="pNodes"/>
        <xsl:call-template name="section">
            <xsl:with-param name="pName" select="'Links'"/>
            <xsl:with-param name="pContent">
                <xsl:for-each select="$pNodes">
                    <a href="{@url}" target="_blank">
                        <xsl:call-template name="highlight">
                            <xsl:with-param name="pQueryId" select="$pQueryId"/>
                            <xsl:with-param name="pText" select="@url"/>
                        </xsl:call-template>
                    </a>
                    <xsl:if test="fn:position() != fn:last()">
                        <br/>
                    </xsl:if>
                </xsl:for-each>
            </xsl:with-param>
            <xsl:with-param name="pClass" select="('left')"/>
        </xsl:call-template>
    </xsl:template>
    <!--
    <xsl:template name="exp-samples-section">
        <xsl:param name="pQueryString"/>
        <xsl:param name="pQueryId"/>
        <xsl:param name="pBasePath"/>
        <xsl:param name="pFiles"/>

        <xsl:if test="$pFiles/file[@extension = 'txt' and@kind = 'sdrf']">
            <tr>
                <td class="name"><div>Samples (<xsl:value-of select="samples"/>)</div></td>
                <td class="value">
                    <div>
                        <a class="samples" href="{$pBasePath}/experiments/{accession}/samples/{$pQueryString}">
                            <span class="sample-view"><xsl:text>Click for detailed sample information and links to data</xsl:text></span>
                            <br/>
                            <xsl:variable name="vPossibleMatches">
                                <xsl:for-each select="experimentalfactor/name">
                                    <match text="{fn:lower-case(.)}">
                                        <xsl:call-template name="highlight">
                                            <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                            <xsl:with-param name="pText" select="."/>
                                            <xsl:with-param name="pFieldName" select="'ef'"/>
                                        </xsl:call-template>
                                    </match>
                                </xsl:for-each>
                                <xsl:for-each select="experimentalfactor/value">
                                    <match text="{fn:lower-case(.)}">
                                        <xsl:call-template name="highlight">
                                            <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                            <xsl:with-param name="pText" select="."/>
                                            <xsl:with-param name="pFieldName" select="'efv'"/>
                                        </xsl:call-template>
                                    </match>
                                </xsl:for-each>
                                <xsl:for-each select="sampleattribute/category | sampleattribute/value">
                                    <match text="{fn:lower-case(.)}">
                                        <xsl:call-template name="highlight">
                                            <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                            <xsl:with-param name="pText" select="."/>
                                            <xsl:with-param name="pFieldName" select="'sa'"/>
                                        </xsl:call-template>
                                    </match>
                                </xsl:for-each>
                            </xsl:variable>
                            <xsl:variable name="vMatches" select="$vPossibleMatches/match[span]"/>
                            <xsl:if test="$vMatches">
                                <em><xsl:text>&#160;&#x2514;&#x2500;&#160;found inside: </xsl:text></em>
                                <xsl:for-each-group select="$vMatches[fn:position() &lt;= 20]" group-by="@text">
                                    <xsl:sort select="@text" order="ascending"/>
                                    
                                    <xsl:copy-of select="fn:current-group()[1]/node()"/>
                                    <xsl:if test="fn:position() != fn:last()">
                                        <xsl:text>, </xsl:text>
                                    </xsl:if>
                                </xsl:for-each-group>
                                <xsl:if test="fn:count($vMatches) > 20">
                                    <xsl:text>, ...</xsl:text>
                                </xsl:if>
                            </xsl:if>
                        </a>
                    </div>
                </td>
            </tr>
        </xsl:if>
    </xsl:template>

    <xsl:template name="exp-experimental-factors-section">
        <xsl:param name="pQueryId"/>

        <xsl:if test="experimentalfactor/name">
            <tr>
                <td class="name"><div>Experimental factors</div></td>
                <td class="value"><div>
                    <table cellpadding="0" cellspacing="0" border="0">
                        <thead>
                            <tr>
                                <th class="name">Factor name</th>
                                <th class="value">Factor values</th>
                            </tr>
                        </thead>
                        <tbody>
                            <xsl:for-each select="experimentalfactor">
                                <tr>
                                    <td class="name">
                                        <xsl:call-template name="highlight">
                                            <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                            <xsl:with-param name="pText" select="name"/>
                                            <xsl:with-param name="pFieldName" select="'ef'"/>
                                        </xsl:call-template>
                                    </td>
                                    <td class="value">
                                        <xsl:call-template name="highlight">
                                            <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                            <xsl:with-param name="pText" select="string-join(value, ', ')"/>
                                            <xsl:with-param name="pFieldName" select="'efv'"/>
                                        </xsl:call-template>
                                    </td>
                                </tr>
                            </xsl:for-each>
                        </tbody>
                    </table></div>
                </td>
            </tr>
        </xsl:if>
    </xsl:template>

    <xsl:template name="exp-sample-attributes-section">
        <xsl:param name="pQueryId"/>

        <xsl:if test="sampleattribute/category">
            <tr>
                <td class="name"><div>Sample attributes</div></td>
                <td class="value"><div>
                    <table cellpadding="0" cellspacing="0" border="0">
                        <thead>
                            <tr>
                                <th class="name">Attribute name</th>
                                <th class="value">Attribute values</th>
                            </tr>
                        </thead>
                        <tbody>
                            <xsl:for-each select="sampleattribute">
                                <tr>
                                    <td class="name">
                                        <xsl:call-template name="highlight">
                                            <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                            <xsl:with-param name="pText" select="category"/>
                                            <xsl:with-param name="pFieldName"/>
                                        </xsl:call-template>
                                    </td>
                                    <td class="value">
                                        <xsl:call-template name="highlight">
                                            <xsl:with-param name="pQueryId" select="$pQueryId"/>
                                            <xsl:with-param name="pText" select="string-join(value, ', ')"/>
                                            <xsl:with-param name="pFieldName" select="'sa'"/>
                                        </xsl:call-template>
                                    </td>
                                </tr>
                            </xsl:for-each>
                        </tbody>
                    </table></div>
                </td>
            </tr>
        </xsl:if>
    </xsl:template>
    -->

    <xsl:template name="section">
        <xsl:param name="pName"/>
        <xsl:param name="pContent"/>
        <xsl:param name="pClass" as="xs:string*"/>
        <xsl:if test="fn:exists($pName) and fn:not(fn:matches(fn:string-join($pContent//text(), ''), '^\s*$'))">
            <tr>
                <td class="name">
                    <div><xsl:value-of select="$pName"/></div>
                </td>
                <td>
                    <xsl:attribute name="class" select="fn:string-join((('value'),$pClass), ' ')"/>
                    <xsl:copy-of select="$pContent"/>
                </td>
            </tr>
        </xsl:if>
    </xsl:template>

    <xsl:template name="highlighted-list">
        <xsl:param name="pQueryId"/>
        <xsl:param name="pList"/>
        <xsl:param name="pType"/>

        <xsl:variable name="vSize" select="fn:count($pList)"/>

        <xsl:for-each select="$pList[fn:position() = (1 to 20)]">
            <xsl:call-template name="highlight-reference">
                <xsl:with-param name="pQueryId" select="$pQueryId"/>
                <xsl:with-param name="pText"
                                select="text()"/>
                <xsl:with-param name="pType" select="$pType"/>
            </xsl:call-template>
            <xsl:if test="fn:position() != fn:last() or $vSize &gt; 20">
                <xsl:text>, </xsl:text>
            </xsl:if>
        </xsl:for-each>
        <xsl:if test="$vSize &gt; 20">
            <span class="hidden-values" size="{$vSize}">
                <xsl:for-each select="$pList[fn:position() = (21 to $vSize)]">
                    <xsl:call-template name="highlight-reference">
                        <xsl:with-param name="pQueryId" select="$pQueryId"/>
                        <xsl:with-param name="pText"
                                        select="text()"/>
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
        <xsl:variable name="vUrl" select="ae:getUrlFor($pType, $pText)"/>
        <xsl:choose>
            <xsl:when test="$vUrl != ''">
                <a href="{$vUrl}" target="_blank">
                    <xsl:call-template name="highlight">
                        <xsl:with-param name="pQueryId" select="$pQueryId"/>
                        <xsl:with-param name="pText"
                                        select="text()"/>
                    </xsl:call-template>
                </a>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="highlight">
                    <xsl:with-param name="pQueryId" select="$pQueryId"/>
                    <xsl:with-param name="pText"
                                    select="text()"/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:function name="ae:getTitleFor">
        <xsl:param name="pType"/>
        <xsl:choose>
            <xsl:when test="fn:lower-case($pType) = 'sprot'">
                <xsl:value-of select="'UniProt'"/>
            </xsl:when>
            <xsl:when test="fn:lower-case($pType) = 'gen'">
                <xsl:value-of select="'ENA'"/>
            </xsl:when>
            <xsl:when test="fn:lower-case($pType) = 'arrayexpress'">
                <xsl:value-of select="'ArrayExpress'"/>
            </xsl:when>
            <xsl:when test="fn:lower-case($pType) = 'refsnp'">
                <xsl:value-of select="'dbSNP'"/>
            </xsl:when>
            <xsl:when test="fn:lower-case($pType) = 'pdb'">
                <xsl:value-of select="'PDBe'"/>
            </xsl:when>
            <xsl:when test="fn:lower-case($pType) = 'pfam'">
                <xsl:value-of select="'Pfam'"/>
            </xsl:when>
            <xsl:when test="fn:lower-case($pType) = 'omim'">
                <xsl:value-of select="'OMIM'"/>
            </xsl:when>
            <xsl:when test="fn:lower-case($pType) = 'interpro'">
                <xsl:value-of select="'InterPro'"/>
            </xsl:when>
            <xsl:when test="fn:lower-case($pType) = 'refseq'">
                <xsl:value-of select="'Nucleotide'"/>
            </xsl:when>
            <xsl:when test="fn:lower-case($pType) = 'ensembl'">
                <xsl:value-of select="'Ensembl'"/>
            </xsl:when>
            <xsl:when test="fn:lower-case($pType) = 'doi'">
                <xsl:value-of select="'DOI'"/>
            </xsl:when>
            <xsl:when test="fn:lower-case($pType) = 'intact'">
                <xsl:value-of select="'IntAct'"/>
            </xsl:when>
            <xsl:when test="fn:lower-case($pType) = 'chebi'">
                <xsl:value-of select="'ChEBI'"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$pType"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="ae:getUrlFor">
        <xsl:param name="pType"/>
        <xsl:param name="pId"/>
        <xsl:choose>
            <xsl:when test="fn:lower-case($pType) = 'sprot'">
                <xsl:value-of select="fn:concat('http://www.uniprot.org/uniprot/', $pId)"/>
            </xsl:when>
            <xsl:when test="fn:lower-case($pType) = 'gen'">
                <xsl:value-of select="fn:concat('http://www.ebi.ac.uk/ena/data/view/', $pId)"/>
            </xsl:when>
            <xsl:when test="fn:lower-case($pType) = 'arrayexpress'">
                <xsl:value-of select="fn:concat('http://www.ebi.ac.uk/arrayexpress/experiments/', $pId)"/>
            </xsl:when>
            <xsl:when test="fn:lower-case($pType) = 'refsnp'">
                <xsl:value-of select="fn:concat('http://www.ncbi.nlm.nih.gov/SNP/snp_ref.cgi?rs=', $pId)"/>
            </xsl:when>
            <xsl:when test="fn:lower-case($pType) = 'pdb'">
                <xsl:value-of select="fn:concat('http://www.ebi.ac.uk/pdbe-srv/view/entry/', $pId, '/summary')"/>
            </xsl:when>
            <xsl:when test="fn:lower-case($pType) = 'pfam'">
                <xsl:value-of select="fn:concat('http://pfam.xfam.org/family/', $pId)"/>
            </xsl:when>
            <xsl:when test="fn:lower-case($pType) = 'omim'">
                <xsl:value-of select="fn:concat('http://omim.org/entry/', $pId)"/>
            </xsl:when>
            <xsl:when test="fn:lower-case($pType) = 'interpro'">
                <xsl:value-of select="fn:concat('http://www.ebi.ac.uk/interpro/entry/', $pId)"/>
            </xsl:when>
            <xsl:when test="fn:lower-case($pType) = 'refseq'">
                <xsl:value-of select="fn:concat('http://www.ncbi.nlm.nih.gov/nuccore/', $pId)"/>
            </xsl:when>
            <xsl:when test="fn:lower-case($pType) = 'doi'">
                <xsl:value-of select="fn:concat('http://dx.doi.org/', $pId)"/>
            </xsl:when>
            <xsl:when test="fn:lower-case($pType) = 'intact'">
                <xsl:value-of select="fn:concat('http://www.ebi.ac.uk/intact/pages/details/details.xhtml?experimentAc=', $pId)"/>
            </xsl:when>
            <xsl:when test="fn:lower-case($pType) = 'chebi'">
                <xsl:value-of select="fn:concat('http://www.ebi.ac.uk/chebi/searchId.do?chebiId=', fn:replace($pId, '[:]', '%3A'))"/>
            </xsl:when>
        </xsl:choose>
    </xsl:function>
</xsl:stylesheet>