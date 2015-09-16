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
import uk.ac.ebi.microarray.arrayexpress.shared.auth.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

//TODO: decouple from the BaseDownloadServlet to remove vfs dependency
public class ZipDownloadServlet extends BaseDownloadServlet {
    private static final long serialVersionUID = 292987974909737571L;
    private transient final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String ROOTPATH = "ram://virtual";
    private FileSystemManager fsManager;

    @Override
    protected void doBeforeDownloadFileFromRequest(HttpServletRequest request, HttpServletResponse response) throws DownloadServletException {
        // set filename and accession
        String[] requestArgs = request.getPathInfo().replaceFirst("^/", "").split("/");
        String accession = requestArgs[0];

        String[] filenames = request.getParameterMap().get("files");
        Files files = getComponent(Files.class);
        byte[] buffer = new byte[1024];
        try {
            fsManager = VFS.getManager();
            FileObject zipFile = fsManager.resolveFile(ROOTPATH + "/" + accession + "." + UUID.randomUUID() + ".zip");
            logger.info("Creating zip file {} for accession [{}] files: {}", zipFile.getName(), accession, filenames);
            zipFile.createFile();
            try (ZipOutputStream zos = new ZipOutputStream(zipFile.getContent().getOutputStream())) {
                for (String filename : filenames) {
                    if (files.doesExist(accession, filename)) {
                        ZipEntry entry = new ZipEntry(filename);
                        zos.putNextEntry(entry);
                        FileInputStream fin = new FileInputStream(files.getRootFolder() + files.getLocation(accession, filename));
                        int length;
                        while ((length = fin.read(buffer)) > 0) {
                            zos.write(buffer, 0, length);
                        }
                        fin.close();
                        zos.closeEntry();
                    }
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
            if (request.getAttribute("zipFile")!=null) {
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
            , User authenticatedUser
    ) throws DownloadServletException {
        // set filename and accession
        String[] requestArgs = request.getPathInfo().replaceFirst("^/", "").split("/");
        String accession = requestArgs[0];

        String[] filenames = request.getParameterMap().get("files");
        logger.info("Requested download of accession [{}] files: {}", accession, filenames);
        IDownloadFile zipfile = new RAMZipFile((FileObject) request.getAttribute("zipFile"),accession);
        return zipfile;
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
            return null;
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
}
