/*
 * Copyright 2009-2015 European Molecular Biology Laboratory
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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress.app.ApplicationServlet;
import uk.ac.ebi.arrayexpress.components.Users;
import uk.ac.ebi.arrayexpress.utils.CookieMap;
import uk.ac.ebi.arrayexpress.utils.HttpTools;
import uk.ac.ebi.microarray.arrayexpress.shared.auth.User;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/*
 *  This servlet supports openId authentication to GenomeSpace
 *  and experiment upload functionality
 */
public class AuthServlet extends ApplicationServlet {
    private static final long serialVersionUID = -4788567497622259711L;

    private final transient Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected boolean canAcceptRequest(HttpServletRequest request, RequestType requestType) {
        return requestType == RequestType.GET || requestType == RequestType.POST;
    }

    @Override
    protected void doRequest(HttpServletRequest request, HttpServletResponse response, RequestType requestType)
            throws ServletException, IOException {
        logRequest(logger, request, requestType);

        String returnURL = request.getHeader(HttpTools.REFERER_HEADER);

        String username = request.getParameter("u");
        String password = request.getParameter("p");
        String remember = request.getParameter("r");
        String email = request.getParameter("e");
        String accession = request.getParameter("a");
        String userAgent = request.getHeader("User-Agent");

        Users users = getComponent(Users.class);
        boolean isLoginSuccessful = false;
        if (null != email) {
            String message = users.remindPassword(StringUtils.trimToEmpty(email), StringUtils.trimToEmpty(accession));
            if (null != message) {
                HttpTools.setCookie(response, HttpTools.AE_AUTH_MESSAGE_COOKIE, message, null);
            }
        } else {
            User authenticatedUser = users.login(username, password);
            isLoginSuccessful = authenticatedUser!=null;
            // 31,557,600 is a standard year in seconds
            Integer maxAge = "on".equals(remember) ? 31557600 : null;

            if (isLoginSuccessful) {
                logger.debug("Successfully authenticated user [{}]", username);
                HttpTools.setCookie(response, HttpTools.AE_USERNAME_COOKIE, username, maxAge);
                HttpTools.setCookie(response, HttpTools.AE_TOKEN_COOKIE, authenticatedUser.getHashedPassword(), maxAge);
            } else {
                HttpTools.setCookie(response, HttpTools.AE_AUTH_USERNAME_COOKIE, username, null);
                HttpTools.setCookie(response, HttpTools.AE_AUTH_MESSAGE_COOKIE, "Incorrect user name or password", null);
            }
        }

        if (null != returnURL) {
            if (isLoginSuccessful && returnURL.matches("^http[:]//www(dev)?[.]ebi[.]ac[.]uk/.+")) {
                returnURL = returnURL.replaceFirst("^http[:]//", "https://");
            }
            logger.debug("Will redirect to [{}]", returnURL);
            response.sendRedirect(returnURL);
        } else {
            response.setContentType("text/plain; charset=UTF-8");
            // Disable cache no matter what (or we're fucked on IE side)
            response.addHeader("Pragma", "no-cache");
            response.addHeader("Cache-Control", "no-cache");
            response.addHeader("Cache-Control", "must-revalidate");
            response.addHeader("Expires", "Fri, 16 May 2008 10:00:00 GMT"); // some date in the past

            PrintWriter out = response.getWriter();
            try {
                // TODO: check what this does
                // out.print(token);
            } catch (Exception x) {
                throw new RuntimeException(x);
            }
            out.close();
        }

    }


}
