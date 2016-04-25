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
    <xsl:param name="code"/>
    <xsl:param name="assignmentId"/>
    <xsl:param name="description"/>
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
        <section class="alpha intro grid_24 omega">
            <h3>Test Assignment for <xsl:value-of select="$assignmentId"/></h3>
            <xsl:choose>
                <xsl:when test="not($code)"> <!--modify ControlServlet.java as well-->
                    <h3>Please enter the code below:</h3>
                    <form method="get">
                        <input type="text" name="code" maxlength="11"
                               style="font-size:300%;font-family:monospace;width:7em;text-align:center"/>
                    </form>
                </xsl:when>
                <xsl:otherwise>
                    <h5>Instructions</h5>
                    <div style="text-align:justify">
                        <p><xsl:value-of select="$description" disable-output-escaping="yes"/></p>
                        <p><strong>Please note - time spent between downloading the assignment and sending us the
                            results will be measured; please proceed only when you're ready to complete the
                            assignment</strong>.
                        </p>

                        <form id="accept" method="post"
                              action="{$context-path}/jobs/{$assignmentId}">
                            <input type="hidden" name="start" value="true"/>
                            <input type="hidden" name="code" value="{$code}"/>
                            <input type="submit" value="I understand - please let me start the assignment"/>
                        </form>
                    </div>
                </xsl:otherwise>
            </xsl:choose>
        </section>
    </xsl:template>
</xsl:stylesheet>
