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

package uk.ac.ebi.biostudies.components;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostudies.app.ApplicationComponent;
import uk.ac.ebi.microarray.biostudies.shared.auth.AuthenticationHelper;
import uk.ac.ebi.microarray.biostudies.shared.auth.User;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Users extends ApplicationComponent {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Cache<Object, Object> passwordCache;

    private AuthenticationHelper authHelper;
    private SearchEngine search;

    public Users() {
        // TODO: move password cache timeout to config file
        passwordCache = CacheBuilder.newBuilder()
                .expireAfterAccess(60, TimeUnit.MINUTES)
                .build();
    }

    @Override
    public void initialize() throws Exception {
        this.search = getComponent(SearchEngine.class);
        this.authHelper = new AuthenticationHelper();
    }

    @Override
    public void terminate() throws Exception {
    }

    public User login(String username, String password) throws IOException {
        return checkAccess(username, this.authHelper.generateHash(password));
    }

    public void logout(String username) {
        passwordCache.invalidate(username);
    }

    public User checkAccess(String username, String passwordHash) throws IOException {
        if (username==null || passwordHash ==null) return null;

        // if password is found in the cache, use it
        User user =  (User) passwordCache.getIfPresent(username);
        if (user!=null && username.equalsIgnoreCase(user.getUsername()) && passwordHash.equalsIgnoreCase(user.getHashedPassword())) {
            logger.debug("Found authentication for user [{}] in local cache",username);
            return user;
        }

        // send request to backend for authentication
        logger.debug("Trying to authenticate user [{}] remotely",username);
        String response = this.authHelper.sendAuthenticationRequest(username
                                    , passwordHash
                                    , getPreferences().getString("bs.users.authentication-url")  );
        String [] lines = response.split("\n");
        if (lines.length<3 || !"Status: OK".equalsIgnoreCase(lines[0])) {
            return null;
        }

        user = new User();
        user.setUsername(username);
        user.setHashedPassword(passwordHash);
        Set<String> allowSet = new HashSet<>(Arrays.asList(StringUtils.split(StringUtils.split(lines[1], ':')[1].trim().replaceAll("~", ""), ';')));
        Set<String> denySet = new HashSet<>(Arrays.asList(StringUtils.split(StringUtils.split(lines[2], ':')[1].trim().replaceAll("~", ""), ';')));
        allowSet.removeAll(denySet);
        user.setAllow(allowSet.toArray(new String[allowSet.size()]));
        user.setDeny(denySet.toArray(new String[denySet.size()]));
        passwordCache.put(username, user);
        return user;
    }

    //TODO: handle password flow
    public String remindPassword(String nameOrEmail, String accession) throws IOException {
       /* nameOrEmail = StringEscapeUtils.escapeXml(nameOrEmail);
        accession = null != accession ? accession.toUpperCase() : "";

        try {
            List users = null;

            Object userIds = this.userMap.getValue(accession);
            if (userIds instanceof Set) {
                Set<String> uids = (Set<String>) (userIds);
                String ids = StringTools.arrayToString(uids.toArray(new String[uids.size()]), ",");

                users = this.saxon.evaluateXPath(
                        getRootNode()
                        , "/users/user[(name|email = '" + nameOrEmail + "') and id = (" + ids + ")]"
                );
            }

            String reportMessage;
            String result = "Unable to find matching account information, please contact us for assistance.";
            if (null != users && users.size() > 0) {
                if (1 == users.size()) {
                    String username = this.saxon.evaluateXPathSingle((NodeInfo) users.get(0), "string(name)").getStringValue();
                    String email = this.saxon.evaluateXPathSingle((NodeInfo) users.get(0), "string(email)").getStringValue();
                    String password = this.saxon.evaluateXPathSingle((NodeInfo) users.get(0), "string(password)").getStringValue();

                    getApplication().sendEmail(
                            getPreferences().getString("bs.password-remind.originator")
                            , new String[]{email}
                            , getPreferences().getString("bs.password-remind.subject")
                            , "Dear " + username + "," + StringTools.EOL
                                    + StringTools.EOL
                                    + "Your ArrayExpress account information is:" + StringTools.EOL
                                    + StringTools.EOL
                                    + "    User name: " + username + StringTools.EOL
                                    + "    Password: " + password + StringTools.EOL
                                    + StringTools.EOL
                                    + "Regards," + StringTools.EOL
                                    + "ArrayExpress." + StringTools.EOL
                                    + StringTools.EOL
                    );


                    reportMessage = "Sent account information to the user [" + username + "], email [" + email + "], accession [" + accession + "]";
                    result = "Account information sent, please check your email";
                } else {
                    // multiple results, report this to administrators
                    reportMessage = "Request failed: found multiple users for name/email [" + nameOrEmail + "] accessing [" + accession + "].";
                }
            } else {
                // no results, report this to administrators
                reportMessage = "Request failed: found no users for name/email [" + nameOrEmail + "] accessing [" + accession + "].";
            }

            getApplication().sendEmail(
                    getPreferences().getString("bs.password-remind.originator")
                    , getPreferences().getStringArray("bs.password-remind.recipients")
                    , "ArrayExpress account information request"
                    , reportMessage + StringTools.EOL
                            + StringTools.EOL
                            + "Sent by [${variable.appname}] running on [${variable.hostname}]" + StringTools.EOL
                            + StringTools.EOL
            );
            return result;

        } catch (XPathException x) {
            throw new RuntimeException(x);
        }*/
        return null;
    }

}
