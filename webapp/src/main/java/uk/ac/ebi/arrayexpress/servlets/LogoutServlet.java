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

package uk.ac.ebi.arrayexpress.servlets;

import net.sf.saxon.om.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress.components.SaxonEngine;
import uk.ac.ebi.arrayexpress.components.Users;
import uk.ac.ebi.arrayexpress.utils.HttpServletRequestParameterMap;
import uk.ac.ebi.arrayexpress.utils.HttpTools;
import uk.ac.ebi.microarray.arrayexpress.shared.auth.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.PrintWriter;

public class LogoutServlet extends AuthAwareApplicationServlet {


    private static final long serialVersionUID = 7914844673011547649L;
    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    protected boolean canAcceptRequest(HttpServletRequest request, RequestType requestType) {
        return true;
    }

    protected void doAuthenticatedRequest(
            HttpServletRequest request
            , HttpServletResponse response
            , RequestType requestType
            , User authenticatedUser
    ) throws ServletException, IOException {
        logRequest(logger, request, requestType);
        Users users = getComponent(Users.class);
        users.logout(authenticatedUser.getUsername());
        HttpTools.setCookie(response, HttpTools.AE_USERNAME_COOKIE, null, 0);
        HttpTools.setCookie(response, HttpTools.AE_TOKEN_COOKIE, null, 0);
        HttpTools.setCookie(response, HttpTools.AE_AUTH_MESSAGE_COOKIE, null, 0);
        HttpTools.setCookie(response, HttpTools.AE_AUTH_USERNAME_COOKIE, null, 0);
        logger.debug("Logged out user [{}]", authenticatedUser.getUsername());
        response.sendRedirect(request.getContextPath());

    }
}
