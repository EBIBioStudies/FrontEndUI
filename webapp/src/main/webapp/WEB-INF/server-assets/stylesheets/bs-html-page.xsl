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
                xmlns:html="http://www.w3.org/1999/xhtml"
                extension-element-prefixes="html xs fn ae"
                exclude-result-prefixes="html xs fn ae"
                version="2.0">

    <xsl:param name="host"/>
    <xsl:param name="context-path"/>
    <xsl:param name="original-request-uri"/>
    <xsl:param name="referer"/>
    <xsl:param name="keywords"/>
    <xsl:param name="query-string"/>
    <xsl:param name="userid"/>
    <xsl:param name="username"/>
    <xsl:param name="project"/>
    <xsl:param name="project-title" select="''"/>
    <xsl:param name="project-description" select="''"/>
    <xsl:param name="project-url" select="''"/>

    <xsl:output omit-xml-declaration="yes" method="html" indent="no" encoding="UTF-8"/>

    <xsl:variable name="relative-uri" select="fn:substring-after($original-request-uri, $context-path)"/>
    <xsl:variable name="relative-referer" select="if (fn:starts-with($referer, '/')) then fn:substring-after($referer, $context-path) else ''"/>

    <xsl:variable name="secure-host" select="if (fn:matches($host, '^http[:]//www(dev)?[.]ebi[.]ac[.]uk$')) then fn:replace($host, '^http[:]//', 'https://') else ''"/>

    <xsl:template name="bs-page">
        <xsl:param name="pIsSearchVisible" as="xs:boolean"/>
        <xsl:param name="pExtraSearchFields"/>
        <xsl:param name="pTitleTrail" as="xs:string"/>
        <xsl:param name="pBreadcrumbTrail"/>
        <xsl:param name="pEBISearchWidget"/>
        <xsl:param name="pExtraCSS"/>
        <xsl:param name="pExtraJS"/>
        <xsl:param name="pExtraBodyClasses" as="xs:string"/>

        <xsl:text disable-output-escaping='yes'>&lt;!DOCTYPE html></xsl:text>
        <!-- http://paulirish.com/2008/conditional-stylesheets-vs-css-hacks-answer-neither/ -->
        <xsl:comment>[if lt IE 7]>&lt;html class="no-js ie6 oldie" lang="en">&lt;![endif]</xsl:comment>
        <xsl:comment>[if IE 7]>&lt;html class="no-js ie7 oldie" lang="en">&lt;![endif]</xsl:comment>
        <xsl:comment>[if IE 8]>&lt;html class="no-js ie8 oldie" lang="en">&lt;![endif]</xsl:comment>
        <xsl:comment>[if gt IE 8]>&lt;!</xsl:comment>
        <html class="no-js" lang="en">
            <xsl:comment>&lt;![endif]</xsl:comment>
            <xsl:call-template name="ae-page-head">
                <xsl:with-param name="pTitleTrail" select="$pTitleTrail"/>
                <xsl:with-param name="pExtraCode" select="$pExtraCSS"/>
            </xsl:call-template>
            <xsl:call-template name="ae-page-body">
                <xsl:with-param name="pIsSearchVisible" select="$pIsSearchVisible"/>
                <xsl:with-param name="pExtraSearchFields" select="$pExtraSearchFields"/>
                <xsl:with-param name="pBreadcrumbTrail" select="$pBreadcrumbTrail"/>
                <xsl:with-param name="pEBISearchWidget" select="$pEBISearchWidget"/>
                <xsl:with-param name="pExtraCode" select="$pExtraJS"/>
                <xsl:with-param name="pExtraBodyClasses" select="$pExtraBodyClasses"/>
            </xsl:call-template>
        </html>
    </xsl:template>

    <xsl:template name="ae-page-head">
        <xsl:param name="pTitleTrail"/>
        <xsl:param name="pExtraCode"/>
        <head>
            <meta charset="utf-8"/>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
            <title>
                <xsl:if test="$pTitleTrail"><xsl:value-of select="$pTitleTrail"/> &lt; </xsl:if>
                <xsl:text>BioStudies &lt; EMBL-EBI</xsl:text>
            </title>
            <meta name="description" content="EMBL-EBI"/>   <!-- Describe what this page is about -->
            <meta name="keywords" content="bioinformatics, europe, institute"/> <!-- A few keywords that relate to the content of THIS PAGE (not the whol project) -->
            <meta name="author" content="EMBL-EBI"/>        <!-- Your [project-name] here -->

            <!-- Mobile viewport optimized: http://j.mp/bplateviewport -->
            <meta name="viewport" content="width=device-width,initial-scale=1"/>

            <!-- Place favicon.ico and apple-touch-icon.png in the root directory: http://mathiasbynens.be/notes/touch-icons -->
            <link rel="stylesheet" href="{$context-path}/assets/stylesheets/biostudies-colours.css" type="text/css"/>
            <link rel="stylesheet" href="{$context-path}/assets/stylesheets/font-awesome.min.css" type="text/css"/>
            <link rel="stylesheet" href="{$context-path}/assets/stylesheets/bs-common-1.0.150320.css" type="text/css"/>
            <link rel="stylesheet" href="{$context-path}/assets/stylesheets/bs-study-home-1.0.151126.css" type="text/css"/>
            <!-- CSS: implied media=all -->
            <!-- CSS concatenated and minified via ant build script-->


            <xsl:copy-of select="$pExtraCode"/>
            <!-- end CSS-->

            <!-- All JavaScript at the bottom, except for Modernizr / Respond.
                Modernizr enables HTML5 elements & feature detects; Respond is a polyfill for min/max-width CSS3 Media Queries
                For optimal performance, use a custom Modernizr build: http://www.modernizr.com/download/ -->

            <!-- Full build -->
            <!-- <script src="../js/libs/modernizr.minified.2.1.6.js"></script> -->

            <!-- custom build (lacks most of the "advanced" HTML5 support -->
            <script src="{$context-path}/assets/scripts/jquery-1.8.2.min.js"></script>
            <script src="//www.ebi.ac.uk/web_guidelines/js/libs/modernizr.custom.49274.js"/>

            <link rel="stylesheet" href="//www.ebi.ac.uk/web_guidelines/EBI-Framework/v1.1/libraries/foundation-6/css/foundation.css" type="text/css"/>
            <link rel="stylesheet" href="//www.ebi.ac.uk/web_guidelines/EBI-Framework/v1.1/css/ebi-global.css" type="text/css"/>
            <link rel="stylesheet" href="//www.ebi.ac.uk/web_guidelines/EBI-Icon-fonts/v1.1/fonts.css" type="text/css"/>
            <!--<link rel="stylesheet" href="//www.ebi.ac.uk/web_guidelines/EBI-Framework/v1.1/css/theme-embl-petrol.css" type="text/css"/>-->


            <script defer="defer" src="//www.ebi.ac.uk/web_guidelines/EBI-Framework/v1.1/js/cookiebanner.js"></script>
            <script defer="defer" src="//www.ebi.ac.uk/web_guidelines/EBI-Framework/v1.1/js/foot.js"></script>
            <script defer="defer" src="//www.ebi.ac.uk/web_guidelines/EBI-Framework/v1.1/js/script.js"></script>
            <script defer="defer" src="//www.ebi.ac.uk/web_guidelines/EBI-Framework/v1.1/libraries/foundation-6/js/foundation.js"></script>
            <script defer="defer" src="//www.ebi.ac.uk/web_guidelines/EBI-Framework/v1.1/js/foundationExtendEBI.js"></script>
            <script defer="defer" src="{$context-path}/assets/scripts/jquery.cookie-1.0.js"/>
            <script defer="defer" src="{$context-path}/assets/scripts/jquery.caret-range-1.0.js"/>
            <script defer="defer" src="{$context-path}/assets/scripts/jquery.autocomplete-1.1.0.150319.js"/>
            <script defer="defer" src="{$context-path}/assets/scripts/jquery.common-1.0.150220.js"/>

        </head>
    </xsl:template>

    <xsl:template name="ae-page-body">
        <xsl:param name="pIsSearchVisible"/>
        <xsl:param name="pExtraSearchFields"/>
        <xsl:param name="pBreadcrumbTrail"/>
        <xsl:param name="pEBISearchWidget"/>
        <xsl:param name="pExtraCode"/>
        <xsl:param name="pExtraBodyClasses"/>

        <body>   <!-- add any of your classes or IDs -->
            <xsl:attribute name="class">
                <xsl:text>level2 local-search</xsl:text>
                <xsl:if test="$pExtraBodyClasses != ''">
                    <xsl:text> </xsl:text><xsl:value-of select="$pExtraBodyClasses"/>
                </xsl:if>
            </xsl:attribute>

            <div id="skip-to">
                <ul>
                    <li><a href="#content" title="">Skip to main content</a></li>
                    <li><a href="#local-nav" title="">Skip to local navigation</a></li>
                    <li><a href="#global-nav" title="">Skip to EBI global navigation menu</a></li>
                    <li><a href="#global-nav-expanded" title="">Skip to expanded EBI global navigation menu (includes all sub-sections)</a></li>
                </ul>
            </div>

            <div id="wrapper" class="container_24">
                <header>
                    <div data-sticky-container="">
                    <div data-btm-anchor="300000" data-top-anchor="180" data-sticky-on="large" data-sticky="true" id="local-masthead" class="meta-background-image">
                    <div id="global-masthead" class="clearfix">
                        <!--This has to be one line and no newline characters-->
                        <a href="//www.ebi.ac.uk/" title="Go to the EMBL-EBI homepage"><span class="ebi-logo"></span></a>
                        <nav>
                            <div class="row">
                                <ul id="global-nav" class="menu">
                                    <!-- set active class as appropriate -->
                                    <li id="home-mobile" class=""><a href="//www.ebi.ac.uk"></a></li>
                                    <li id="home" class="active"><a href="//www.ebi.ac.uk"><i class="icon icon-generic" data-icon="H"></i> EMBL-EBI</a></li>
                                    <li id="services"><a href="//www.ebi.ac.uk/services"><i class="icon icon-generic" data-icon="("></i> Services</a></li>
                                    <li id="research"><a href="//www.ebi.ac.uk/research"><i class="icon icon-generic" data-icon=")"></i> Research</a></li>
                                    <li id="training"><a href="//www.ebi.ac.uk/training"><i class="icon icon-generic" data-icon="t"></i> Training</a></li>
                                    <li id="about"><a href="//www.ebi.ac.uk/about"><i class="icon icon-generic" data-icon="i"></i> About us</a></li>
                                    <li id="search">
                                        <a href="#" data-toggle="search-global-dropdown"><i class="icon icon-functional" data-icon="1"></i> <span class="show-for-small-only">Search</span></a>
                                        <div id="search-global-dropdown" class="dropdown-pane" data-dropdown="" data-options="closeOnClick:true;">
                                            <form id="global-search" name="global-search" action="/ebisearch/search.ebi" method="GET">
                                                <fieldset>
                                                    <div class="input-group">
                                                        <input type="text" name="query" id="global-searchbox" class="input-group-field" placeholder="Search all of EMBL-EBI"/>
                                                        <div class="input-group-button">
                                                            <input type="submit" name="submit" value="Search" class="button"/>
                                                            <input type="hidden" name="db" value="allebi" checked="checked"/>
                                                            <input type="hidden" name="requestFrom" value="global-masthead" checked="checked"/>
                                                        </div>
                                                    </div>
                                                </fieldset>
                                            </form>
                                        </div>
                                    </li>
                                    <li class="float-right show-for-medium embl-selector">
                                        <button class="button" type="button" data-toggle="embl-dropdown">Hinxton</button>
                                        <div id="embl-dropdown" class="dropdown-pane" data-dropdown="" data-options="closeOnClick:true;">
                                            to come.
                                        </div>
                                    </li>
                                </ul>
                            </div>
                        </nav>

                    </div>
                    <!-- local-title -->
                    <div class="masthead row">
                        <div id="local-title" >
                            <xsl:attribute name="class">logo-title columns medium-6<xsl:if test="$pIsSearchVisible"> grid_12 alpha</xsl:if></xsl:attribute>
                            <!-- <img class="svg" src="{$context-path}/assets/images/ae-logo-64.svg" width="64" height="64" alt="AE"/> -->
                            <a href="{$context-path}/" title="Back to BioStudies homepage"><img src="{$context-path}/assets/images/logo.png"/></a>
                        </div>

                        <!-- /local-title -->
                        <!-- local-search -->
                        <!-- NB: if you do not have a local-search, delete the following div, and drop the class="grid_12 alpha" class from local-title above -->
                        <xsl:if test="$pIsSearchVisible">
                            <div class="columns medium-6 last">
                                    <form id="local-search" name="local-search"  action="{$context-path}/search" method="get">
                                        <fieldset>
                                            <div class="input-group">

                                                <label>
                                                    <input type="search" name="query" id="local-searchbox">
                                                        <xsl:if test="$keywords != ''">
                                                            <xsl:attribute name="value" select="$keywords"/>
                                                        </xsl:if>
                                                    </input>
                                                <!-- Include some example searchterms - keep them short and few! -->
                                                    <p class="examples">Examples: <a href="#">hyperplasia</a>,
                                                        <a href="#">PMC516016</a>
                                                    </p>
                                                </label>
                                                <xsl:if test="$project!=''">
                                                    <p class="projectcheck">
                                                        <input type="checkbox" checked="true" id="search-in-project" class="do-not-clear"/>Search in <xsl:value-of select="$project"/> only
                                                        <input type="hidden" id="project" value="{$project}" />
                                                    </p>
                                                </xsl:if>
                                                <div class="input-group-button">
                                                    <input type="submit" value="Search" class="submit button secondary"/>
                                                    <div class="small">
                                                    <!-- If your search is more complex than just a keyword search, you can link to an Advanced Search,
                                                   with whatever features you want available -->
                                                    <!--
                                                    <span class="adv"><a href="{$context-path}/help/how_to_search.html#AdvancedSearchExperiment" id="adv-search" title="Advanced">Advanced</a></span>
                                                    -->
                                        </div></div></div>
                                        </fieldset>
                                        <xsl:copy-of select="$pExtraSearchFields"/>
                                    </form>
                             </div>

                        </xsl:if>
                            <!-- /local-search -->
                            <!-- local-nav -->
                            <nav>
                                    <ul class="grid_24 dropdown menu float-left columns medium-12" data-dropdown-menu="true" id="local-nav">
                                        <li>
                                            <xsl:attribute name="class">first<xsl:if test="$relative-uri = '/'"> active</xsl:if></xsl:attribute>
                                            <a class="inner" href="{$context-path}/" title="BioStudies ${project.version}.${buildNumber}">
                                                Home
                                            </a>
                                        </li>
                                        <li>
                                            <xsl:if test="fn:starts-with($relative-uri, '/studies/')"><xsl:attribute name="class">active</xsl:attribute></xsl:if>
                                            <a class="inner" href="{$context-path}/studies/" title="Browse BioStudies">Browse</a>
                                        </li>
                                        <li>
                                            <a class="inner" target="_blank" href="{$context-path}/submissions/" title="Submit a study">Submit a study</a>
                                        </li>
                                        <li>
                                            <xsl:if test="fn:starts-with($relative-uri, '/help/')"><xsl:attribute name="class">active</xsl:attribute></xsl:if>
                                            <a class="inner" href="{$context-path}/help/index.html" title="BioStudies Help">Help</a>
                                        </li>
                                        <li class="last">
                                            <xsl:if test="$relative-uri = '/about.html'"><xsl:attribute name="class">active</xsl:attribute></xsl:if>
                                            <a class="inner" href="{$context-path}/about.html">About BioStudies</a>
                                        </li>
                                        <!-- If you need to include functional (as opposed to purely navigational) links in your local menu,
                                         add them here, and give them a class of "functional". Remember: you'll need a class of "last" for
                                         whichever one will show up last...
                                         For example: -->
                                        <li class="functional float-right last">
                                            <a  href="#" class="inner icon icon-functional login" data-icon="l">
                                                <xsl:choose>
                                                    <xsl:when test="$username">Logout [<xsl:value-of select="$username"/>]</xsl:when>
                                                    <xsl:otherwise>Login</xsl:otherwise>
                                                </xsl:choose>
                                            </a>
                                        </li>
                                        <li class="inner functional first float-right">
                                            <a  href="#" class="icon icon-static feedback" data-icon="\">Feedback</a>
                                        </li>
                                        <!--
                                        <li class="functional"><a href="#" class="icon icon-functional" data-icon="r">Share</a></li>
                                        -->
                                    </ul>

                            </nav>

                            <!-- /local-nav -->
                        </div>
                    </div>
                    <xsl:if test="$project">
                        <div class="project-banner clearfix">
                            <span class="project-banner-content">
                                <div class="project-logo">
                                    <a class="no-border" href="{$project-url}" target="_blank">
                                        <img src="{$context-path}/files/{lower-case($project)}/logo.png"/></a>
                                </div>
                                <div class="project-text">
                                    <span class="project-description"><xsl:value-of select="$project-description"/></span>
                                </div>
                            </span>
                        </div>
                    </xsl:if>
            </div>
            </header>

                <div id="content" role="main" class="columns medium-12  clearfix row">
                    <!-- If you require a breadcrumb trail, its root should be your service.
     	                 You don't need a breadcrumb trail on the homepage of your service... -->
                    <xsl:if test="$pBreadcrumbTrail != ''">
                        <section>
                            <xsl:if test="$pEBISearchWidget">
                                <xsl:attribute name="class" select="'grid_18 alpha'"/>
                            </xsl:if>
                            <nav id="breadcrumb">
                                <p>
                                    <a href="{$context-path}/">BioStudies</a>
                                    &gt;
                                    <xsl:copy-of select="$pBreadcrumbTrail"/>
                                </p>
                            </nav>
                        </section>
                        <xsl:copy-of select="$pEBISearchWidget"/>
                    </xsl:if>

                    <xsl:call-template name="bs-content-section"/>
                    <section id="ae-feedback" style="display:none">
                        <h3>Have your say<a id="ae-feedback-close" href="#" class="icon icon-functional" data-icon="x"/></h3>
                        <form method="post" action="#" onsubmit="return false">
                            <fieldset class="callout">
                                <label for="ae-feedback-message">We value your feedback. Please leave your comment below.</label>
                                <textarea id="ae-feedback-message" name="m"/>
                            </fieldset>
                            <fieldset class="callout">
                                <label for="ae-email-field">Optionally please enter your email address if you wish to get a response.<br/>We will never share this address with anyone else.</label>
                                <input id="ae-email-field" name="e" maxlength="50"/>
                            </fieldset>
                            <input type="hidden" name="p" value="{$host}{$context-path}{$relative-uri}{if ($query-string) then fn:concat('?', $query-string) else ''}"/>
                            <input type="hidden" name="r" value="{$host}{$context-path}{$relative-referer}"/>
                            <input class="submit button" type="submit" value="Send"/>
                        </form>
                    </section>
                    <section id="ae-login" style="display:none">
                        <h3>BioStudies submitter/reviewer login<a id="ae-login-close" href="#" class="icon icon-functional" data-icon="x"/></h3>
                        <form id="ae-login-form" method="post" action="{$secure-host}{$context-path}/auth">
                            <fieldset class="callout">
                                <label for="ae-user-field">User name</label>
                                <input id="ae-user-field" name="u" maxlength="50"/>
                            </fieldset>
                            <fieldset class="callout">
                                <label for="ae-pass-field">Password</label>
                                <input id="ae-pass-field" type="password" name="p" maxlength="50"/>
                            </fieldset>
                            <span id="ae-login-remember-option">
                                <input id="ae-login-remember" name="r" type="checkbox"/>
                                <label for="ae-login-remember">Remember me</label>
                            </span>
                            <input class="submit button" type="submit" value="Login"/>
                            <div class="ae-login-status" style="display:none">
                            <span class="alert"></span>
                            </div>
                            <!--div id="ae-login-forgot"><a href="#">Forgot user name or password?</a></div-->
                        </form>
                        <form id="ae-logout-form" method="post" action="{$secure-host}{$context-path}/logout">
                        </form>
                        <form id="ae-forgot-form" method="post" action="{$secure-host}{$context-path}/auth">
                            <fieldset class="callout">
                                <label for="ae-name-email-field">User name or email address</label>
                                <input id="ae-name-email-field" name="e" maxlength="50"/>
                            </fieldset>
                            <fieldset class="callout">
                                <label for="ae-accession-field">Experiment accession associated with the account</label>
                                <input id="ae-accession-field" name="a" maxlength="14"/>
                            </fieldset>
                            <div>We will send you a reminder with your account information</div>
                            <div class="ae-login-status" style="display:none">
                            <span class="alert"></span>
                            </div>
                            <input class="submit button" type="submit" value="Send"/>
                        </form>
                    </section>
                </div>
                <footer>
                    <!-- Optional local footer (insert citation / project-specific copyright / etc here -->
                    <!--
                    <div id="local-footer" class="grid_24 clearfix">
                            <p>How to reference this page: ...</p>
                        </div>
                    -->
                    <!-- End optional local footer -->
                    <div id="global-footer" class="columns medium-12">
                        <nav id="global-nav-expanded" class="row">
                            <div class="columns small-6 medium-2">
                                <a href="//www.ebi.ac.uk/" title="EMBL-EBI"><span class="ebi-logo"></span></a>
                            </div>
                            <div class="columns small-6 medium-2">
                                <h5 class="services"><a class="services-color" href="//www.ebi.ac.uk/services">Services</a></h5>
                                <ul>
                                    <li class="first"><a href="//www.ebi.ac.uk/services">By topic</a></li>
                                    <li><a href="//www.ebi.ac.uk/services/all">By name (A-Z)</a></li>
                                    <li class="last"><a href="//www.ebi.ac.uk/support">Help &amp; Support</a></li>
                                </ul>
                            </div>
                            <div class="columns small-6 medium-2">
                                <h5 class="research"><a class="research-color" href="//www.ebi.ac.uk/research">Research</a></h5>
                                <ul>
                                    <li><a href="//www.ebi.ac.uk/research/publications">Publications</a></li>
                                    <li><a href="//www.ebi.ac.uk/research/groups">Research groups</a></li>
                                    <li class="last"><a href="//www.ebi.ac.uk/research/postdocs">Postdocs</a> &amp; <a href="//www.ebi.ac.uk/research/eipp">PhDs</a></li>
                                </ul>
                            </div>
                            <div class="columns small-6 medium-2">
                                <h5 class="training"><a class="training-color" href="//www.ebi.ac.uk/training">Training</a></h5>
                                <ul>
                                    <li><a href="//www.ebi.ac.uk/training/handson">Train at EBI</a></li>
                                    <li><a href="//www.ebi.ac.uk/training/roadshow">Train outside EBI</a></li>
                                    <li><a href="//www.ebi.ac.uk/training/online">Train online</a></li>
                                    <li class="last"><a href="//www.ebi.ac.uk/training/contact-us">Contact organisers</a></li>
                                </ul>
                            </div>
                            <div class="columns small-6 medium-2">
                                <h5 class="industry"><a class="industry-color" href="//www.ebi.ac.uk/industry">Industry</a></h5>
                                <ul>
                                    <li><a href="//www.ebi.ac.uk/industry/private">Members Area</a></li>
                                    <li><a href="//www.ebi.ac.uk/industry/workshops">Workshops</a></li>
                                    <li><a href="//www.ebi.ac.uk/industry/sme-forum"><abbr title="Small Medium Enterprise">SME</abbr> Forum</a></li>
                                    <li class="last"><a href="//www.ebi.ac.uk/industry/contact">Contact Industry programme</a></li>
                                </ul>
                            </div>
                            <div class="columns small-6 medium-2">
                                <h5 class="about"><a class="ebi-color" href="//www.ebi.ac.uk/about">About EMBL-EBI</a></h5>
                                <ul>
                                    <li><a href="//www.ebi.ac.uk/about/leadership">Leadership</a></li>
                                    <li><a href="//www.ebi.ac.uk/about/funding">Funding</a></li>
                                    <li><a href="//www.ebi.ac.uk/about/jobs" title="Jobs, postdocs, PhDs...">Jobs</a></li>
                                    <li><a href="//www.ebi.ac.uk/about/people">People &amp; groups</a></li>
                                    <li><a href="//www.ebi.ac.uk/about/travel">Visit us</a></li>
                                </ul>
                            </div>
                        </nav>
                        <section id="ebi-footer-meta" class="row">
                            <p class="address">EMBL-EBI, Wellcome Trust Genome Campus, Hinxton, Cambridgeshire, CB10 1SD, UK &#160; &#160; +44 (0)1223 49 44 44</p>
                            <p class="legal">Copyright &#169; EMBL-EBI 2015 | EBI is an Outstation of the
                                <a href="http://www.embl.org">European Molecular Biology Laboratory</a>
                                |
                                <!--<a href="/about/privacy">Privacy</a>-->
                                <!--|-->
                                <!--<a href="/about/cookies">Cookies</a>-->
                                <!--|-->
                                <a href="/about/terms-of-use">Terms of use</a>
                            </p>
                        </section>
                    </div>
                </footer>
            </div>  <!--! end of #wrapper -->

            <script defer="defer" src="//www.ebi.ac.uk/web_guidelines/js/cookiebanner.js"/>
            <!--script defer="defer" src="http://www.ebi.ac.uk/web_guidelines/js/foot.js"/ -->
            <script><![CDATA[var contextPath = "]]><xsl:value-of select="$context-path"/><![CDATA[";]]>
            </script>


            <xsl:copy-of select="$pExtraCode"/>
            ${interface.application.google.analytics}
            <div class="floating-panel"></div>
        </body>
    </xsl:template>

    <xsl:template name="add-table-sort">
        <xsl:param name="pKind"/>
        <xsl:param name="pSortBy"/>
        <xsl:param name="pSortOrder"/>
        <xsl:if test="$pKind = $pSortBy">
            <xsl:choose>
                <xsl:when test="fn:starts-with($pSortOrder, 'a')">
                    <i class="aw-icon-angle-up"/>
                </xsl:when>
                <xsl:otherwise>
                    <i class="aw-icon-angle-down"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
    </xsl:template>

    <xsl:function name="ae:setQSParam" as="xs:string">
        <xsl:param name="pQueryString" as="xs:string"/>
        <xsl:param name="pParamName" as="xs:string"/>
        <xsl:param name="pParamValue" as="xs:string"/>

        <xsl:choose>
            <xsl:when test="fn:matches($pQueryString, fn:concat('(^|&amp;)', $pParamName, '='))">
                <xsl:value-of select="fn:replace($pQueryString, fn:concat('(^|&amp;)(', $pParamName, '=)([^&amp;]+)'), fn:concat('$1$2', $pParamValue))"/>
            </xsl:when>
            <xsl:when test="fn:string-length($pQueryString) > 0">
                <xsl:value-of select="fn:concat($pQueryString, '&amp;', $pParamName,'=', $pParamValue)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="fn:concat($pParamName, '=', $pParamValue)"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:template name="table-page-size">
        <xsl:param name="pCurrentPageSize" as="xs:integer"/>
        <xsl:param name="pTotal" as="xs:integer"/>
        <xsl:param name="pPageParam" as="xs:string"/>
        <xsl:param name="pPageSizeParam" as="xs:string"/>
        <div class="ae-page-size">
            <xsl:choose>
                <xsl:when test="$pTotal > 25">
                    <xsl:variable name="vPageSizes" select="25, 50, 100, 250, 500"/>
                    <xsl:text>Page size </xsl:text>
                    <xsl:for-each select="$vPageSizes">
                        <xsl:choose>
                            <xsl:when test="fn:current() = $pCurrentPageSize">
                                <span><xsl:value-of select="."/></span>
                            </xsl:when>
                            <xsl:otherwise>
                                <a href="{$context-path}{$relative-uri}?{ae:setQSParam(ae:setQSParam($query-string, $pPageParam, '1'), $pPageSizeParam, fn:string(fn:current()))}">
                                    <xsl:value-of select="fn:current()"/>
                                </a>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:for-each>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>&#160;</xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </div>
    </xsl:template>

    <xsl:template name="table-pager-pages">
        <xsl:param name="pTotal" as="xs:integer"/>
        <xsl:param name="pPage" as="xs:integer"/>
        <xsl:param name="pPageSize" as="xs:integer"/>
        <xsl:param name="pPageParam" as="xs:string"/>
        <xsl:param name="pPageSizeParam" as="xs:string"/>

        <div class="ae-pager">
            <xsl:choose>
                <xsl:when test="$pTotal > $pPageSize">
                    <xsl:variable name="vTotalPages" select="(fn:floor( ( $pTotal - 1 ) div $pPageSize ) + 1) cast as xs:integer" as="xs:integer"/>

                    <xsl:text>Page </xsl:text>
                    <xsl:call-template name="table-pager-page">
                        <xsl:with-param name="pPage" select="1"/>
                        <xsl:with-param name="pCurrentPage" select="$pPage"/>
                        <xsl:with-param name="pPageSize" select="$pPageSize"/>
                        <xsl:with-param name="pTotalPages" select="$vTotalPages"/>
                        <xsl:with-param name="pPageParam" select="$pPageParam"/>
                        <xsl:with-param name="pPageSizeParam" select="$pPageSizeParam"/>
                    </xsl:call-template>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>&#160;</xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </div>
    </xsl:template>

    <xsl:template name="table-pager-page">
        <xsl:param name="pPage" as="xs:integer"/>
        <xsl:param name="pCurrentPage" as="xs:integer"/>
        <xsl:param name="pPageSize" as="xs:integer"/>
        <xsl:param name="pTotalPages" as="xs:integer"/>
        <xsl:param name="pPageParam" as="xs:string"/>
        <xsl:param name="pPageSizeParam" as="xs:string"/>

        <xsl:if test="$pPage &lt;= $pTotalPages">

            <xsl:choose>
                <xsl:when test="$pPage = $pCurrentPage">
                    <span><xsl:value-of select="$pPage"/></span>
                </xsl:when>
                <xsl:when test="($pPage = 2) and ($pCurrentPage > 4) and ($pTotalPages > 8)">
                    <xsl:text>..</xsl:text>
                </xsl:when>
                <xsl:when test="($pPage = ($pTotalPages - 1)) and (($pTotalPages - $pCurrentPage) > 3) and ($pTotalPages > 8)">
                    <xsl:text>..</xsl:text>
                </xsl:when>
                <xsl:when test="($pPage = 1) or (($pPage &lt; 7) and ($pCurrentPage &lt; 4)) or (fn:abs($pPage - $pCurrentPage) &lt; 3)">
                    <a href="{$context-path}{$relative-uri}?{ae:setQSParam(ae:setQSParam($query-string, $pPageParam, fn:string($pPage)), $pPageSizeParam, fn:string($pPageSize))}">
                        <xsl:value-of select="$pPage"/>
                    </a>
                </xsl:when>
                <xsl:when test="((($pTotalPages - $pCurrentPage) &lt; 2) and ($pTotalPages - $pPage &lt; 6)) or ($pPage = $pTotalPages) or ($pTotalPages &lt;= 6)">
                    <a href="{$context-path}{$relative-uri}?{ae:setQSParam(ae:setQSParam($query-string, $pPageParam, fn:string($pPage)), $pPageSizeParam, fn:string($pPageSize))}">
                        <xsl:value-of select="$pPage"/>
                    </a>
                </xsl:when>
            </xsl:choose>

            <xsl:if test="$pPage &lt; $pTotalPages">
                <xsl:call-template name="table-pager-page">
                    <xsl:with-param name="pPage" select="$pPage + 1"/>
                    <xsl:with-param name="pCurrentPage" select="$pCurrentPage"/>
                    <xsl:with-param name="pPageSize" select="$pPageSize"/>
                    <xsl:with-param name="pTotalPages" select="$pTotalPages"/>
                    <xsl:with-param name="pPageParam" select="$pPageParam"/>
                    <xsl:with-param name="pPageSizeParam" select="$pPageSizeParam"/>
                </xsl:call-template>
            </xsl:if>
        </xsl:if>

    </xsl:template>

    <xsl:template name="table-pager">
        <xsl:param name="pColumnsToSpan" as="xs:integer"/>
        <xsl:param name="pName" as="xs:string"/>
        <xsl:param name="pParamPrefix" as="xs:string" select="''"/>
        <xsl:param name="pTotal" as="xs:integer"/>
        <xsl:param name="pPage" as="xs:integer"/>
        <xsl:param name="pPageSize" as="xs:integer"/>

        <xsl:variable name="vPageParam" select="fn:concat($pParamPrefix, 'page')"/>
        <xsl:variable name="vPageSizeParam" select="fn:concat($pParamPrefix, 'pagesize')"/>

        <xsl:variable name="vFrom" as="xs:integer">
            <xsl:choose>
                <xsl:when test="$pPage > 0"><xsl:value-of select="1 + ( $pPage - 1 ) * $pPageSize"/></xsl:when>
                <xsl:when test="$pTotal = 0">0</xsl:when>
                <xsl:otherwise>1</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="vTo" as="xs:integer">
            <xsl:choose>
                <xsl:when test="( $vFrom + $pPageSize - 1 ) > $pTotal"><xsl:value-of select="$pTotal"/></xsl:when>
                <xsl:otherwise><xsl:value-of select="$vFrom + $pPageSize - 1"/></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <div colspan="{$pColumnsToSpan}" class="col_pager">
            <xsl:call-template name="table-pager-pages">
                <xsl:with-param name="pPage" select="$pPage"/>
                <xsl:with-param name="pPageSize" select="$pPageSize"/>
                <xsl:with-param name="pTotal" select="$pTotal"/>
                <xsl:with-param name="pPageParam" select="$vPageParam"/>
                <xsl:with-param name="pPageSizeParam" select="$vPageSizeParam"/>
            </xsl:call-template>
            <div class="ae-stats">
                <xsl:if test="$pTotal > $pPageSize">
                    <xsl:text>Showing </xsl:text>
                    <span>
                        <xsl:value-of select="$vFrom"/>
                        <xsl:text> - </xsl:text>
                        <xsl:value-of select="$vTo"/>
                    </span>
                    <xsl:text> of </xsl:text>
                </xsl:if>
                <span><xsl:value-of select="$pTotal"/></span>
                <xsl:value-of select="fn:concat(' ', $pName)"/>
            </div>
            <xsl:call-template name="table-page-size">
                <xsl:with-param name="pCurrentPageSize" select="$pPageSize"/>
                <xsl:with-param name="pTotal" select="$pTotal"/>
                <xsl:with-param name="pPageParam" select="$vPageParam"/>
                <xsl:with-param name="pPageSizeParam" select="$vPageSizeParam"/>
            </xsl:call-template>
        </div>
    </xsl:template>

</xsl:stylesheet>