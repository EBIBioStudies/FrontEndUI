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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostudies.app.ApplicationServlet;
import uk.ac.ebi.biostudies.components.SaxonEngine;
import uk.ac.ebi.biostudies.utils.HttpServletRequestParameterMap;
import uk.ac.ebi.biostudies.utils.StringTools;
import uk.ac.ebi.biostudies.utils.saxon.SaxonException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AssignmentServlet extends ApplicationServlet {


    private static final long serialVersionUID = -8989741313068467584L;
    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected boolean canAcceptRequest(HttpServletRequest request, RequestType requestType) {
        return (requestType == RequestType.GET || requestType == RequestType.POST);
    }

    @Override
    protected void doRequest(HttpServletRequest request, HttpServletResponse response, RequestType requestType) throws ServletException, IOException {

        // tell client to not cache the page unless we want to
        if (!"true".equalsIgnoreCase(request.getParameter("cache"))) {
            response.addHeader("Pragma", "no-cache");
            response.addHeader("Cache-Control", "must-revalidate");
            response.addHeader("Cache-Control", "no-cache");
            response.addHeader("Expires", "Fri, 16 May 2000 10:00:00 GMT"); // some date in the past
        }

        // Read the mapping file
        HashMap<String, AssignmentMetaData> assignmentUserMap = readAssignmentToUserMapFile();
        HttpServletRequestParameterMap params = new HttpServletRequestParameterMap(request);
        String assignmentId = params.getString("assignmentId");
        String code = params.getString("code");

        //invalid job
        if (!assignmentUserMap.containsKey(assignmentId)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        AssignmentMetaData assignment = assignmentUserMap.get(assignmentId);
        params.put("description", assignment.getDescription());

        // invalid code
        if (!assignment.getAllowList().contains(code)) {
            params.remove("code");
            renderPage(response, params);
            return;
        }

        // send file if conditions are accepted
        if (params.containsKey("start")) {
            sendAssignment(code,assignmentId, assignment.getEmailTo(), response);
            return;
        }

        //shouldn't reach here but just in case there is an error
        renderPage(response, params);
        return;
    }

    private void renderPage(HttpServletResponse response, HttpServletRequestParameterMap params) {
        // Output goes to the response PrintWriter.
        try (PrintWriter out = response.getWriter()) {
            String stylesheetName = "assignment-html.xsl";
            try {
                SaxonEngine saxonEngine = getComponent(SaxonEngine.class);

                if (!saxonEngine.transform(saxonEngine.buildDocument("<assignment/>"), stylesheetName, params, new StreamResult(out))) {
                    throw new Exception("Transformation returned an error");
                }
            } catch (SaxonException x) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    private HashMap<String, AssignmentMetaData> readAssignmentToUserMapFile() throws ServletException {
        HashMap<String, AssignmentMetaData> map = new HashMap<>();
        JSONParser parser = new JSONParser();
        try (FileReader reader = new FileReader(new File(System.getProperty("java.io.tmpdir"), "jobs/assignments.json"))) {
            JSONArray assignments = (JSONArray) parser.parse(reader);
            for (int i = 0; i < assignments.size(); i++) {
                JSONObject assignment = (JSONObject) assignments.get(i);
                String id = (String) assignment.get("id");
                JSONArray allow = (JSONArray) assignment.get("allow");
                List<String> allowList = new ArrayList<>();
                for (int j = 0; j < allow.size(); j++) {
                    allowList.add((String) allow.get(j));
                }
                String emailTo = (String) assignment.get("emailTo");
                AssignmentMetaData amd = new AssignmentMetaData(
                        id,
                        allowList,
                        (String) assignment.get("description"),
                        emailTo
                );
                map.put(id, amd);
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
        return map;
    }

    private void sendAssignment(String code, String assignmentId, String emailTo, HttpServletResponse response) throws IOException {
        logger.debug("Sending "+ assignmentId + " to " + code );
        getApplication().sendEmail(
                null
                , new String [] {emailTo}
                , "Assignment requested"
                , "An assignment for "+assignmentId+" has just been downloaded, the code was [" + code + "]"
                        + StringTools.EOL
        );

        response.setContentType("application/pdf");
        response.addHeader("Content-Disposition", "attachment; "+assignmentId+".pdf");

        FileInputStream fin =  new FileInputStream(new File(System.getProperty("java.io.tmpdir"), "jobs/"+assignmentId+".pdf"));
        OutputStream out = response.getOutputStream();
        IOUtils.copy(fin, out);
        out.flush();
        out.close();
        fin.close();

    }

    private class AssignmentMetaData {
        private String id;
        private List<String> allowList;
        private String description;
        private String emailTo;

        public AssignmentMetaData(String id, List<String> allowList, String description, String emailTo) {
            this.id = id;
            this.allowList = allowList;
            this.description = description;
            this.emailTo = emailTo;
        }

        public String getId() {
            return id;
        }

        public List<String> getAllowList() {
            return allowList;
        }

        public String getDescription() {
            return description;
        }

        public String getEmailTo() {
            return emailTo;
        }
    }
}