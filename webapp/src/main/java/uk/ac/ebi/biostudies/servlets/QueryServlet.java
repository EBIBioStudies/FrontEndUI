/*
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
 */

package uk.ac.ebi.biostudies.servlets;


import net.sf.saxon.trans.XPathException;
import org.apache.lucene.queryparser.classic.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostudies.components.Files;
import uk.ac.ebi.biostudies.components.SaxonEngine;
import uk.ac.ebi.biostudies.components.SearchEngine;
import uk.ac.ebi.biostudies.utils.AccessType;
import uk.ac.ebi.biostudies.utils.HttpServletRequestParameterMap;
import uk.ac.ebi.biostudies.utils.RegexHelper;
import uk.ac.ebi.biostudies.utils.saxon.SaxonException;
import uk.ac.ebi.fg.saxon.functions.HTTPStatusException;
import uk.ac.ebi.microarray.biostudies.shared.auth.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class QueryServlet extends AuthAwareApplicationServlet {
    private static final long serialVersionUID = 6806580383145704364L;

    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected boolean canAcceptRequest(HttpServletRequest request, RequestType requestType) {
        return (requestType == RequestType.GET || requestType == RequestType.POST);
    }

    @Override
    protected void doAuthenticatedRequest(
            HttpServletRequest request
            , HttpServletResponse response
            , RequestType requestType
            , User authenticatedUser
    ) throws ServletException, IOException {
        RegexHelper PARSE_ARGUMENTS_REGEX = new RegexHelper("/([^/]+)/([^/]+)/([^/]+)$", "i");

        logRequest(logger, request, requestType);

        String[] requestArgs = PARSE_ARGUMENTS_REGEX.match(request.getPathInfo());

        if (null == requestArgs || requestArgs.length != 3
                || "".equals(requestArgs[0]) || "".equals(requestArgs[1]) || "".equals(requestArgs[2])) {
            throw new ServletException("Bad arguments passed via request URL [" + request.getRequestURL().toString() + "]");
        }

        String index = requestArgs[0];
        String stylesheet = requestArgs[1];
        String outputType = requestArgs[2];


        if ("xls".equals(outputType)) {
            // special case for Excel docs
            // we actually send tab-delimited file but mimick it as XLS doc
            String timestamp = new SimpleDateFormat("yyMMdd-HHmmss").format(new Date());
            response.setContentType("application/vnd.ms-excel; charset=ISO-8859-1");
            response.setHeader("Content-disposition", "attachment; filename=\"ArrayExpress-Experiments-" + timestamp + ".xls\"");
            outputType = "tab";
        } else if ("tab".equals(outputType)) {
            // special case for tab-delimited files
            // we send tab-delimited file as an attachment
            String timestamp = new SimpleDateFormat("yyMMdd-HHmmss").format(new Date());
            response.setContentType("text/plain; charset=ISO-8859-1");
            response.setHeader("Content-disposition", "attachment; filename=\"ArrayExpress-Experiments-" + timestamp + ".txt\"");
            outputType = "tab";
        } else if ("json".equals(outputType)) {
            response.setContentType("application/json; charset=UTF-8");
        } else {
            response.setContentType("text/" + outputType + "; charset=UTF-8");
        }

        // tell client to not cache the page unless we want to
        if (!"true".equalsIgnoreCase(request.getParameter("cache"))) {
            response.addHeader("Pragma", "no-cache");
            response.addHeader("Cache-Control", "no-cache");
            response.addHeader("Cache-Control", "must-revalidate");
            response.addHeader("Expires", "Fri, 16 May 2008 10:00:00 GMT"); // some date in the past
        }

        // flushing buffer to output headers; should only be used for looooong operations to mitigate proxy timeouts
        // because it disallows sending errors like 404 and alike
        if (null != request.getParameter("flusheaders")) {
            response.flushBuffer();
        }

        // Output goes to the response PrintWriter.
        try (PrintWriter out = response.getWriter()) {
            String stylesheetName = ("-".equals(index) ? "" : index + "-")
                    + stylesheet + "-" + outputType + ".xsl";
            HttpServletRequestParameterMap params = getParameterMap(request, authenticatedUser);
            if(stylesheetName.toLowerCase().contains("hecatos") || params.containsKey("facets"))
                params.put("chkfacets", "");
            try {
                SearchEngine search = getComponent(SearchEngine.class);
                SaxonEngine saxonEngine = getComponent(SaxonEngine.class);

                if (!search.getController().hasIndexDefined(index)) // default to studies index to show total
                    index = "studies";
                Integer queryId = search.getController().addQuery(index, params);
                params.put("queryid", String.valueOf(queryId));
                Source source = search.getController().search(queryId);
                //System.out.println(saxonEngine.serializeDocument(source));
                if (!saxonEngine.transform(source, stylesheetName, params, new StreamResult(out))) {
                    throw new Exception("Transformation returned an error");
                }
            } catch (ParseException x) {
                logger.error("Caught lucene parse exception:", x);
                response.sendError(
                        HttpServletResponse.SC_BAD_REQUEST
                        , params.getString("keywords"));
            } catch (SaxonException x) {
                if (x.getCause() instanceof HTTPStatusException) {
                    HTTPStatusException xx = (HTTPStatusException) x.getCause();
                    logger.warn("ae:httpStatus({}) called from the transformation", xx.getStatusCode());
                    if (null != xx.getStatusCode() && xx.getStatusCode() > 200 && xx.getStatusCode() < 500) {
                        response.sendError(xx.getStatusCode());
                    } else {
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    }
                } else if (x.getCause() instanceof XPathException && x.getCause().getMessage().contains("Illegal HTML")) {
                    getApplication().handleException("[SEVERE] Runtime error while processing " + requestToString(request, requestType), x);
                    return;
                }
            }

        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    private HttpServletRequestParameterMap getParameterMap(HttpServletRequest request, User authenticatedUser) throws UnsupportedEncodingException {
        HttpServletRequestParameterMap params = new HttpServletRequestParameterMap(request);

        // to make sure nobody sneaks in the other value w/o proper authentication
        if (authenticatedUser!=null && authenticatedUser.isSuperUser()) {
            params.put("username", authenticatedUser.getUsername());
        } else if (authenticatedUser!=null) {
            params.put("username", authenticatedUser.getUsername());
            params.put(AccessType.ALLOW, authenticatedUser.getAllow());
            params.put(AccessType.DENY, authenticatedUser.getDeny());
        } else {
            params.put(AccessType.ALLOW, AccessType.PUBLIC_ACCESS);
        }

        // migration rudiment - show only "visible" i.e. overlapping experiments from AE2
        params.put("visible", "true");

        // add page-specific application constants
        if (request.getRequestURI().contains("/detail/")) {
            params.put("ftp-url", getComponent(Files.class).getFtpURL());
        }
        return params;
    }
}