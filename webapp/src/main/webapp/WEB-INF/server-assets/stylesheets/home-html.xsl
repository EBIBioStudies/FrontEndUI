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
                extension-element-prefixes="fn ae search html xs"
                exclude-result-prefixes="fn ae search html xs"
                version="2.0">
    <xsl:param name="queryid"/>
    <xsl:include href="bs-html-page.xsl"/>
    <xsl:include href="bs-date-functions.xsl"/>

    <xsl:template match="/">
        <xsl:call-template name="bs-page">
            <xsl:with-param name="pIsSearchVisible" select="fn:true()"/>
            <xsl:with-param name="pExtraSearchFields"/>
            <xsl:with-param name="pTitleTrail"/>
            <xsl:with-param name="pBreadcrumbTrail"/>
            <xsl:with-param name="pEBISearchWidget"/>
            <xsl:with-param name="pExtraJS">
                <script src="{$context-path}/assets/scripts/jquery.bs-studies-home-1.0.151127.js" type="text/javascript"/>
            </xsl:with-param>
            <xsl:with-param name="pExtraBodyClasses"/>
            <xsl:with-param name="pExtraCSS">
                <link rel="stylesheet" href="{$context-path}/assets/stylesheets/bs-study-home-1.0.151126.css"
                      type="text/css"/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="bs-content-section">
        <xsl:variable name="vStudies" select="//study"/>
        <xsl:variable name="vTotal" select="xs:integer(search:getQueryInfoParameter($queryid,'total'))"/>
        <xsl:variable name="vRetrieved" select="$vStudies[1]/../@updated"/>
        <!--
        <xsl:variable name="vFiles" select="search:queryIndex('files', 'userid:1 (kind:raw OR kind:processed)')"/>
        <xsl:variable name="vNews" select="doc('news.xml')"/>
        -->
        <section class="alpha intro  columns medium-12" id="primary">
            <div id="bs-title-header">
                <div class="ribbon">s</div>
                <div class="twist">d</div>
                <div class="molecule">b</div>
                <div class="flask">A</div>
                <h2>
                    <div id="tagline">BioStudies – one package for all the data supporting a study</div>
                </h2>
            </div>
            <p class="justify">The BioStudies database holds descriptions of biological studies, links to data
                from these studies in other databases at EMBL-EBI or outside, as well as data that do not fit in the
                structured archives at EMBL-EBI. The database can accept a wide range of types of studies described
                via a simple format. It also enables manuscript authors to submit supplementary information and link
                to it from the publication.
            </p>
        </section>
        <div class="columns medium-12  intro" id="secondary">
            <xsl:if test="$vTotal>0">
            <h2><a href="{$context-path}/studies/" title="Browse BioStudies"><span class="icon icon-functional home-icon" data-icon="1">
                Browse</span>
            </a></h2>
            </xsl:if>
        </div>
        <div class="columns medium-12" id="tertiary">
        <div class="row">
            <div class="columns medium-3" id="stats">
                <xsl:if test="$vTotal>0">
                <h5>
                    <a href="{$context-path}/studies/" title="Browse BioStudies">
                        <span class="icon icon-functional home-icon-small" id="submissionsIcon" data-icon="D" />
                        <xsl:value-of select="fn:concat($vTotal, ' submissions')"/>
                    </a>
                </h5>
                </xsl:if>
                <h5 id="projectCountStats" >
                    <a href="{$context-path}/studies/?query=type:Project" title="Browse BioStudies">
                        <span class="icon icon-functional home-icon-small" id="projectsIcon"  data-icon="A" />
                        <span id="projectCount"/>
                    </a>
                </h5>
                <h5 id="studyCountStats">
                    <a href="{$context-path}/studies/?query=type:Study" title="Browse BioStudies">
                        <span class="icon icon-functional home-icon-small" id="studiesIcon"  data-icon="b" />
                        <span id="studyCount"/>
                    </a>
                </h5>
            </div>
            <div class="columns medium-9" id="latest" style="display: block;">
                <h5>
                    <a href="#" title="Latest studies">
                        <span class="icon icon-functional home-icon-small" id="latestIcon" data-icon="n" />Latest</a>
                </h5>
                <ul id="latestList">

                </ul>
            </div>
        </div>
        </div>
    </xsl:template>
</xsl:stylesheet>
