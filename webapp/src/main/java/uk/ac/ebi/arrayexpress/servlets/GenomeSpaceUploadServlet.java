package uk.ac.ebi.arrayexpress.servlets;

/*
 * Copyright 2009-2012 European Molecular Biology Laboratory
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

import org.apache.commons.lang.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress.app.ApplicationServlet;
import uk.ac.ebi.arrayexpress.components.Files;
import uk.ac.ebi.arrayexpress.utils.StringTools;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenomeSpaceUploadServlet extends ApplicationServlet
{
    private static final long serialVersionUID = 4447436099042802322L;

    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected boolean canAcceptRequest( HttpServletRequest request, RequestType requestType )
    {
        return (requestType == RequestType.GET || requestType == RequestType.POST);
    }

    @Override
    protected void doRequest( HttpServletRequest request, HttpServletResponse response, RequestType requestType )
            throws ServletException, IOException
    {
        // implements two commands:
        //
        //  info: get file info (JSON format) for given accession and filename
        //  upload: upload a file (using PUT method) to a given URL, accession and filename

        String action = request.getParameter("action");
        if ("info".equals(action)) {
            doInfoAction(request, response);
        } else if ("upload".equals(action)) {
            doUploadAction(request, response);
        } else {
             response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action [" + (null != action ? action : "null") + "]");
        }
    }

    private void doInfoAction( HttpServletRequest request, HttpServletResponse response )
            throws ServletException, IOException
    {
        Files files = (Files) getComponent("Files");
        String fileName = request.getParameter("file");
        String accession = request.getParameter("accession");

        try {
            String fileLocation = files.getLocation(accession, fileName);
            File file = null != fileLocation ? new File(files.getRootFolder(), fileLocation) : null;

            if (null == file || !file.exists()) {
                logger.error("Requested file information of [{}] which is not found", fileLocation);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                StringBuilder jsonOutput = new StringBuilder();
                jsonOutput
                        .append("{")
                        .append("\"content_size\":\"").append(file.length()).append("\",")
                        .append("\"content_md5\":\"").append(getFileMD5(fileLocation)).append("\",")
                        .append("\"content_type\":\"").append(getServletContext().getMimeType(fileName)).append("\"")
                        .append("}");
                response.setContentType("application/json; charset=US-ASCII");
                try (PrintWriter out = response.getWriter()) {
                    out.print(jsonOutput.toString());
                }
            }
        } catch (InterruptedException x) {
            throw new ServletException(x);
        }
    }

    private void doUploadAction( HttpServletRequest request, HttpServletResponse response )
            throws ServletException, IOException
    {

    }

    private String getFileMD5( String fileLocation ) throws IOException, InterruptedException
    {
        String md5 = null;
        if (null != fileLocation) {
            String md5Command = getPreferences().getString("ae.files.get-md5-base64-encoded-command");

            // put fileLocation in place of ${file} in command line
            Map<String, String> params = new HashMap<>();
            params.put("arg.file", fileLocation);
            StrSubstitutor sub = new StrSubstitutor(params);
            md5Command = sub.replace(md5Command);

            // execute command
            List<String> commandParams = new ArrayList<>();
            commandParams.add("/bin/sh");
            commandParams.add("-c");
            commandParams.add(md5Command);
            this.logger.debug("Executing [{}]", md5Command);
            ProcessBuilder pb = new ProcessBuilder(commandParams);
            Process process = pb.start();

            try (InputStream stdOut = process.getInputStream()) {

                md5 = StringTools.streamToString(stdOut, "US-ASCII").replaceAll("\\s", "");
                if (0 != process.waitFor()) {
                    md5 = null;
                }
            }
        }
        return md5;
    }
}