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

import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress.app.ApplicationServlet;
import uk.ac.ebi.arrayexpress.components.JobsController;
import uk.ac.ebi.arrayexpress.components.Studies;
import uk.ac.ebi.arrayexpress.components.Thumbnails;
import uk.ac.ebi.arrayexpress.utils.RegexHelper;
import uk.ac.ebi.arrayexpress.utils.StringTools;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ControlServlet extends ApplicationServlet {
    private static final long serialVersionUID = -4509580274404536983L;

    private static final String REFERER_HEADER = "Referer";

    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    protected boolean canAcceptRequest(HttpServletRequest request, RequestType requestType) {
        // Allow only ebi hosts and 127.0.0.1 to call the admin functions
        // Doing it here because RemoteHostFilter doesn't seem to be working properly
        // and we don't have access to the Apache server
        try {
            String ip = request.getRemoteHost();
            String hn = InetAddress.getByName(ip).getCanonicalHostName();
            String patternString = getPreferences().getString("app.admin.allow-list");
            Pattern allow = Pattern.compile(patternString);
            Matcher matcher = allow.matcher(hn);
            if(!matcher.matches()) {
                logger.warn("Rejecting admin URL request from {} {}",ip,hn);
                return false;
            }
            logger.warn("Accepting admin URL request from {} {}",ip,hn);
        } catch (Exception ex) {
            return  false;
        }

        return (requestType == RequestType.GET || requestType == RequestType.POST);
    }

    // Respond to HTTP requests from browsers.
    protected void doRequest(HttpServletRequest request, HttpServletResponse response, RequestType requestType) throws ServletException, IOException {
        logRequest(logger, request, requestType);

        String command = "";
        String params = "";

        String[] requestArgs = new RegexHelper("/([^/]+)/?(.*)", "i")
                .match(request.getPathInfo());
        if (null != requestArgs) {
            command = requestArgs[0];
            params = requestArgs[1];
        }
        try {
            //TODO: place these behind an authentication flow
            if (
                    "reload-atlas-info".equals(command)
                            || "reload-efo".equals(command)
                            || "update-efo".equals(command)
                            || "check-files".equals(command)
                            || "rescan-files".equals(command)
                            || "check-experiments".equals(command)
                            || "reload-atlas-info".equals(command)
                    ) {
                getComponent(JobsController.class).executeJob(command);
            } else if ("reload-xml".equals(command)) {
                getComponent(Studies.class).updateFromXMLFile(request.getParameter("xmlFilePath"),Boolean.parseBoolean(request.getParameter("delete")));
            } else if ("clear-index".equals(command)) {
                getComponent(Studies.class).clearIndex();
            } else if ("delete".equals(command)) {
                getComponent(Studies.class).delete(request.getParameter("accession"));
            } else if ("clear-thumbnails".equals(command)) {
                getComponent(Thumbnails.class).clearThumbnails();
            } else if ("test-email".equals(command)) {
                getApplication().sendEmail(
                        null
                        , null
                        , "Test message"
                        , "This test message was sent from [${variable.appname}] running on [${variable.hostname}], please ignore."
                                + StringTools.EOL
                );
            } else if ("restart".equals(command)) {
                getApplication().requestRestart();
            } else if ("assignment".equals(command)) {
                getApplication().sendEmail(
                        null
                        , null
                        , "Assignment requested"
                        , "An assignment has just been downloaded, the code was [" + request.getParameter("code") + "]"
                                + StringTools.EOL
                );
                if (!params.isEmpty()) {
                    logger.debug("Will redirect to [{}]", params);
                    response.sendRedirect("/" + params);
                }
            }
//            } else if ("reload-ae1-xml".equals(command)) {
//                ((JobsController) getComponent("JobsController")).executeJobWithParam(command, "connections", params);
//            } else if ("rescan-files".equals(command)) {
//                if (!params.isEmpty()) {
//                    ((Files) getComponent("Files")).setRootFolder(params);
//                }
//                ((JobsController) getComponent("JobsController")).executeJob(command);

        } catch (SchedulerException x) {
            logger.error("Jobs controller threw an exception", x);
        } catch (Exception e) {
            logger.error("Controller threw an exception", e);
        }
    }
}
