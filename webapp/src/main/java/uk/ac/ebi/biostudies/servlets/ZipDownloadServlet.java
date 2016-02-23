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
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipDownloadServlet extends BaseDownloadServlet {
    private static final long serialVersionUID = 292987974909737571L;
    private static final int KB = 1024;
    private static final int MB = KB * KB;
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
            for (String filename : filenames) {
                String fqName = files.getRootFolder() + "/" + relativePath + "/Files/" + filename;
                fileSizeSum += new File(fqName).length();
            }
            boolean isLargeFile = fileSizeSum > 200* MB;  // Threshold for large files which will be available for 24 hours
            request.setAttribute("isLargeFile", true);
            String uuid = UUID.randomUUID().toString();
            try {
                if (isLargeFile) {
                    Thread thread = new ZipperThread(filenames, relativePath, uuid,null);
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
                    Thread thread = new ZipperThread(filenames, relativePath, uuid,response.getOutputStream());
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

    private class ZipperThread extends Thread {
        private String[] files;
        private String relativePath;
        private String uuid;
        private OutputStream out;

        public ZipperThread(String[] files, String relativePath, String uuid, OutputStream out) {
            this.files = files;
            this.relativePath = relativePath;
            this.uuid = uuid;
            this.out = out;
        }

        public void run() {
            Files filesComponent = getComponent(Files.class);

            String zipFileName = filesComponent.getTempZipFolder() + "/" + uuid + ".zip";
            byte[] buffer = new byte[4 * KB];
            OutputStream outputStream = null;
            try {
                outputStream= out!=null ? out : new FileOutputStream(zipFileName);
                try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
                    for (String filename : files) {
                        ZipEntry entry = new ZipEntry(filename);
                        zos.putNextEntry(entry);
                        File file = new File(filesComponent.getRootFolder() + "/" + relativePath + "/Files/" + filename);
                        FileInputStream fin = new FileInputStream(file);
                        int length;
                        while ((length = fin.read(buffer)) > 0) {
                            zos.write(buffer, 0, length);
                        }
                        fin.close();
                        zos.closeEntry();
                    }
                }
            } catch (Exception e) {
                new File(zipFileName).delete();
                e.printStackTrace();
            } finally {
                if(outputStream!=null)
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
            ZipStatusServlet.removeFile(uuid);
        }
    }
}
