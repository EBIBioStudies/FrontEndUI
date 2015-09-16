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

package uk.ac.ebi.arrayexpress.components;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress.app.ApplicationComponent;
import uk.ac.ebi.arrayexpress.utils.StringTools;
import uk.ac.ebi.arrayexpress.utils.saxon.*;
import uk.ac.ebi.microarray.arrayexpress.shared.auth.AuthenticationHelper;
import uk.ac.ebi.microarray.arrayexpress.shared.auth.User;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Users extends ApplicationComponent implements XMLDocumentSource {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final static String MAP_USERS_FOR_ACCESSION = "users-for-accession";

    private AuthenticationHelper authHelper;
    private StoredDocument document;
    private MapEngine.JointValueMap userMap;

    private SaxonEngine saxon;
    private SearchEngine search;

    public final String INDEX_ID = "users";

    public enum UserSource {
        AE1, AE2;

        public String getStylesheetName() {
            switch (this) {
                case AE1:
                    return "preprocess-users-ae1-xml.xsl";
                case AE2:
                    return "preprocess-users-ae2-xml.xsl";
            }
            return null;
        }
    }

    public Users() {
    }

    @Override
    public void initialize() throws Exception {
        /*
        this.saxon = getComponent(SaxonEngine.class);
        this.search = getComponent(SearchEngine.class);
        this.document = new StoredDocument(
                new File(getPreferences().getString("bs.users.persistence-location")),
                "users"
        );

        this.userMap = new MapEngine.JointValueMap(MAP_USERS_FOR_ACCESSION);

        MapEngine maps = getComponent(MapEngine.class);
        maps.registerMap(this.userMap);
//        maps.registerMap(new MapEngine.SimpleValueMap(Studies.MAP_EXPERIMENTS_FOR_USER));

        updateIndex();


        this.saxon.registerDocumentSource(this);
        */
        this.authHelper = new AuthenticationHelper();
    }

    @Override
    public void terminate() throws Exception {
    }

    @Override
    public String getURI() {
        return "users.xml";
    }

    @Override
    public synchronized NodeInfo getRootNode() throws IOException {
        return document.getRootNode();
    }

    @Override
    public synchronized void setRootNode(NodeInfo rootNode) throws IOException, SaxonException {
        if (null != rootNode) {
            this.document = new StoredDocument(rootNode,
                    new File(getPreferences().getString("bs.users.persistence-location")));
            updateIndex();
        } else {
            this.logger.error("User information NOT updated, NULL document passed");
        }
    }

    public void registerUserMap(MapEngine.IValueMap map) {
        this.userMap.addMap(map);
    }

    public void clearUserMap(String name) {
        MapEngine.IValueMap map = this.userMap.getMap(name);
        if (null != map) {
            map.clearValues();
        } else {
            logger.error("Map [{}] not found", name);
        }
    }

    public void setUserMapping(String name, String accession, Object ids) {
        MapEngine.IValueMap map = this.userMap.getMap(name);
        if (null != map) {
            map.setValue(accession, ids);
        } else {
            logger.error("Map [{}] not found", name);
        }
    }


    public boolean isAccessible(String accession, List<String> userIds) throws IOException {
        @SuppressWarnings("unchecked")
        Set<String> ids = (Set<String>) this.userMap.getValue(accession);
        for (String userId : userIds) {
            if (isPrivilegedByID(userId) || (null != ids && ids.contains(userId)))
                return true;
        }
        return false;
    }

    public void update(String xmlString, UserSource source) throws IOException, InterruptedException {
        try {
            NodeInfo update = this.saxon.transform(xmlString, source.getStylesheetName(), null);
            if (null != update) {
                new DocumentUpdater(this, update).update();
            }
        } catch (SaxonException x) {
            throw new RuntimeException(x);
        }
    }

    private void updateIndex() throws IOException {
        try {
            this.search.getController().index(INDEX_ID, document);
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    public boolean isPrivilegedByName(String name) throws IOException {
        /*
        name = StringEscapeUtils.escapeXml(name);
        try {
            return ((BooleanValue) saxon.evaluateXPathSingle(
                    getRootNode()
                    , "(/users/user[name = '" + name + "']/is_privileged = true())"
            )).effectiveBooleanValue();
        } catch (XPathException x) {
            throw new RuntimeException(x);
        }*/
        //TODO: check privilege
        return true;
    }

    public boolean isPrivilegedByID(String id) throws IOException {
        id = StringEscapeUtils.escapeXml(id);
        try {
            return ((BooleanValue) saxon.evaluateXPathSingle(
                    getRootNode()
                    , "(/users/user[id = '" + id + "']/is_privileged = true())"
            )).effectiveBooleanValue();
        } catch (XPathException x) {
            throw new RuntimeException(x);
        }
    }

    public User login(String username, String password) throws IOException {
        return checkAccess(username, this.authHelper.generateHash(password));
    }

    public User checkAccess(String username, String passwordHash) throws IOException {
        if (username==null || passwordHash ==null) return null;
        String response = this.authHelper.sendAuthenticationRequest(username
                                    , passwordHash
                                    , getPreferences().getString("bs.users.authentication-url")  );
        String [] lines = response.split("\n");
        if (lines.length<3 || !"Status: OK".equalsIgnoreCase(lines[0])) {
            return null;
        }

        User user = new User();
       user.setUsername(username);
       user.setHashedPassword(passwordHash);
       user.setAllow(StringUtils.split(StringUtils.split(lines[1], ':')[1].trim(), ','));
       user.setDeny(StringUtils.split(StringUtils.split(lines[2], ':')[1].trim(), ','));
        return user;
    }

    @SuppressWarnings("unchecked")
    public String remindPassword(String nameOrEmail, String accession) throws IOException {
        nameOrEmail = StringEscapeUtils.escapeXml(nameOrEmail);
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
        }

    }

}
