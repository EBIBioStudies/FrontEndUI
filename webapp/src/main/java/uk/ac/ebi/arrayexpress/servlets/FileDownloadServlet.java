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
import uk.ac.ebi.arrayexpress.components.Studies;
import uk.ac.ebi.arrayexpress.components.Users;
import uk.ac.ebi.arrayexpress.utils.saxon.search.Querier;
import uk.ac.ebi.microarray.arrayexpress.shared.auth.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileDownloadServlet extends BaseDownloadServlet {
    private static final long serialVersionUID = 292987974909737571L;

    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    protected IDownloadFile getDownloadFileFromRequest(
            HttpServletRequest request
            , HttpServletResponse response
            , User authenticatedUser
    ) throws DownloadServletException {
        String accession = "";
        String name = "";
        IDownloadFile file = null;

        try {
            String[] requestArgs = request.getPathInfo().replaceFirst("^/", "").split("/");
            if (1 == requestArgs.length) { // name only passed
                name = requestArgs[0];
            } else if (2 == requestArgs.length) { // accession/name passed
                accession = requestArgs[0];
                name = requestArgs[1];
            }

            file = getSingleFile(request, response, accession, name);
        } catch (DownloadServletException x) {
            throw x;
        } catch (Exception x) {
            throw new DownloadServletException(x);
        }
        return file;
    }

    private IDownloadFile getSingleFile(HttpServletRequest request, HttpServletResponse response, String accession, String name) throws IOException, DownloadServletException {
        IDownloadFile file;
        logger.info("Requested download of [" + name + "], accession [" + accession + "]");
        Files files = getComponent(Files.class);
        if (!files.doesExist(accession, name)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            throw new DownloadServletException(
                    "File [" + name + "], accession [" + accession + "] is not in files.xml");
        } else {
            String location = files.getLocation(accession, name);
            // finally if there is no accession or location determined at the stage - panic
            if ("".equals(location) || "".equals(accession)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                throw new DownloadServletException(
                        "Either accession ["
                                + String.valueOf(accession)
                                + "] or location ["
                                + String.valueOf(location)
                                + "] were not determined");
            }


            logger.debug("Will be serving file [{}]", location);
            file = new RegularDownloadFile(new File(files.getRootFolder(), location));
        }
        return file;
    }

    protected final class RegularDownloadFile implements IDownloadFile {
        private final File file;

        public RegularDownloadFile(File file) {
            if (null == file) {
                throw new IllegalArgumentException("File cannot be null");
            }
            this.file = file;
        }

        private File getFile() {
            return this.file;
        }

        public String getName() {
            return getFile().getName();
        }

        public String getPath() {
            return getFile().getPath();
        }

        public long getLength() {
            return getFile().length();
        }

        public long getLastModified() {
            return getFile().lastModified();
        }

        public boolean canDownload() {
            return getFile().exists() && getFile().isFile() && getFile().canRead();
        }

        public boolean isRandomAccessSupported() {
            return true;
        }

        public DataInput getRandomAccessFile() throws IOException {
            return new RandomAccessFile(getFile(), "r");
        }

        public InputStream getInputStream() throws IOException {
            return new FileInputStream(getFile());
        }

        public void close() throws IOException {
        }
    }

}
