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

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.util.RandomAccessMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress.components.Files;
import uk.ac.ebi.arrayexpress.utils.download.IDownloadFile;
import uk.ac.ebi.arrayexpress.utils.download.RegularDownloadFile;
import uk.ac.ebi.microarray.arrayexpress.shared.auth.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

//TODO: decouple from the BaseDownloadServlet to remove vfs dependency
public class ZipDownloadServlet extends BaseDownloadServlet {
    private static final long serialVersionUID = 292987974909737571L;
    private transient final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String VIRTUAL_ROOT = "ram://virtual";
    private FileSystemManager fsManager;
    private static final int MB = 1024*1024;

    @Override
    protected void doBeforeDownloadFileFromRequest(HttpServletRequest request, HttpServletResponse response, String relativePath) throws DownloadServletException {

        Files files = getComponent(Files.class);

        // set filename and accession
        String[] requestArgs = request.getPathInfo().replaceFirst("^/", "").split("/");
        String accession = requestArgs[0];
        String[] filenames = request.getParameterMap().get("files");

        if (filenames!=null) { // first hit: We have to zip the files
            long fileSizeSum = 0;
            for (String filename : filenames) {
                String fqName = files.getRootFolder() + "/" + relativePath+"/Files/"+filename;
               fileSizeSum += new File(fqName).length();
            }
            boolean isLargeFile = fileSizeSum > 50*MB;  // Threshold for large files which will be available only on ftp
            if (!isLargeFile) {
                // stream smaller files from memory
                createZipArchive(request, relativePath, accession, filenames, files);
            } else {
                // create larger files asynchronously
                request.setAttribute("isLargeFile",true);
                String uuid = UUID.randomUUID().toString();
                ZipStatusServlet.addFile(uuid);
                new ZipperThread(filenames, relativePath, uuid).start();

                try {
                    String datacenter = System.getProperty("datacentre")==null ? "lc" : System.getProperty("datacentre");
                    String forwardedParams = String.format("?uuid=%s&accession=%s&dc=%s",
                            URLEncoder.encode(uuid,"UTF-8"),
                            URLEncoder.encode(accession,"UTF-8"),
                            URLEncoder.encode(datacenter.substring(0,1),"UTF-8"));
                    request.getRequestDispatcher("/servlets/query/-/zip/html"+forwardedParams)
                                .forward(request, response);
                } catch (Exception e) {
                    throw new DownloadServletException(e);
                }
            }
        }

    }

    private void createZipArchive(HttpServletRequest request, String relativePath, String accession, String[] filenames, Files files) throws DownloadServletException {
        byte[] buffer = new byte[10*MB];
        try {
            fsManager = VFS.getManager();
            FileObject zipFile = fsManager.resolveFile( VIRTUAL_ROOT + "/" + accession + "." + UUID.randomUUID() + ".zip");
            logger.info("Creating zip file {} for accession [{}] files: {}", zipFile.getName(), accession, filenames);
            zipFile.createFile();
            try (ZipOutputStream zos = new ZipOutputStream(zipFile.getContent().getOutputStream())) {
                for (String filename : filenames) {
                    ZipEntry entry = new ZipEntry(filename);
                    zos.putNextEntry(entry);
                    FileInputStream fin = new FileInputStream(files.getRootFolder() + "/" + relativePath+"/Files/"+filename );
                    int length;
                    while ((length = fin.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    fin.close();
                    zos.closeEntry();
                }
            }
            zipFile.close();
            request.setAttribute("zipFile", zipFile);
        } catch (IOException e) {
            throw  new DownloadServletException(e);
        }
    }

    @Override
    protected void doAfterDownloadFileFromRequest(HttpServletRequest request, HttpServletResponse response) throws DownloadServletException {
        try {
            if (request.getAttribute("zipFile")!=null && request.getAttribute("isLargeFile")==null) {
                FileObject zipFile = (FileObject) request.getAttribute("zipFile");
                zipFile.delete();
                logger.info("Zip file {} deleted", zipFile.getName());
            }
        } catch (FileSystemException e) {
            throw  new DownloadServletException(e);
        }
    }

    protected IDownloadFile getDownloadFileFromRequest(
            HttpServletRequest request
            , HttpServletResponse response
            , String relativePath, User authenticatedUser
    ) throws DownloadServletException {
        // set filename and accession
        String[] requestArgs = request.getPathInfo().replaceFirst("^/", "").split("/");
        String accession = requestArgs[0];

        String[] filenames = request.getParameterMap().get("files");
        if (filenames!=null) { // First hit: zip them files!
            logger.info("Requested download of accession [{}] files: {}", accession, filenames);
            if (request.getAttribute("zipFile") != null) {
                IDownloadFile zipfile = new RAMZipFile((FileObject) request.getAttribute("zipFile"), accession);
                return zipfile;
            }
        } else if (request.getParameter("file")!=null) { //second hit: File has already been created. Stream it
            Files filesComponent = getComponent(Files.class);
            String uuid = UUID.fromString(request.getParameter("file")).toString();
            return new RegularDownloadFile(new File(filesComponent.getTempZipFolder(), uuid+".zip"));
        }
        return null;
    }

    protected final class RAMZipFile implements IDownloadFile {
        private final FileObject fileObject;
        private String accession;

        public RAMZipFile(FileObject fileObject, String accession) {
            if (null == fileObject) {
                throw new IllegalArgumentException("FileObject cannot be null");
            }
            this.fileObject = fileObject;
            this.accession = accession;
        }

        public String getName() {
            return accession + ".zip";
        }

        public String getPath() {
            return fileObject.getName().getBaseName();
        }

        public long getLength() {
            try {
                return fileObject.getContent().getSize();
            } catch (FileSystemException e) {
                e.printStackTrace();
            }
            return 0;
        }

        public long getLastModified() {
            // last modified makes part of the etag so return length instead in order to support resumes
            return getLength();
        }

        public boolean canDownload() {
            return true;
        }

        public boolean isRandomAccessSupported() {
            return true;
        }

        public DataInput getRandomAccessFile() throws IOException {
            return fileObject.getContent().getRandomAccessContent(RandomAccessMode.READ);
        }

        public InputStream getInputStream() throws IOException {
            return fileObject.getContent().getInputStream();
        }

        public void close() throws IOException {
        }
    }

    private class ZipperThread extends Thread {
        private String[] files;
        private String relativePath;
        private String uuid;

        public ZipperThread(String[] files, String relativePath, String uuid) {
            this.files = files;
            this.relativePath = relativePath;
            this.uuid = uuid;
        }

        public void run() {
            Files filesComponent = getComponent(Files.class);

            String zipFileName = filesComponent.getTempZipFolder()+ "/" + uuid + ".zip";
            byte[] buffer = new byte[10*MB];
            try (FileOutputStream zipFile = new FileOutputStream(zipFileName)) {
                try (ZipOutputStream zos = new ZipOutputStream(zipFile)) {
                    for (String filename : files) {
                        ZipEntry entry = new ZipEntry(filename);
                        zos.putNextEntry(entry);
                        File file = new File (filesComponent.getRootFolder()+"/"+relativePath+"/Files/"+filename);
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
            }
            ZipStatusServlet.removeFile(uuid);
        }
    }
}
