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


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostudies.components.Studies;
import uk.ac.ebi.biostudies.components.Thumbnails;
import uk.ac.ebi.microarray.biostudies.shared.auth.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ThumbnailServlet extends AuthAwareApplicationServlet {

    private static final long serialVersionUID = 3061219919204683614L;
    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    protected boolean canAcceptRequest(HttpServletRequest request, RequestType requestType) {
        return (requestType == RequestType.GET || requestType == RequestType.POST);
    }

    @Override
    protected void doAuthenticatedRequest(HttpServletRequest request, HttpServletResponse response, RequestType requestType, User authenticatedUser) throws ServletException, IOException {
        logRequest(logger, request, requestType);
        String accession = "";
        String name = "";

        String[] requestArgs = request.getPathInfo().replaceFirst("^/", "").split("/");
        if (1 == requestArgs.length) { // name only passed
            name = requestArgs[0];
        } else if (requestArgs.length>1) { // accession/name passed
            accession = requestArgs[0];
            name = StringUtils.replace(request.getPathInfo().substring(accession.length()+2), "..", "");
        }
        logger.info("Requested thumbnail of [" + name + "], accession [" + accession + "]");

        Studies studies = getComponent(Studies.class);
        String relativePath = null;
        try {
            relativePath = studies.getRelativePath(accession, authenticatedUser);
        } catch (Exception e) {
            throw new ServletException(e);
        }
        Thumbnails thumbnails = getComponent(Thumbnails.class);
        if (relativePath==null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
                throw new IOException("File [" + name + "], accession [" + accession + "] is not present");
        }
        try {
            thumbnails.sendThumbnail(response, relativePath, name);
        } catch (Exception ex) {
            logger.warn("Could not generate thumbnail. User might have moved their mouse too fast. "+ ex.getMessage());
        }



    }


}