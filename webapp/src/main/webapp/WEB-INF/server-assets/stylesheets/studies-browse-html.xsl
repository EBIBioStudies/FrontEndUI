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
                extension-element-prefixes="ae fn search html xs"
                exclude-result-prefixes="ae fn search html xs"
                version="2.0">

    <xsl:param name="page"/>
    <xsl:param name="pagesize"/>
    <xsl:param name="sortby"/>
    <xsl:param name="sortorder"/>

    <xsl:param name="queryid"/>

    <xsl:include href="bs-html-page.xsl"/>
    <xsl:include href="bs-highlight.xsl"/>
    <xsl:include href="bs-sort-studies.xsl"/>
    <xsl:include href="bs-date-functions.xsl"/>

    <xsl:variable name="vSearchMode" select="$keywords != ''"/>
    <xsl:variable name="vQueryString" select="if ($query-string) then fn:concat('?', $query-string) else ''"/>
    <xsl:variable name="vUnrestrictedAccess" select="fn:not($userid)"/>

    <xsl:variable name="vFilteredStudies" select="search:queryIndex($queryid)"/>
    <xsl:variable name="vTotal" select="count($vFilteredStudies)"/>

    <xsl:template match="/">
        <xsl:variable name="vTitle" select="if ($vSearchMode) then fn:concat('Studies matching &quot;', $keywords, '&quot;') else 'Studies'"/>

        <xsl:call-template name="bs-page">
            <xsl:with-param name="pIsSearchVisible" select="fn:true()"/>
            <xsl:with-param name="pEBISearchWidget">
                <xsl:if test="$vSearchMode and $vTotal > 0">
                    <aside class="grid_6 omega shortcuts expander" id="search-extras">
                        <div id="ebi_search_results">
                            <h3 class="slideToggle icon icon-functional" data-icon="u">Show more data from EMBL-EBI</h3>
                        </div>
                    </aside>
                </xsl:if>
            </xsl:with-param>
            <xsl:with-param name="pExtraSearchFields">
                <!--
                <input id="ls-organism" type="hidden" name="organism" value="{$organism}"/>
                <input id="ls-array" type="hidden" name="array" value="{$array}"/>
                <input id="ls-expdesign" type="hidden" name="exptype[]" value="{$exptype[1]}"/>
                <input id="ls-exptech" type="hidden" name="exptype[]" value="{$exptype[2]}"/>
                -->
            </xsl:with-param>
            <xsl:with-param name="pTitleTrail" select="$vTitle"/>
            <xsl:with-param name="pExtraCSS">
                <link rel="stylesheet" href="{$context-path}/assets/stylesheets/bs-studies-browse-1.0.150220.css" type="text/css"/>
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
        <xsl:variable name="vSortBy" select="if ($sortby) then $sortby else 'name'"/>
        <xsl:variable name="vSortOrder" select="if ($sortorder) then $sortorder else 'descending'"/>

        <xsl:variable name="vPage" select="if ($page and $page castable as xs:integer) then $page cast as xs:integer else 1" as="xs:integer"/>
        <xsl:variable name="vPageSize" select="if ($pagesize and $pagesize castable as xs:integer) then $pagesize cast as xs:integer else 25" as="xs:integer"/>

        <xsl:variable name="vFrom" as="xs:integer">
            <xsl:choose>
                <xsl:when test="$vPage > 0"><xsl:value-of select="1 + ( $vPage - 1 ) * $vPageSize"/></xsl:when>
                <xsl:when test="$vTotal = 0">0</xsl:when>
                <xsl:otherwise>1</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="vTo" as="xs:integer">
            <xsl:choose>
                <xsl:when test="( $vFrom + $vPageSize - 1 ) > $vTotal"><xsl:value-of select="$vTotal"/></xsl:when>
                <xsl:otherwise><xsl:value-of select="$vFrom + $vPageSize - 1"/></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>


        <xsl:choose>
            <xsl:when test="$vTotal&gt;0">
                <section class="grid_18 alpha search-title">
                    <xsl:if test="$vSearchMode">
                        <h2>
                            <xsl:text>BioStudies results for </xsl:text>
                            <span class="ae_keywords"><xsl:value-of select="$keywords"/></span>
                        </h2>
                    </xsl:if>
                    <xsl:text>&#160;</xsl:text>
                </section>
                <xsl:if test="$vSearchMode">
                    <aside class="grid_6 omega shortcuts expander" id="search-extras">
                        <div id="ebi_search_results">
                            <h3 class="slideToggle icon icon-functional" data-icon="u">Show more data from EMBL-EBI<i class="fa fa-spinner fa-pulse"/></h3>
                        </div>
                    </aside>
                </xsl:if>
                <section class="grid_24 alpha omega browser">
                    <div id="ae-content">
                        <div id="ae-browse">
                            <div class="persist-area">
                                <table class="persist-header" border="0" cellpadding="0" cellspacing="0">
                                    <col class="col_accession"/>
                                    <col class="col_title"/>
                                    <col class="col_author"/>
                                    <col class="col_release_date"/>
                                    <col class="col_files"/>
                                    <col class="col_links"/>
                                    <thead>
                                        <xsl:call-template name="table-pager">
                                            <xsl:with-param name="pColumnsToSpan" select="6"/>
                                            <xsl:with-param name="pName"
                                                            select="if ($vTotal > 1) then 'studies' else 'study'"/>
                                            <xsl:with-param name="pTotal" select="$vTotal"/>
                                            <xsl:with-param name="pPage" select="$vPage"/>
                                            <xsl:with-param name="pPageSize" select="$vPageSize"/>
                                        </xsl:call-template>
                                        <!--tr>
                                            <th class="col_accession sortable">
                                                <xsl:text>Accession</xsl:text>
                                                <xsl:call-template name="add-table-sort">
                                                    <xsl:with-param name="pKind" select="'accession'"/>
                                                    <xsl:with-param name="pSortBy" select="$vSortBy"/>
                                                    <xsl:with-param name="pSortOrder" select="$vSortOrder"/>
                                                </xsl:call-template>
                                            </th>
                                            <th class="col_title sortable">
                                                <xsl:text>Title</xsl:text>
                                                <xsl:call-template name="add-table-sort">
                                                    <xsl:with-param name="pKind" select="'name'"/>
                                                    <xsl:with-param name="pSortBy" select="$vSortBy"/>
                                                    <xsl:with-param name="pSortOrder" select="$vSortOrder"/>
                                                </xsl:call-template>
                                            </th>
                                            <th class="col_author sortable">
                                                <xsl:text>Authors</xsl:text>
                                                <xsl:call-template name="add-table-sort">
                                                    <xsl:with-param name="pKind" select="'author'"/>
                                                    <xsl:with-param name="pSortBy" select="$vSortBy"/>
                                                    <xsl:with-param name="pSortOrder" select="$vSortOrder"/>
                                                </xsl:call-template>
                                            </th>
                                            <th class="col_release_date sortable">
                                                <xsl:text>Released</xsl:text>
                                                <xsl:call-template name="add-table-sort">
                                                    <xsl:with-param name="pKind" select="'releasedate'"/>
                                                    <xsl:with-param name="pSortBy" select="$vSortBy"/>
                                                    <xsl:with-param name="pSortOrder" select="$vSortOrder"/>
                                                </xsl:call-template>
                                            </th>
                                            <th class="col_files sortable">
                                                <xsl:text>Files</xsl:text>
                                                <xsl:call-template name="add-table-sort">
                                                    <xsl:with-param name="pKind" select="'files'"/>
                                                    <xsl:with-param name="pSortBy" select="$vSortBy"/>
                                                    <xsl:with-param name="pSortOrder" select="$vSortOrder"/>
                                                </xsl:call-template>
                                            </th>
                                            <th class="col_links sortable">
                                                <xsl:text>Links</xsl:text>
                                                <xsl:call-template name="add-table-sort">
                                                    <xsl:with-param name="pKind" select="'links'"/>
                                                    <xsl:with-param name="pSortBy" select="$vSortBy"/>
                                                    <xsl:with-param name="pSortOrder" select="$vSortOrder"/>
                                                </xsl:call-template>
                                            </th>
                                        </tr-->
                                    </thead>
                                </table>
                                <div>
                                    <ul class="ae-studies-browse-list">
                                        <xsl:call-template name="ae-sort-experiments">
                                            <xsl:with-param name="pExperiments" select="$vFilteredStudies"/>
                                            <xsl:with-param name="pFrom" select="$vFrom"/>
                                            <xsl:with-param name="pTo" select="$vTo"/>
                                            <xsl:with-param name="pSortBy" select="$vSortBy"/>
                                            <xsl:with-param name="pSortOrder" select="$vSortOrder"/>
                                        </xsl:call-template>
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

    <xsl:template match="study">
        <xsl:param name="pFrom"/>
        <xsl:param name="pTo"/>
        <xsl:if test="position() >= $pFrom and not(position() > $pTo)">
            <xsl:variable name="vAccession" select="accession"/>
            <!-- <xsl:variable name="vFiles" select="ae:getMappedValue('ftp-folder', $vAccession)"/> -->
            <li class="browse-study">
                <div>
                    <span class="browse-study-release-date">
                        <xsl:value-of select="ae:formatDateLong(releasedate)"/>
                    </span>
                    <xsl:if test="@files != '0'">
                        <span class="browse-study-release-files">
                                <xsl:value-of select="@files"/><xsl:text> data files</xsl:text>
                        </span>
                    </xsl:if>
                    <xsl:if test="@links != '0'">
                        <span class="browse-study-release-links">
                            <xsl:value-of select="@links"/><xsl:text> links</xsl:text>
                        </span>
                    </xsl:if>
                </div>
                <div class="browse-study-title">
                    <a href="{$context-path}/studies/{accession}/{$vQueryString}">
                        <xsl:call-template name="highlight">
                            <xsl:with-param name="pQueryId" select="$queryid"/>
                            <xsl:with-param name="pText" select="fn:string-join(title, ', ')"/>
                            <xsl:with-param name="pFieldName"/>
                        </xsl:call-template>
                    </a>
                </div>
                <div>
                    <xsl:variable name="vSize" select="fn:count(author)"/>
                    <xsl:for-each select="author[fn:position() = (1 to 5)]">
                        <span>
                            <xsl:call-template name="highlight">
                                <xsl:with-param name="pQueryId" select="$queryid"/>
                                <xsl:with-param name="pText" select="."/>
                                <xsl:with-param name="pFieldName"/>
                            </xsl:call-template>
                        </span>
                        <xsl:if test="fn:position() != fn:last() or $vSize &gt; 5">
                            <xsl:text>, </xsl:text>
                        </xsl:if>
                    </xsl:for-each>
                </div>
                <!--div>

                </div>
                <div>
                    <xsl:choose>
                        <xsl:when test="@links != '0'">
                            <xsl:value-of select="@links"/>
                        </xsl:when>
                        <xsl:otherwise>&#8729;</xsl:otherwise>
                    </xsl:choose>
                </div-->
            </li>
        </xsl:if>
    </xsl:template>

    <!--
    <xsl:template name="data-files-main">
        <xsl:param name="pAccession"/>
        <xsl:param name="pEnaAccession"/>
        <xsl:param name="pFiles"/>
        <xsl:param name="pKind"/>

        <xsl:variable name="vFiles" select="$pFiles/file[@kind = $pKind]"/>
        <xsl:choose>
            <xsl:when test="fn:count($vFiles) > 1">
                <a href="{$context-path}/experiments/{$pAccession}/files/{$pKind}/">
                    <span class="icon icon-generic" data-icon="L"/>
                </a>
            </xsl:when>
            <xsl:when test="fn:count($vFiles) = 1">
                <a href="{$context-path}/files/{$pAccession}/{$vFiles[1]/@name}">
                    <span class="icon icon-functional" data-icon="="/>
                </a>
            </xsl:when>
            <xsl:otherwise>
                <xsl:if test="fn:not($pKind = 'raw' and fn:exists(seqdatauri))"><xsl:text>-</xsl:text></xsl:if>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:if test="$pKind = 'raw'">
            <xsl:if test="fn:exists($vFiles) and fn:exists(seqdatauri)">
                <xsl:text>, </xsl:text>
            </xsl:if>
            <xsl:for-each-group select="seqdatauri" group-by="fn:contains(., '/ena/')">
                <xsl:choose>
                    <xsl:when test="fn:current-grouping-key()">
                        <xsl:choose>
                            <xsl:when test="fn:count(fn:current-group()) = 1
                                            and fn:matches(fn:current-group()[1], '/[DES]RR\d+$')">
                                <a href="{fn:current-group()[1]}" title="Click to go to ENA run"><img src="{$context-path}/assets/images/ena-icon-16.svg" width="22" height="16" alt="ENA"/></a>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:for-each select="$pEnaAccession">
                                    <a href="http://www.ebi.ac.uk/ena/data/view/{.}" title="Click to go to ENA study"><img src="{$context-path}/assets/images/ena-icon-16.svg" width="22" height="16" alt="ENA"/></a>
                                    <xsl:if test="fn:position() != fn:last()">
                                        <xsl:text>, </xsl:text>
                                    </xsl:if>
                                </xsl:for-each>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:for-each select="fn:current-group()">
                            <xsl:choose>
                                <xsl:when test="fn:contains(., '/ega/')">
                                    <a href="{.}" title="Click to go to EGA study"><img src="{$context-path}/assets/images/ega-icon-16.png" width="16" height="16" alt="EGA"/></a>
                                </xsl:when>
                                <xsl:otherwise>
                                    <a href="{.}">
                                        <span class="icon icon-generic" data-icon="L"/>
                                    </a>
                                </xsl:otherwise>
                            </xsl:choose>
                            <xsl:if test="fn:position() != fn:last()">
                                <xsl:text>, </xsl:text>
                            </xsl:if>
                        </xsl:for-each>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each-group>
        </xsl:if>
    </xsl:template>
    -->

    <xsl:template name="browse-no-results">

        <section class="grid_18 alpha">
            <h2 class="alert">We’re sorry that we couldn’t find any matching studies</h2>
            <p>Your search for <span class="alert"><xsl:value-of select="$keywords"/></span> returned no results.</p>
            <!-- TODO:
            <h3>Did you mean...</h3>
            <ul>
                <li>Suggestion 1</li>
                <li>Suggestion 2</li>
                <li>Suggestion 3</li>
            </ul>
            -->
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
</xsl:stylesheet>
