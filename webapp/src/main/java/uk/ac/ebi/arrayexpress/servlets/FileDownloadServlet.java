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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress.components.Files;
import uk.ac.ebi.microarray.arrayexpress.shared.auth.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

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
            if (1 == requestArgs.length) { // name only passed
                name = requestArgs[0];
            } else if (2 == requestArgs.length) { // accession/name passed
                accession = requestArgs[0];
                name = requestArgs[1];
            }

            file = getSingleFile(request, response, relativePath, name);
        } catch (DownloadServletException x) {
            throw x;
        } catch (Exception x) {
            throw new DownloadServletException(x);
        }
        return file;
    }

    private IDownloadFile getSingleFile(HttpServletRequest request, HttpServletResponse response, String relativePath, String name) throws IOException, DownloadServletException {
        IDownloadFile file;
        logger.info("Requested download of [" + name + "], path [" + relativePath + "]");
        Files files = getComponent(Files.class);
        file = new RegularDownloadFile(new File(files.getRootFolder(), relativePath+"/Files/"+name));
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
