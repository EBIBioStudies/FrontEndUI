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
                exclude-result-prefixes="xs ae fn"
                version="2.0">

    <xsl:function name="ae:isFutureDate" as="xs:boolean">
        <xsl:param name="pDate"/>
        <xsl:variable name="vTodaysDate" as="xs:date" select="fn:current-date()"/>
        <xsl:choose>
            <xsl:when test="fn:not($pDate castable as xs:date)">
                <xsl:value-of select="fn:false()"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="xs:date($pDate) > $vTodaysDate"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="ae:dateTimeToRfc822" as="xs:string">
        <xsl:param name="pDateTime"/>
        <xsl:value-of select="fn:format-dateTime($pDateTime, '[FNn,*-3], [D01] [MNn,*-3] [Y0001] [H01]:[m01]:[s01] +0000', 'en', (), ())"/>
    </xsl:function>

    <xsl:function name="ae:formatDateTime" as="xs:string">
        <xsl:param name="pDateTime"/>
        <xsl:variable name="vDate" as="xs:date" select="xs:date(if (fn:contains($pDateTime, 'T')) then fn:substring-before($pDateTime, 'T') else $pDateTime)"/>
        <xsl:variable name="vTodaysDate" as="xs:date" select="fn:current-date()"/>
        <xsl:variable name="vYesterdaysDate" as="xs:date" select="$vTodaysDate - xs:dayTimeDuration('P1D')"/>
        <xsl:choose>
            <xsl:when test="fn:not($pDateTime castable as xs:dateTime)">
                <xsl:value-of select="''"/>
            </xsl:when>
            <xsl:when test="$vTodaysDate eq $vDate">
                <xsl:value-of select="fn:format-dateTime(xs:dateTime($pDateTime), 'Today, [H01]:[m01]', 'en', (), ())"/>
            </xsl:when>
            <xsl:when test="$vYesterdaysDate eq $vDate">
                <xsl:value-of select="fn:format-dateTime(xs:dateTime($pDateTime), 'Yesterday, [H01]:[m01]', 'en', (), ())"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="fn:format-dateTime(xs:dateTime($pDateTime), '[D1] [MNn] [Y0001], [H01]:[m01]', 'en', (), ())"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="ae:formatDateTime2" as="xs:string">
        <xsl:param name="pDateTime"/>
        <xsl:variable name="vDate" as="xs:date" select="xs:date(if (fn:contains($pDateTime, 'T')) then fn:substring-before($pDateTime, 'T') else $pDateTime)"/>
        <xsl:variable name="vTodaysDate" as="xs:date" select="fn:current-date()"/>
        <xsl:variable name="vYesterdaysDate" as="xs:date" select="$vTodaysDate - xs:dayTimeDuration('P1D')"/>
        <xsl:choose>
            <xsl:when test="fn:not($pDateTime castable as xs:dateTime)">
                <xsl:value-of select="''"/>
            </xsl:when>
            <xsl:when test="$vTodaysDate eq $vDate">
                <xsl:value-of select="fn:format-dateTime(xs:dateTime($pDateTime), 'today at [H01]:[m01]', 'en', (), ())"/>
            </xsl:when>
            <xsl:when test="$vYesterdaysDate eq $vDate">
                <xsl:value-of select="fn:format-dateTime(xs:dateTime($pDateTime), 'yesterday at [H01]:[m01]', 'en', (), ())"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="fn:format-dateTime(xs:dateTime($pDateTime), 'on [D1] [MNn] [Y0001] at [H01]:[m01]', 'en', (), ())"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="ae:formatDate" as="xs:string">
        <xsl:param name="pDate"/>
        <xsl:variable name="vTodaysDate" as="xs:date" select="fn:current-date()"/>
        <xsl:variable name="vTomorrowDate" as="xs:date" select="$vTodaysDate + xs:dayTimeDuration('P1D')"/>
        <xsl:variable name="vYesterdaysDate" as="xs:date" select="$vTodaysDate - xs:dayTimeDuration('P1D')"/>
        <xsl:choose>
            <xsl:when test="fn:not($pDate castable as xs:date)">
                <xsl:value-of select="''"/>
            </xsl:when>
            <xsl:when test="$vTomorrowDate eq xs:date($pDate)">
                <xsl:value-of select="'tomorrow'"/>
            </xsl:when>
            <xsl:when test="$vTodaysDate eq xs:date($pDate)">
                <xsl:value-of select="'today'"/>
            </xsl:when>
            <xsl:when test="$vYesterdaysDate eq xs:date($pDate)">
                <xsl:value-of select="'yesterday'"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="fn:format-date(xs:date($pDate), 'on [D1] [MNn] [Y0001]', 'en', (), ())"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="ae:formatDateLong" as="xs:string">
        <xsl:param name="pDate"/>
        <xsl:variable name="vTodaysDate" as="xs:date" select="fn:current-date()"/>
        <xsl:variable name="vTomorrowDate" as="xs:date" select="$vTodaysDate + xs:dayTimeDuration('P1D')"/>
        <xsl:variable name="vYesterdaysDate" as="xs:date" select="$vTodaysDate - xs:dayTimeDuration('P1D')"/>
        <xsl:choose>
            <xsl:when test="fn:not($pDate castable as xs:date)">
                <xsl:value-of select="''"/>
            </xsl:when>
            <xsl:when test="$vTomorrowDate eq xs:date($pDate)">
                <xsl:value-of select="'tomorrow'"/>
            </xsl:when>
            <xsl:when test="$vTodaysDate eq xs:date($pDate)">
                <xsl:value-of select="'today'"/>
            </xsl:when>
            <xsl:when test="$vYesterdaysDate eq xs:date($pDate)">
                <xsl:value-of select="'yesterday'"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="fn:format-date(xs:date($pDate), '[D1] [MNn] [Y0001]', 'en', (), ())"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="ae:formatDateGoogle" as="xs:string">
        <xsl:param name="pDate"/>
        <xsl:value-of select="fn:format-date(xs:date($pDate), '[D1] [MNn] [Y0001]', 'en', (), ())"/>
    </xsl:function>

    <xsl:function name="ae:unixTimeToDate" as="xs:dateTime">
        <xsl:param name="pTime"  />
        <xsl:value-of select='xs:dateTime("1970-01-01T00:00:00") + number($pTime) * xs:dayTimeDuration("PT01S")'/>
    </xsl:function>

    <xsl:function name="ae:formatDateShort" as="xs:string">
        <xsl:param name="pDate"/>
        <xsl:variable name="vTodaysDate" as="xs:date" select="fn:current-date()"/>
        <xsl:variable name="vTomorrowDate" as="xs:date" select="$vTodaysDate + xs:dayTimeDuration('P1D')"/>
        <xsl:variable name="vYesterdaysDate" as="xs:date" select="$vTodaysDate - xs:dayTimeDuration('P1D')"/>
        <xsl:choose>
            <xsl:when test="fn:not($pDate castable as xs:date)">
                <xsl:value-of select="''"/>
            </xsl:when>
            <xsl:when test="$vTomorrowDate eq xs:date($pDate)">
                <xsl:value-of select="'Tomorrow'"/>
            </xsl:when>
            <xsl:when test="$vTodaysDate eq xs:date($pDate)">
                <xsl:value-of select="'Today'"/>
            </xsl:when>
            <xsl:when test="$vYesterdaysDate eq xs:date($pDate)">
                <xsl:value-of select="'Yesterday'"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="fn:format-date(xs:date($pDate), '[D01]/[M01]/[Y]', 'en', (), ())"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="ae:formatDateEBEye">
        <xsl:param name="pDate" as="xs:date"/>
        <xsl:value-of select="fn:format-date($pDate, '[D01]-[MN,*-3]-[Y0001]')"/>
    </xsl:function>

</xsl:stylesheet>