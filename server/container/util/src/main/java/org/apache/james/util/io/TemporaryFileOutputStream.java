/**
 * *************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                   *
 * *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ***************************************************************/

package org.apache.james.util.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemporaryFileOutputStream extends FileOutputStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(TemporaryFileOutputStream.class);

    private final File file;

    public TemporaryFileOutputStream(String prefix, String suffix) throws IOException {
        this(File.createTempFile("imap", ".msg"));
    }

    private TemporaryFileOutputStream(File temporaryFile) throws IOException {
        super(temporaryFile);
        this.file = temporaryFile;
    }

    public File getFile() {
        return file;
    }

    @Override
    public void close() throws IOException {
        super.close();

        if (!file.delete()) {
            LOGGER.warn("Unable to delete the temporary file: {}", file);
        }
    }
}
