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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostudies.components.Files;
import uk.ac.ebi.biostudies.utils.download.IDownloadFile;
import uk.ac.ebi.biostudies.utils.download.RegularDownloadFile;
import uk.ac.ebi.biostudies.utils.download.ZipperThread;
import uk.ac.ebi.microarray.biostudies.shared.auth.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.UUID;

public class ZipDownloadServlet extends BaseDownloadServlet {
    private static final long serialVersionUID = 292987974909737571L;
    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected void doBeforeDownloadFileFromRequest(HttpServletRequest request, HttpServletResponse response, String relativePath) throws DownloadServletException {

        Files files = getComponent(Files.class);

        // set filename and accession
        String[] requestArgs = request.getPathInfo().replaceFirst("^/", "").split("/");
        String accession = requestArgs[0];
        String[] filenames = request.getParameterMap().get("files");

        if (filenames != null) { // first hit: We have to zip the files
            long fileSizeSum = 0;
            boolean isDirectory =false ;
            for (String filename : filenames) {
                String fqName = files.getRootFolder() + "/" + relativePath + "/Files/" + StringUtils.replace(filename,"..","");
                File thisFile = new File(fqName);
                fileSizeSum += thisFile.length();
                if (thisFile.isDirectory()) {
                    isDirectory = true;
                    break;
                }
            }
            // Threshold for large files which will be available for 24 hours.
            // Since we don't know the size of a directory, just treat is as a large file.
            boolean isLargeFile = isDirectory || fileSizeSum > 200* Files.MB;
            String uuid = UUID.randomUUID().toString();
            try {
                if (isLargeFile) {
                    Thread thread = new ZipperThread(this, filenames, relativePath, uuid,null);
                    thread.start();
                    String datacentre = System.getProperty("datacentre") == null ? "lc" : System.getProperty("datacentre");
                    String forwardedParams = String.format("?uuid=%s&accession=%s&dc=%s",
                            URLEncoder.encode(uuid, "UTF-8"),
                            URLEncoder.encode(accession, "UTF-8"),
                            URLEncoder.encode(datacentre.substring(0, 1), "UTF-8"));
                    ZipStatusServlet.addFile(uuid);
                    request.getRequestDispatcher("/servlets/view/-/zip/html" + forwardedParams)
                            .forward(request, response);
                    return;
                } else {
                    // File is not large. Send over the zipped stream
                    response.setContentType("application/zip");
                    response.addHeader("Content-Disposition", "attachment; filename="+ accession+".zip");
                    Thread thread = new ZipperThread(this, filenames, relativePath, uuid,response.getOutputStream());
                    thread.start();
                    thread.join();
                }
            } catch (Exception e) {
                throw new DownloadServletException(e);
            }

        }

    }

    protected IDownloadFile getDownloadFileFromRequest(
            HttpServletRequest request
            , HttpServletResponse response
            , String relativePath, User authenticatedUser
    ) throws DownloadServletException {

        String path=null;
        String[] requestArgs = request.getPathInfo().replaceFirst("^/", "").split("/");
        String accession = requestArgs[0];

        if (request.getParameter("file")!=null) { // async request
            Files filesComponent = getComponent(Files.class);
            String uuid = UUID.fromString(request.getParameter("file")).toString();
            path = filesComponent.getTempZipFolder()+"/"+uuid + ".zip";
        } else if (request.getAttribute("zipFile")!=null) {
            path = (String) request.getAttribute("zipFile");
        }
        if (path==null) return null;
        return (path!=null) ? new RegularDownloadFile(new File(path), accession+".zip") : null;
    }

    @Override
    protected void doAfterDownloadFileFromRequest(HttpServletRequest request, HttpServletResponse response) throws DownloadServletException {
        if (request.getAttribute("zipFile") != null) {
            File zipFile = new File( (String)request.getAttribute("zipFile"));
            zipFile.delete();
            logger.info("Zip file {} deleted", zipFile.getName());
        }
    }

}
