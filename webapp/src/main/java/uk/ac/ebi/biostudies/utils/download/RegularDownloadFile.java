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

package uk.ac.ebi.biostudies.utils.download;

import java.io.*;

public final class RegularDownloadFile implements IDownloadFile {
    private final File file;
    private String downloadName;

    public RegularDownloadFile(File file) {
        if (null == file) {
            throw new IllegalArgumentException("File cannot be null");
        }
        this.file = file;
    }

    public RegularDownloadFile(File file, String downloadName) {
        if (null == file) {
            throw new IllegalArgumentException("File cannot be null");
        }
        this.file = file;
        this.downloadName = downloadName;
    }
    private File getFile() {
        return this.file;
    }

    public String getName() {
        return downloadName!=null ? downloadName : getFile().getName();
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

    public boolean isDirectory() {
        return getFile().exists() && getFile().isDirectory() && getFile().canRead();
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
