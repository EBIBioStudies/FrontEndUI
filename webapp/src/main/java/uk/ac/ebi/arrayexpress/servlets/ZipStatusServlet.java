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

import com.google.inject.Singleton;
import uk.ac.ebi.arrayexpress.app.ApplicationServlet;
import uk.ac.ebi.arrayexpress.components.Files;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Singleton
public class ZipStatusServlet extends ApplicationServlet {
    private static final long serialVersionUID = 7126224359824782268L;
    private static Set<String> filesBeingProcessed = new HashSet<>();
    Files files = getComponent(Files.class);

    public static void addFile(String file) {
        filesBeingProcessed.add(file);
    }

    public static void removeFile(String file) {
        filesBeingProcessed.remove(file);
    }

    @Override
    protected boolean canAcceptRequest(HttpServletRequest request, RequestType requestType) {
        return requestType == RequestType.GET;
    }

    @Override
    protected void doRequest(HttpServletRequest request, HttpServletResponse response, RequestType requestType) throws ServletException, IOException {
        response.setContentType("application/json; charset=UTF-8");
        response.addHeader("Pragma", "no-cache");
        response.addHeader("Cache-Control", "no-cache");
        response.addHeader("Cache-Control", "must-revalidate");
        response.addHeader("Expires", "Fri, 16 May 2008 10:00:00 GMT"); // some date in the past

        try (PrintWriter out = response.getWriter()) {
            String status = "invalid";
            Files filesComponent = getComponent(Files.class);
            try {
                String uuid = UUID.fromString(request.getParameter("filename")).toString();

                boolean fileExists = new File(filesComponent.getTempZipFolder() + "/" + uuid + ".zip").exists();

                if (fileExists) {
                    if (filesBeingProcessed.contains(uuid)) {
                        status = "processing";
                    } else {
                        status = "done";
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            out.println("{ \"status\": \"" + status + "\"}");
            out.flush();
        }

    }
}
