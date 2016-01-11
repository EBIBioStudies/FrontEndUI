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

package uk.ac.ebi.arrayexpress.utils.download;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;

public interface IDownloadFile {
    public String getName();

    public String getPath();

    public long getLength();

    public long getLastModified();

    public boolean canDownload();

    public boolean isRandomAccessSupported();

    public DataInput getRandomAccessFile() throws IOException;

    public InputStream getInputStream() throws IOException;

    public void close() throws IOException;
}
