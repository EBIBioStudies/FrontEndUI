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
                xmlns:html="http://www.w3.org/1999/xhtml"
                version="2.0">
    <xsl:include href="bs-html-page.xsl"/>

    <xsl:template match="/">
        <xsl:call-template name="bs-page">
            <xsl:with-param name="pIsSearchVisible" select="true()"/>
            <xsl:with-param name="pExtraSearchFields"/>
            <xsl:with-param name="pTitleTrail"/>
            <xsl:with-param name="pExtraCSS"/>
            <xsl:with-param name="pBreadcrumbTrail"/>
            <xsl:with-param name="pEBISearchWidget"/>
            <xsl:with-param name="pExtraJS">
                <script>
                    $(document).ready( function() {
                        $("#file").change(function() {
                            $("#file").hide();
                            $("#wait").show();
                            $("#form").submit();
                        });
                    });
                </script>
            </xsl:with-param>
            <xsl:with-param name="pExtraBodyClasses"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="bs-content-section">
        <section class="alpha intro grid_24 omega">
            <h3>Select file to upload and index</h3>
            <form method="POST" id="form" enctype="multipart/form-data" >
                <input type="file" name="file" id="file" />
                <p style="display:none" id="wait">Uploading file. Please wait <img src="{$context-path}/assets/images/ajax-loader.gif"/></p>
            </form>
        </section>

    </xsl:template>
</xsl:stylesheet>
