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

import org.apache.commons.io.IOUtils;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostudies.app.ApplicationServlet;
import uk.ac.ebi.biostudies.components.JobsController;
import uk.ac.ebi.biostudies.components.Studies;
import uk.ac.ebi.biostudies.components.Thumbnails;
import uk.ac.ebi.biostudies.utils.HttpTools;
import uk.ac.ebi.biostudies.utils.RegexHelper;
import uk.ac.ebi.biostudies.utils.StringTools;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ControlServlet extends ApplicationServlet {
    private static final long serialVersionUID = -4509580274404536983L;

    private static final String REFERER_HEADER = "Referer";

    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    protected boolean canAcceptRequest(HttpServletRequest request, RequestType requestType) {
        if (request.getPathInfo().contains("assignment")) {
            return true;
        }
        // Allow only ebi hosts and 127.0.0.1 to call the admin functions
        // Doing it here because RemoteHostFilter doesn't seem to be working properly
        // and we don't have access to the Apache server
        try {
            String ip = request.getHeader("X-Cluster-Client-IP");
            if (ip == null || ip.equalsIgnoreCase("")) {
                logger.warn("Header X-Cluster-Client-IP not found");
                ip = request.getHeader("X-Forwarded-For");
            }
            if (ip == null || ip.equalsIgnoreCase("")) {
                logger.warn("Header X-Forwarded-For not found");
                ip = request.getRemoteAddr();
            }

            String hn = InetAddress.getByName(ip).getCanonicalHostName();
            String patternString = getPreferences().getString("app.admin.allow-list");
            Pattern allow = Pattern.compile(patternString);
            Matcher matcher = allow.matcher(hn);
            if (!matcher.matches()) {
                logger.warn("Rejecting admin URL request from {} {}", ip, hn);
                return false;
            }
            logger.warn("Accepting admin URL request from {} {}", ip, hn);
        } catch (Exception ex) {
            return false;
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
            if (
                    "reload-atlas-info".equals(command)
                            || "reload-efo".equals(command)
                            || "update-efo".equals(command)
                            || "check-files".equals(command)
                            || "rescan-files".equals(command)
                            || "check-experiments".equals(command)
                            || "reload-atlas-info".equals(command)
                            || "delete-temp-zip-files".equals(command)
                    ) {
                getComponent(JobsController.class).executeJob(command);
            } else if ("reload-xml".equals(command)) {
                getComponent(Studies.class).updateFromXMLFile(request.getParameter("xmlFilePath"), Boolean.parseBoolean(request.getParameter("delete")));
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
            } else if ("assignment".equals(command)) { //TODO: refactor this (and remove hardcoded check in canAcceptRequest)
                sendAssignment(request, response, params);
            } else if ("upload-and-index".equals(command)) {
                File uploadedFile = uploadFile(request, response);
                if (uploadedFile != null) {
                    getComponent(Studies.class).updateFromXMLFile(uploadedFile, true, false);
                    HttpTools.displayMessage(request,response,"Success!", uploadedFile.getName()+" successfully loaded.");
                }
            }

        } catch (SchedulerException x) {
            logger.error("Jobs controller threw an exception", x);
            HttpTools.displayMessage(request,response,"Error!", x.getMessage());
        } catch (Exception e) {
            logger.error("Controller threw an exception", e);
            HttpTools.displayMessage(request,response,"Error!", e.getMessage());
        }
    }

    private void sendAssignment(HttpServletRequest request, HttpServletResponse response, String params) throws IOException {
        String code = request.getParameter("code");
        getApplication().sendEmail(
                null
                , null
                , "Assignment requested"
                , "An assignment has just been downloaded, the code was [" + code + "]"
                        + StringTools.EOL
        );
        if (!params.isEmpty() && ("ahigw".equals(code) || "naolp".equals(code) || "bkeje".equals(code) || "awais".equals(code))) {
            response.setContentType("application/pdf");
            response.addHeader("Content-Disposition", "attachment; filename=EBI_00651_assignment.pdf");
            InputStream fin = getServletContext().getResourceAsStream("/WEB-INF/server-assets/jobs/EBI_00651_assignment.pdf");
            OutputStream out = response.getOutputStream();
            IOUtils.copy(fin, out);
            out.flush();
            out.close();
            fin.close();
        }
    }

    private File uploadFile(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String sourceLocation = System.getProperty("java.io.tmpdir");
        Part filePart = request.getPart("file");
        if (filePart == null) {
            HttpTools.displayMessage(request,response,"Error!", "Could not upload file.");
            return null;
        }
        String fileName = HttpTools.getFileNameFromPart(filePart);
        if ("studies.xml".equalsIgnoreCase(fileName)) {
            HttpTools.displayMessage(request,response,"Error!", fileName+" can't be overwritten.");
            return null;
        }
        File uploadedFile = new File(sourceLocation, fileName);
        try (FileOutputStream out = new FileOutputStream(uploadedFile);
             InputStream fileContent = filePart.getInputStream();
        ) {
            logger.debug("File {} will be uploaded to {}", fileName, uploadedFile.getAbsolutePath());
            int read;
            final byte[] bytes = new byte[1024];
            while ((read = fileContent.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
        }
        return uploadedFile;
    }

}
