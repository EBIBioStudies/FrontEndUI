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
                extension-element-prefixes="ae fn search html xs"
                exclude-result-prefixes="ae fn search html xs"
                version="2.0">

    <xsl:param name="page"/>
    <xsl:param name="pagesize"/>
    <xsl:param name="sortby"/>
    <xsl:param name="sortorder"/>
    <xsl:param name="queryid"/>
    <xsl:param name="tooManyExpansionTerms"/>

    <xsl:include href="bs-html-page.xsl"/>
    <xsl:include href="bs-sort-studies.xsl"/>
    <xsl:include href="bs-studies-templates.xsl"/>

    <xsl:variable name="vSearchMode" select="$keywords != ''"/>
    <xsl:variable name="vQueryString" select="if ($query-string!='') then fn:concat('?', $query-string) else ''"/>
    <xsl:variable name="vUnrestrictedAccess" select="fn:not($userid)"/>
    <xsl:variable name="vTotal" select="xs:integer(search:getQueryInfoParameter($queryid,'total'))"/>
    <xsl:variable name="projectLink" select="if ($project!='') then concat('/',$project) else '' "/>
    <xsl:variable name="vSuggestions" select="search:getQueryInfoParameter($queryid,'suggestions')"/>

    <xsl:template match="/">
        <xsl:variable name="vTitle" select="if ($vSearchMode) then fn:concat('Studies matching &quot;', $keywords, '&quot;') else 'Studies'"/>
        <xsl:call-template name="bs-page">
            <xsl:with-param name="pIsSearchVisible" select="fn:true()"/>
            <xsl:with-param name="pEBISearchWidget" />
            <xsl:with-param name="pExtraSearchFields"/>
            <xsl:with-param name="pTitleTrail" select="$vTitle"/>
            <xsl:with-param name="pExtraCSS">
                <link rel="stylesheet" href="{$context-path}/assets/stylesheets/bs-studies-browse-1.0.150220.css" type="text/css"/>
                <link rel="stylesheet" href="{$context-path}/assets/stylesheets/bs-hecatos-browse-1.0.160204.css" type="text/css"/>
            </xsl:with-param>
            <xsl:with-param name="pBreadcrumbTrail"/>
            <!-- <xsl:if test="$vTotal > 0"><xsl:value-of select="$vTitle"/></xsl:if></xsl:with-param> -->
            <xsl:with-param name="pExtraJS">
                <script src="//www.ebi.ac.uk/web_guidelines/js/ebi-global-search-run.js" type="text/javascript"/>
                <script src="//www.ebi.ac.uk/web_guidelines/js/ebi-global-search.js" type="text/javascript"/>
                <script src="{$context-path}/assets/scripts/jquery.query-2.1.7m-ebi.js" type="text/javascript"/>
                <script src="{$context-path}/assets/scripts/jquery.bs-studies-browse-1.0.150220.js" type="text/javascript"/>
            </xsl:with-param>
            <xsl:with-param name="pExtraBodyClasses" select="if ($vTotal = 0) then 'noresults' else ''"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="bs-content-section">
        <xsl:variable name="vSortBy" select="if ($sortby) then $sortby else 'relevance'"/>
        <xsl:variable name="vSortOrder" select="if ($sortorder) then $sortorder else 'descending'"/>

        <xsl:variable name="vPage" select="if ($page and $page castable as xs:integer) then $page cast as xs:integer else 1" as="xs:integer"/>
        <xsl:variable name="vPageSize" select="if ($pagesize and $pagesize castable as xs:integer) then $pagesize cast as xs:integer else 25" as="xs:integer"/>

        <xsl:variable name="vFromNode"  select="search:getQueryInfoParameter($queryid,'from')" />
        <xsl:variable name="vFrom" as="xs:integer" select="if ($vFromNode) then xs:integer($vFromNode) else 0" />
        <xsl:variable name="vToNode"  select="search:getQueryInfoParameter($queryid,'to')" />
        <xsl:variable name="vTo" as="xs:integer" select="if ($vToNode) then xs:integer($vToNode) else 0" />
        <xsl:choose>
            <xsl:when test="not($vFromNode)">
                <xsl:call-template name="browse-error"/>
            </xsl:when>
            <xsl:when test="$vTotal&gt;0">
                <section class="grid_24 alpha omega browser">
                    <div id="ae-content">
                        <div id="ae-browse">
                            <div class="persist-area">
                                <div>
                                    <ul class="ae-studies-browse-list">
                                        <xsl:call-template name="study-table">
                                            <xsl:with-param name="pStudies" select=".//study"/>
                                            <xsl:with-param name="pFrom" select="$vFrom"/>
                                            <xsl:with-param name="pTo" select="$vTo"/>
                                            <xsl:with-param name="pPosition" select="position()-1+$vFrom"/>
                                        </xsl:call-template>
                                    </ul>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="grid_24 intro" id="secondary">
                        <p>
                        <xsl:if test="$vTotal>0">
                            <h5><a href="{$context-path}/hecatos/studies/?" title="Browse BioStudies"><span class="icon icon-functional home-icon" data-icon="1">
                                Browse <xsl:value-of select="$project-title"/></span>
                            </a></h5>
                        </xsl:if>
                        </p>
                    </div>
                </section>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="browse-no-results"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="study-table">
        <xsl:param name="pFrom"/>
        <xsl:param name="pTo"/>
        <xsl:param name="pPosition"/>
        <xsl:param name="pStudies"/>
        <xsl:variable name="pColumnAttribute" select="'Assay Technology Type'"/>
        <xsl:variable name="pRowAttribute" select="'Compound'"/>
        <xsl:variable name="vColumns" select="distinct-values($pStudies/attribute[lower-case(@name)=lower-case($pColumnAttribute)]/value/text()/normalize-space(.))"/>
        <xsl:variable name="vRows" select="distinct-values($pStudies/attribute[lower-case(@name)=lower-case($pRowAttribute)]/value/text()/normalize-space(.))"/>

        <table border="1">
            <tr>
                <th><xsl:value-of select="$pRowAttribute"/> <i class="project-table-arrow fa fa-long-arrow-down"></i> &#x2572; <xsl:value-of select="$pColumnAttribute"/> <i class="project-table-arrow fa fa-long-arrow-right"></i></th>
                <xsl:for-each select="$vColumns">
                    <th><xsl:value-of select="."/></th>
                </xsl:for-each>
            </tr>
            <xsl:for-each select="$vRows">
                <xsl:variable name="pCurrentRow" select="."/>
                <tr>
                    <td><xsl:value-of select="$pCurrentRow"/></td>
                    <xsl:for-each select="$vColumns">
                        <xsl:variable name="pCurrentCol" select="."/>
                        <xsl:variable name="pMatchedStudy" select="$pStudies[./attribute[lower-case(@name)=fn:lower-case($pColumnAttribute)]/value/text()/lower-case(.)=lower-case($pCurrentCol)][./attribute[lower-case(@name)=fn:lower-case($pRowAttribute)]/value/text()/lower-case(.)= lower-case($pCurrentRow)]"/>
                        <td><ul>
                            <xsl:for-each select="$pMatchedStudy">
                                <xsl:call-template name="study">
                                    <xsl:with-param name="pFrom" select="$pFrom"/>
                                    <xsl:with-param name="pTo" select="$pTo"/>
                                    <xsl:with-param name="pPosition" select="$pPosition"/>
                                </xsl:call-template>
                            </xsl:for-each>
                        </ul></td>
                    </xsl:for-each>
                </tr>
            </xsl:for-each>
        </table>
    </xsl:template>

    <xsl:template name="study">
        <xsl:param name="pFrom"/>
        <xsl:param name="pTo"/>
        <xsl:param name="pPosition"/>
        <xsl:variable name="titleLink" select="if (fn:lower-case(@type)='project') then fn:concat($context-path,'/',accession,'/studies/') else fn:concat($context-path,$projectLink,'/studies/',accession,'/',$vQueryString)"/>
        <xsl:variable name="isPublic" select="@releaseTime!=9999999999 and contains(concat(' ',lower-case(access),' '),' public ')"/>
        <xsl:if test="not($isPublic)">
            <span class="study-meta-data" data-icon="L">&#x1f512; </span>
        </xsl:if>
        <a href="{$titleLink}">
            <xsl:call-template name="highlight">
                <xsl:with-param name="pQueryId" select="$queryid"/>
                <xsl:with-param name="pFieldName" select="'title'"/>
                <xsl:with-param name="pText" select="accession"/>
            </xsl:call-template>
        </a>
    </xsl:template>


    <xsl:template name="browse-no-results">
        <section class="grid_18 alpha">
            <h2 class="alert">We’re sorry that we couldn’t find any matching studies</h2>
            <xsl:if test="exists($keywords) and $keywords!=''">
                <p>Your search for <span class="alert"><xsl:value-of select="$keywords"/></span> returned no results.</p>
            </xsl:if>
            <xsl:if test="exists($vSuggestions) and $vSuggestions!=''">
            <h3>Did you mean...</h3>
            <ul>
                <xsl:for-each select="$vSuggestions">
                    <li><a href="search.html?query={.}"><xsl:value-of select="." /></a></li>
                </xsl:for-each>
            </ul>
            </xsl:if>
            <!--
            <p>&#160;</p>
            <xsl:if test="$vSearchMode">
                <h3>Try Experiments Browser</h3>
                <p>You can browse available experiments and create a more complex query using our <a href="{$context-path}/experiments/browse.html" title="Click to go to Experiments Browser">Experiments Browser</a>.</p>
            </xsl:if>
            -->
            <!-- TODO:
            <h4>Still can't find what you're looking for?</h4>
            <p>Please <a href="#" title="">contact our support service</a> for help if you still get no results.</p>
            -->
        </section>
        <aside class="grid_6 omega shortcuts" id="search-extras">
            <div id="ebi_search_results">
                <h3>More data from EMBL-EBI</h3>
            </div>
        </aside>

    </xsl:template>

    <xsl:template name="browse-error">

        <section class="grid_18 alpha">
            <h2 class="alert">Yikes! Looks like we are overloaded.</h2>
            <xsl:if test="exists($keywords) and $keywords!=''">
                <p>Your search for <span class="alert"><xsl:value-of select="$keywords"/></span> resulted in an error.</p>
            </xsl:if>
             <p>Our servers may be busy. Please try again later and <a href="#" class="feedback">contact us</a> if the error persists.</p>
        </section>
        <aside class="grid_6 omega shortcuts" id="search-extras">
            <div id="ebi_search_results">
                <h3>More data from EMBL-EBI</h3>
            </div>
        </aside>
    </xsl:template>
</xsl:stylesheet>