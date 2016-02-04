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
                extension-element-prefixes="xs fn ae search"
                exclude-result-prefixes="xs fn ae search"
                version="2.0">

    <xsl:output method="text" indent="no" encoding="US-ASCII"/>

    <xsl:template match="/experiments">

        <!--
            This report will only have output if there are problems; otherwise the output should be empty
            Checks:

                1. (*CRITICAL*) Public experiments with future release date
                2. Experiments with missing or malformed release date
                3. Experiments with more than one sequence data URI defined
                4. Experiments with more than one title defined
        -->
        <xsl:text>--- ISSUES --------------------------------------------------------------------&#10;</xsl:text>
        <xsl:for-each select="experiment[source/@visible = 'true']">
            <xsl:sort select="substring(accession, 3, 4)" order="ascending"/>
            <xsl:sort select="substring(accession, 8)" order="ascending" data-type="number"/>

            <xsl:if test="user/@id = 1 and releasedate > fn:current-date()">
                <xsl:text> *** CRITICAL *** Public experiment </xsl:text>
                <xsl:value-of select="accession"/>
                <xsl:text> has a future release date: [</xsl:text>
                <xsl:value-of select="releasedate"/>
                <xsl:text>]&#10;</xsl:text>
            </xsl:if>
        </xsl:for-each>
        <xsl:text>&#10;&#10;--- WARNINGS ----------------------------------------------------------------&#10;</xsl:text>
        <xsl:for-each select="experiment[source/@visible = 'true']">
            <xsl:sort select="substring(accession, 3, 4)" order="ascending"/>
            <xsl:sort select="substring(accession, 8)" order="ascending" data-type="number"/>

            <xsl:variable name="vExperiment" select="."/>

            <xsl:if test="fn:not(fn:exists(releasedate)) or fn:not(fn:matches(releasedate, '\d{4}-\d{2}-\d{2}'))">
                <xsl:text> * Experiment </xsl:text>
                <xsl:value-of select="accession"/>
                <xsl:text> has a missing or poorly formatted release date: [</xsl:text>
                <xsl:value-of select="releasedate"/>
                <xsl:text>]&#10;</xsl:text>
            </xsl:if>

            <xsl:if test="fn:count(name) > 1">
                <xsl:text> * Experiment </xsl:text>
                <xsl:value-of select="accession"/>
                <xsl:text> has more than one experiment title defined: ["</xsl:text>
                <xsl:value-of select="fn:string-join(name, '&#34;, &#34;')"/>
                <xsl:text>"]&#10;</xsl:text>
            </xsl:if>

            <xsl:if test="fn:matches(accession, '^E-GEOD-\d+$')
                            and fn:not(fn:exists(secondaryaccession[fn:matches(text(), '^(GSE|GDS)\d+$')]))">
                <xsl:text> * GEO experiment </xsl:text>
                <xsl:value-of select="accession"/>
                <xsl:text> does not have a GEO secondary accession defined&#10;</xsl:text>
            </xsl:if>

            <xsl:for-each select="secondaryaccession">
                <xsl:if test="fn:matches(., '^(DRP|ERP|SRP)\d+$')
                                and fn:not(fn:matches($vExperiment/accession, '^E-GEOD-\d+$'))
                                and ae:getMappedValue('raw-files', $vExperiment/accession) = 0
                                and fn:not(fn:exists($vExperiment/seqdatauri))">
                    <xsl:text> * Experiment </xsl:text>
                    <xsl:value-of select="$vExperiment/accession"/>
                    <xsl:text> has ENA secondary accession: [</xsl:text>
                    <xsl:value-of select="."/>
                    <xsl:text>] but no raw data available or SequenceDataURI defined&#10;</xsl:text>
                </xsl:if>

                <xsl:if test="fn:matches(., '^(EGAS)\d+$')
                                and ae:getMappedValue('raw-files', $vExperiment/accession) = 0
                                and fn:not(fn:exists($vExperiment/seqdatauri))">
                    <xsl:text> * Experiment </xsl:text>
                    <xsl:value-of select="$vExperiment/accession"/>
                    <xsl:text> has EGA secondary accession: [</xsl:text>
                    <xsl:value-of select="."/>
                    <xsl:text>] but no raw data available or SequenceDataURI defined&#10;</xsl:text>
                </xsl:if>
            </xsl:for-each>

        </xsl:for-each>


    </xsl:template>
</xsl:stylesheet>