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
import uk.ac.ebi.biostudies.components.Files;
import uk.ac.ebi.biostudies.utils.download.IDownloadFile;
import uk.ac.ebi.biostudies.utils.download.RegularDownloadFile;
import uk.ac.ebi.microarray.biostudies.shared.auth.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;

public class FileDownloadServlet extends BaseDownloadServlet {
    private static final long serialVersionUID = 292987974909737571L;

    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    protected IDownloadFile getDownloadFileFromRequest(
            HttpServletRequest request
            , HttpServletResponse response
            , String relativePath
            , User authenticatedUser
    ) throws DownloadServletException {
        String accession = "";
        String name = "";
        IDownloadFile file = null;

        try {
            String[] requestArgs = request.getPathInfo().replaceFirst("^/", "").split("/");
            if (requestArgs.length == 1) { // name only passed
                name = requestArgs[0];
            } else if (requestArgs.length == 2) { // accession/name passed
                accession = requestArgs[0];
                name = requestArgs[1];
            }

            logger.info("Requested download of [" + name + "], path [" + relativePath + "]");
            Files files = getComponent(Files.class);
            File downloadFile = new File(files.getRootFolder(), relativePath+ "/Files/" +name);

            if (downloadFile.exists()) {
                if (downloadFile.isDirectory()) {
                    String forwardedParams = String.format("?url=%s",
                            URLEncoder.encode(files.getFtpURL() + relativePath+"/"+name, "UTF-8"));
                    request.getRequestDispatcher("/servlets/view/download/directory/html"+forwardedParams ).forward(request, response);
                    return null;
                }
                file = new RegularDownloadFile(downloadFile);
            } else if (name.equalsIgnoreCase(accession+".json") || name.equalsIgnoreCase(accession+".xml") || name.equalsIgnoreCase(accession+".pagetab.tsv") ) {
                file = new RegularDownloadFile(new File(files.getRootFolder(), relativePath+ "/" +name));
            } else {
                throw new DownloadServletException("Could not open "+ downloadFile.getAbsolutePath() );
            }

            // Check if trying to download a src file
            if (file==null) {
                if (name.equalsIgnoreCase(accession+".json")) {
                    file = new RegularDownloadFile(new File(files.getRootFolder(), relativePath+ "/" +name));
                }
            }
        } catch (Exception x) {
            throw new DownloadServletException(x);
        }
        return file;
    }

}
