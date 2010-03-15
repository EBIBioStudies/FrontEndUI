package uk.ac.ebi.arrayexpress.servlets;

/*
 * Copyright 2009-2010 Microarray Informatics Group, European Bioinformatics Institute
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress.app.ApplicationServlet;
import uk.ac.ebi.arrayexpress.components.Experiments;
import uk.ac.ebi.arrayexpress.utils.RegExpHelper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class LookupServlet extends ApplicationServlet
{
    // logging machinery
    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected boolean canAcceptRequest( HttpServletRequest request, RequestType requestType )
    {
        return (requestType == RequestType.GET || requestType == RequestType.POST);
    }

    // Respond to HTTP requests from browsers.
    protected void doRequest( HttpServletRequest request, HttpServletResponse response, RequestType requestType ) throws ServletException, IOException
    {
        logRequest(logger, request, requestType);

        String[] requestArgs = new RegExpHelper("servlets/lookup/([^/]+)", "i")
                .match(request.getRequestURL().toString());

        String type = "";
        String query = "";
        Integer limit = null;

        if (null != request.getParameter("q")) {
            query = request.getParameter("q");
        }

        if (null != request.getParameter("limit")) {
            limit = Integer.parseInt(request.getParameter("limit"));
        }

        if (null != requestArgs) {
            if (!requestArgs[0].equals("")) {
                type = requestArgs[0];
            }
        }

        // Set content type for HTML/XML/plain
        response.setContentType("text/plain; charset=ISO-8859-1");

        // Disable cache no matter what (or we're fucked on IE side)
        response.addHeader("Pragma", "no-cache");
        response.addHeader("Cache-Control", "no-cache");
        response.addHeader("Cache-Control", "must-revalidate");
        response.addHeader("Expires", "Fri, 16 May 2008 10:00:00 GMT"); // some date in the past

        // Output goes to the response PrintWriter.
        PrintWriter out = response.getWriter();
        Experiments experiments = (Experiments)getComponent("Experiments");
        if (type.equals("arrays")) {
            out.print(experiments.getArrays());
        } else if (type.equals("species")) {
            out.print(experiments.getSpecies());
        } else if (type.equals("expdesign")) {
            out.print(experiments.getAssaysByMolecule(query));
        } else if (type.equals("exptech")) {
            out.print(experiments.getAssaysByInstrument(query));
        } else if (type.equals("keywords")) {
            String field = (null != request.getParameter("field") ? request.getParameter("field") : "");
            out.print(experiments.getKeywords(query, field, limit));
        } else if (type.equals("efotree")) {
            String efoId = (null != request.getParameter("efoid") ? request.getParameter("efoid") : "");
            out.print(experiments.getEfoTree(efoId));
        }
    }
}

