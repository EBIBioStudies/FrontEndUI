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
    <xsl:param name="vFacetData"/>
    <xsl:param name="pagesize"/>
    <xsl:param name="sortby"/>
    <xsl:param name="sortorder"/>
    <xsl:param name="queryid"/>
    <xsl:param name="tooManyExpansionTerms"/>

    <xsl:include href="bs-html-page.xsl"/>
    <xsl:include href="bs-sort-studies.xsl"/>
    <xsl:include href="bs-studies-templates.xsl"/>
    <!--<xsl:include href="bs-highlight.xsl"/>-->


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
                <script  defer="defer" src="//www.ebi.ac.uk/web_guidelines/js/ebi-global-search-run.js" type="text/javascript"/>
                <script  defer="defer" src="//www.ebi.ac.uk/web_guidelines/js/ebi-global-search.js" type="text/javascript"/>
                <script  defer="defer" src="{$context-path}/assets/scripts/jquery.query-2.1.7m-ebi.js" type="text/javascript"/>
                <script  defer="defer" src="{$context-path}/assets/scripts/jquery.bs-studies-browse-1.0.150220.js" type="text/javascript"/>
                <script defer="defer" src="{$context-path}/assets/scripts/facetfiller-1.0.0.js"></script>
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

                            </div>
                        </div>
                    </div>

                </section>
                <section class="grid_24 alpha omega browser">
                    <div id="ae-content">
                        <div id="ae-browse">
                            <div class="persist-area hecatos-persist-area">
                                <div class="persist-header" border="0" cellpadding="0" cellspacing="0">
                                    <xsl:call-template name="table-pager">
                                        <xsl:with-param name="pColumnsToSpan" select="1"/>
                                        <xsl:with-param name="pName" select="if ($vTotal > 1) then 'results' else 'result'"/>
                                        <xsl:with-param name="pTotal" select="$vTotal"/>
                                        <xsl:with-param name="pPage" select="$vPage"/>
                                        <xsl:with-param name="pPageSize" select="$vPageSize"/>
                                    </xsl:call-template>
                                </div>
                                <div id="ae-studies-browse-sort-by">
                                    <xsl:text>Sort by: </xsl:text>
                                    <select id="studies-browse-sorter">
                                        <option class="col_relevance sortable">
                                            <xsl:if  test="fn:lower-case($vSortBy)='relevance'" >
                                                <xsl:attribute name="selected" select="'selected'"/>
                                            </xsl:if>Relevance</option>
                                        <option class="col_accession sortable">
                                            <xsl:if  test="fn:lower-case($vSortBy)='accession'" >
                                                <xsl:attribute name="selected" select="'selected'"/>
                                            </xsl:if>Accession
                                        </option>
                                        <option class="col_title sortable">
                                            <xsl:if  test="fn:lower-case($vSortBy)='title'" >
                                                <xsl:attribute name="selected" select="'selected'"/>
                                            </xsl:if>Title</option>
                                        <option class="col_authors sortable">
                                            <xsl:if  test="fn:lower-case($vSortBy)='authors'" >
                                                <xsl:attribute name="selected" select="'selected'"/>
                                            </xsl:if>Authors</option>
                                        <option class="col_release_date sortable">
                                            <xsl:if  test="fn:lower-case($vSortBy)='release_date'" >
                                                <xsl:attribute name="selected" select="'selected'"/>
                                            </xsl:if>Released</option>
                                        <option class="col_files sortable">
                                            <xsl:if  test="fn:lower-case($vSortBy)='files'" >
                                                <xsl:attribute name="selected" select="'selected'"/>
                                            </xsl:if>Files</option>
                                        <option class="col_links sortable">
                                            <xsl:if  test="fn:lower-case($vSortBy)='links'" >
                                                <xsl:attribute name="selected" select="'selected'"/>
                                            </xsl:if>Links</option>
                                    </select>
                                    <span id="sorting-links">
                                        <a class="studies-browse-sort-order-left fa fa-angle-down"/>
                                        <a class="studies-browse-sort-order-right fa fa-angle-up"/>
                                    </span>
                                </div>
                                <div id="study-facet">
                                    <ul class="ae-studies-browse-list">
                                        <xsl:call-template name="study-facet">
                                            <xsl:with-param name="pStudies" select=".//study"/>
                                            <xsl:with-param name="pFrom" select="$vFrom"/>
                                            <xsl:with-param name="pTo" select="$vTo"/>
                                            <xsl:with-param name="pPosition" select="position()-1+$vFrom"/>
                                            <xsl:with-param name="vFacetData" select="$vFacetData"/>
                                        </xsl:call-template>
                                    </ul>
                                    <div class="grid_24 intro" id="secondary">
                                        <p>
                                            <xsl:if test="$vTotal>0">
                                                <h5 id="browsehecatos">
                                                    <a id="hecatos-ref" title="Update">
                                                        <div id="hecatos-div" class="icon icon-functional home-icon" data-icon="1">
                                                            Browse <xsl:value-of select="$project-title"/></div>
                                                    </a>
                                                </h5>
                                            </xsl:if>
                                        </p>
                                    </div>
                                </div>
                                <div id="browse-studies">
                                    <ul class="ae-studies-browse-list">
                                        <xsl:for-each select=".//study">
                                            <xsl:call-template name="study">
                                                <xsl:with-param name="pFrom" select="$vFrom"/>
                                                <xsl:with-param name="pTo" select="$vTo"/>
                                                <xsl:with-param name="pPosition" select="position()-1+$vFrom"/>
                                            </xsl:call-template>
                                        </xsl:for-each>
                                    </ul>
                                </div>
                            </div>
                        </div>
                    </div>
                </section>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="browse-no-results"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="study-facet">
        <xsl:param name="pFrom"/>
        <xsl:param name="pTo"/>
        <xsl:param name="pPosition"/>
        <xsl:param name="pStudies"/>
        <xsl:param name="vFacetData"/>
        <xsl:variable name="vFacets" select="('Assay Technology Type','Compound','Organ')"/>


        <xsl:for-each select="$vFacets">
            <xsl:variable name="vFacet" select="."/>
            <xsl:variable name="vFacetPosition" select="position()"/>
            <xsl:variable name="vFacetValues" select="distinct-values($vFacetData/facets/facet[lower-case(dim)=lower-case($vFacet)]/label/lower-case(normalize-space(text())))"/>
            <b><xsl:value-of select="$vFacet"/></b><br/>
            <ul style="list-style-type:none">
                <xsl:for-each select="$vFacetValues">
                    <xsl:variable name="vFacetVal" select="."/>
                    <li style="height:25px">
                        <!--<xsl:variable name="lid" select="concat( concat('facet',"$vFacetPosition1"), position())"/>-->
                        <xsl:variable name="lid" select="concat(concat('facet',$vFacetPosition), position())"/>
                        <input class="facet-value" type="checkbox"><xsl:attribute name="id" select="$lid" /><xsl:attribute name="value" select="." /><xsl:attribute
                                name="data-facet" select="$vFacet"/></input>
                        <label class="facet-label-class"><xsl:attribute name="for" select="$lid" /><xsl:value-of select="."/><span class="facet-freq-class"><xsl:value-of select="$vFacetData/facets/facet[label=$vFacetVal and lower-case(dim)=lower-case($vFacet)]/value"/></span></label> <!--fn:string-length(.)-->
                    </li>
                </xsl:for-each>
            </ul>
        </xsl:for-each>
        <input type="hidden"  id="facetsinfo">
            <xsl:attribute name="value" select="$vFacets" />
        </input>
    </xsl:template>


    <xsl:template name="study">
        <xsl:param name="pFrom"/>
        <xsl:param name="pTo"/>
        <xsl:param name="pPosition"/>
        <xsl:variable name="vAccession" select="accession"/>
        <xsl:variable name="isPublic" select="release_date!=9999999999 and contains(concat(' ',lower-case(access),' '),' public ')"/>
        <xsl:variable name="isProject" select="fn:lower-case(type)='project'"/>
        <!-- <xsl:variable name="vFiles" select="ae:getMappedValue('ftp-folder', $vAccession)"/> -->
        <li class="browse-study">
            <div>
                <xsl:if test="not($isPublic)">
                    <span class="study-meta-data" data-icon="L">&#x1f512; private</span>
                </xsl:if>
                <xsl:if test="not($isProject) and $isPublic ">
                    <span class="study-meta-data browse-study-release-date">
                        <xsl:value-of select="ae:formatDateLong(ae:unixTimeToDate(release_date))"/>
                    </span>
                </xsl:if>
                <xsl:if test="not($isProject)">
                    <xsl:if test="files != '0'">
                        <span class="study-meta-data browse-study-release-files">
                            <xsl:value-of select="files"/><xsl:text> data file</xsl:text><xsl:value-of select="if (files>1) then 's' else ''" />
                        </span>
                    </xsl:if>
                    <xsl:if test="links != '0'">
                        <span class="study-meta-data browse-study-release-links">
                            <xsl:value-of select="links"/><xsl:text> link</xsl:text><xsl:value-of select="if (links>1) then 's' else ''" />
                        </span>
                    </xsl:if>
                </xsl:if>
            </div>
            <div class="browse-study-title">
                <xsl:variable name="titleLink" select="if (fn:lower-case(type)='project') then fn:concat($context-path,'/',accession,'/studies/') else fn:concat($context-path,$projectLink,'/studies/',accession,'/',$vQueryString)"/>
                <xsl:variable name="accession" select="search:getQueryInfoParameter($queryid,'accessions')[$pPosition - $pFrom + 1]"/>
                <xsl:if test="lower-case(type)='project'">
                    <a class="project-logo no-border" href="{$titleLink}">
                        <img src="{$context-path}/files/{fn:replace(fn:replace(lower-case($accession),'&#x00ab;',''),'&#x00bb;','')}/logo.png"/>
                    </a>
                </xsl:if>
                <a href="{$titleLink}">
                    <xsl:call-template name="highlight">
                        <xsl:with-param name="pQueryId" select="$queryid"/>
                        <xsl:with-param name="pFieldName" select="'title'"/>
                        <xsl:with-param name="pText" select="search:getQueryInfoParameter($queryid,'titles')[$pPosition - $pFrom + 1]"/>
                    </xsl:call-template>
                </a>
                <span class="browse-study-accession">
                    <xsl:call-template name="highlight">
                        <xsl:with-param name="pQueryId" select="$queryid"/>
                        <xsl:with-param name="pFieldName" select="'accession'"/>
                        <xsl:with-param name="pText" select="$accession"/>
                    </xsl:call-template>
                </span>
            </div>
            <xsl:variable name="vSize" select="fn:count(author)"/>
            <xsl:if test="$vSize gt 0">
                <div class="search-authors">
                    <xsl:call-template name="highlight">
                        <xsl:with-param name="pQueryId" select="$queryid"/>
                        <xsl:with-param name="pFieldName" select="'authors'"/>
                        <xsl:with-param name="pText" select="search:getQueryInfoParameter($queryid,'authors')[$pPosition - $pFrom + 1]"/>
                    </xsl:call-template>
                </div>
            </xsl:if>
            <xsl:variable name="snippet" select="search:getQueryInfoParameter($queryid,'fragments')[$pPosition - $pFrom + 1]"/>
            <xsl:if test="$vSearchMode and $snippet!=''">
                <div class="search-snippet">
                    <xsl:if test="substring($snippet,1,1)=fn:lower-case(substring($snippet,1,1))"><xsl:attribute name="class">search-snippet search-snippet-before</xsl:attribute></xsl:if>
                    <xsl:call-template name="highlight">
                        <xsl:with-param name="pQueryId" select="$queryid"/>
                        <xsl:with-param name="pFieldName" select="'keywords'"/>
                        <xsl:with-param name="pText" select="search:getQueryInfoParameter($queryid,'fragments')[$pPosition - $pFrom + 1]"/>
                    </xsl:call-template>
                </div>
            </xsl:if>
        </li>
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

        </section>
        <aside class="grid_6 omega shortcuts" id="search-extras">
            <div id="ebi_search_results">
                <h5>More data from EMBL-EBI</h5>
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
                <h5>More data from EMBL-EBI</h5>
            </div>
        </aside>
    </xsl:template>
</xsl:stylesheet>
