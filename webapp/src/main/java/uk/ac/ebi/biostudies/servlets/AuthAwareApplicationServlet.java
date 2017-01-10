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
import uk.ac.ebi.biostudies.components.Users;
import uk.ac.ebi.biostudies.utils.CookieMap;
import uk.ac.ebi.microarray.biostudies.shared.auth.User;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;

public abstract class AuthAwareApplicationServlet extends ApplicationServlet {
    private static final long serialVersionUID = -82727624065665432L;

    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    private final static String AE_LOGIN_USER_COOKIE = "AeLoggedUser";
    private final static String AE_LOGIN_TOKEN_COOKIE = "AeLoginToken";

    protected static class AuthApplicationServletException extends ServletException {
        private static final long serialVersionUID = 1030249369830812548L;

        public AuthApplicationServletException(Throwable x) {
            super(x);
        }
    }

    protected abstract void doAuthenticatedRequest(
            HttpServletRequest request
            , HttpServletResponse response
            , RequestType requestType
            , User authUser
    ) throws ServletException, IOException;

    protected void doRequest(HttpServletRequest request, HttpServletResponse response, RequestType requestType)
            throws ServletException, IOException {
        User authenticatedUser = getAuthenticatedUser(request, requestType);
        if (authenticatedUser == null) {
            invalidateAuthCookies(response);
        }
        doAuthenticatedRequest(request, response, requestType, authenticatedUser);
    }

    private void invalidateAuthCookies(HttpServletResponse response) {
        // deleting user cookie
        Cookie userCookie = new Cookie(AE_LOGIN_USER_COOKIE, "");
        userCookie.setPath("/");
        userCookie.setMaxAge(0);

        response.addCookie(userCookie);
    }

    protected User getAuthenticatedUser(HttpServletRequest request, RequestType requestType) throws ServletException {
        try {
            CookieMap cookies = new CookieMap(request.getCookies());
            String userName = cookies.getCookieValue(AE_LOGIN_USER_COOKIE);
//            if (null != userName) { // Commenting this to enable + sign in username
//                userName = URLDecoder.decode(userName, "UTF-8");
//            }
            //TODO: token is hashed password for now. Check if it should be replaced
            String token = cookies.getCookieValue(AE_LOGIN_TOKEN_COOKIE);
            Users users = getComponent(Users.class);
            return users.checkAccess(userName, token);
        } catch (Exception x) {
            getApplication().handleException(
                    "[SEVERE] Runtime error while processing " + requestToString(request, requestType)
                    , x
            );
            return null;
        }
    }

}
