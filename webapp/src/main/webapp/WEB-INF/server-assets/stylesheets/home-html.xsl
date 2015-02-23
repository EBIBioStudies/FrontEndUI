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
                extension-element-prefixes="fn ae search html xs"
                exclude-result-prefixes="fn ae search html xs"
                version="2.0">

    <xsl:include href="bs-html-page.xsl"/>
    <xsl:include href="bs-date-functions.xsl"/>

    <xsl:template match="/">
        <xsl:call-template name="bs-page">
            <xsl:with-param name="pIsSearchVisible" select="fn:true()"/>
            <xsl:with-param name="pExtraSearchFields"/>
            <xsl:with-param name="pTitleTrail"/>
            <xsl:with-param name="pExtraCSS"/>
            <xsl:with-param name="pBreadcrumbTrail"/>
            <xsl:with-param name="pEBISearchWidget"/>
            <xsl:with-param name="pExtraJS"/>
            <xsl:with-param name="pExtraBodyClasses"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="bs-content-section">
        <xsl:variable name="vStudies" select="search:queryIndex('studies', 'public:true')"/>
        <xsl:variable name="vTotal" select="fn:count($vStudies)"/>
        <xsl:variable name="vRetrieved" select="$vStudies[1]/../@updated"/>
        <!--
        <xsl:variable name="vFiles" select="search:queryIndex('files', 'userid:1 (kind:raw OR kind:processed)')"/>
        <xsl:variable name="vNews" select="doc('news.xml')"/>
        -->

        <div>
            <xsl:attribute name="class">alpha
                <xsl:choose>
                    <xsl:when test="$vTotal > 0">grid_18</xsl:when>
                    <xsl:otherwise>grid_24 omega</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <section>
                <h2>BioStudies – database of biological studies</h2>
                <p class="intro justify">The BioStudies database holds descriptions of biological studies, links to data
                    from these studies in other databases at EMBL-EBI or outside, as well as data that do not fit in the
                    structured archives at EMBL-EBI. The database can accept a wide range of types of studies described
                    via a simple format. It also enables manuscript authors to submit supplementary information and link
                    to it from the publication.
                </p>
            </section>
        </div>

        <xsl:if test="$vTotal > 0">
            <div class="grid_6 omega">
                <section>
                    <h3 class="icon icon-generic" data-icon="g">Statistics</h3>
                    <xsl:if test="fn:string-length($vRetrieved) > 1">
                        <h5>Updated <xsl:value-of select="ae:formatDateTime2($vRetrieved)"/></h5>
                    </xsl:if>
                    <ul>
                        <li>
                            <xsl:value-of
                                    select="fn:concat($vTotal, ' ', if ($vTotal > 1) then 'studies' else 'study')"/>
                        </li>
                        <!--
                        <li><xsl:value-of select="ae:formatFileSize(fn:sum($vFiles/@size) cast as xs:integer)"/> of archived data</li>
                        -->
                    </ul>
                </section>
            </div>
        </xsl:if>
        <!--
        <div class="grid_24 alpha">
            <xsl:if test="fn:count($vNews/news/item) > 0">
                <section id="ae-news">
                    <h3 class="icon icon-generic" data-icon="N">Latest News</h3>
                    <xsl:for-each select="$vNews/news/item">
                        <xsl:if test="fn:position() &lt; 3">
                            <p class="news"><xsl:value-of select="ae:formatDateGoogle(date)"/> - <strong><xsl:value-of select="title"/></strong><br/>
                                <xsl:copy-of select="summary/node()"/>
                                <xsl:if test="fn:string-length(text) > 0">
                                    <br/>
                                    <a href="news.html#{fn:position()}">Read more...</a>
                                </xsl:if>
                            </p>
                        </xsl:if>
                    </xsl:for-each>
                </section>
            </xsl:if>
            <section>
                <div class="grid_8 alpha">
                    <h3 class="icon icon-generic" data-icon="L">Links</h3>
                    <p>Information about how to search ArrayExpress, understand search results, how to submit data and FAQ can be found in our <a href="{$context-path}/help/index.html">Help section</a>.</p>
                    <p>Find out more about the <a href="/about/people/alvis-brazma">Functional Genomics group</a>.</p>
                </div>
                <div class="grid_8">
                    <h3 class="icon icon-functional" data-icon="t">Tools and Access</h3>
                    <p><a href="http://www.bioconductor.org/packages/release/bioc/html/ArrayExpress.html">ArrayExpress Bioconductor package</a>: an R package to access ArrayExpress and build data structures.</p>
                    <p><a href="{$context-path}/help/programmatic_access.html">Programmatic access</a>: query and download data using web services or JSON.</p>
                    <p><a href="ftp://ftp.ebi.ac.uk/pub/databases/microarray/data/">FTP access</a>: data can be downloaded directly from our FTP site.</p>
                </div>
                <div class="grid_8 omega">
                    <h3 class="icon icon-generic" data-icon="L">Related Projects</h3>
                    <p>Discover up and down regulated genes in numerous experimental conditions in the <a href="${interface.application.link.atlas.base.url}">Expression Atlas</a>.</p>
                    <p>Explore the <a href="/efo">Experimental Factor Ontology</a> used to support queries and annotation of ArrayExpress data.</p>
                </div>
            </section>
        </div>
        -->
    </xsl:template>
</xsl:stylesheet>
