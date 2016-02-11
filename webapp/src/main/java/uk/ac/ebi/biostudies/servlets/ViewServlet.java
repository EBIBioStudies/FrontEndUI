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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostudies.app.ApplicationServlet;
import uk.ac.ebi.biostudies.components.SaxonEngine;
import uk.ac.ebi.biostudies.utils.HttpServletRequestParameterMap;
import uk.ac.ebi.biostudies.utils.RegexHelper;
import uk.ac.ebi.biostudies.utils.saxon.SaxonException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.PrintWriter;

public class ViewServlet extends ApplicationServlet {
    private static final long serialVersionUID = 6806580383145704364L;

    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected boolean canAcceptRequest(HttpServletRequest request, RequestType requestType) {
        return (requestType == RequestType.GET || requestType == RequestType.POST);
    }

    @Override
    protected void doRequest(HttpServletRequest request, HttpServletResponse response, RequestType requestType) throws ServletException, IOException {
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


        // tell client to not cache the page unless we want to
        if (!"true".equalsIgnoreCase(request.getParameter("cache"))) {
            response.addHeader("Pragma", "no-cache");
            response.addHeader("Cache-Control", "no-cache");
            response.addHeader("Cache-Control", "must-revalidate");
            response.addHeader("Expires", "Fri, 16 May 2000 10:00:00 GMT"); // some date in the past
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

            HttpServletRequestParameterMap params = new HttpServletRequestParameterMap(request);

            try {
                SaxonEngine saxonEngine = getComponent(SaxonEngine.class);

                if (!saxonEngine.transform(saxonEngine.buildDocument("<dummyRoot/>"), stylesheetName, params, new StreamResult(out))) {
                    throw new Exception("Transformation returned an error");
                }
            } catch (SaxonException x) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }
}