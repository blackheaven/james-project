/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.modules.objectstorage;

import java.net.URI;

import org.apache.commons.configuration.Configuration;

import com.google.common.base.Strings;
import com.google.common.base.Preconditions;

public class AwsS3ConfigurationReader {

    static final String OBJECTSTORAGE_ACCESKEYID =
            "objectstorage.s3.accessKeyId";
    static final String OBJECTSTORAGE_SECRETKEY =
            "objectstorage.s3.secretKey";
    static final String OBJECTSTORAGE_ENDPOINT =
            "objectstorage.s3.endPoint";

    public static AwsS3AuthObjectStorage.Configuration readAwsS3Configuration(Configuration configuration) {
        String accessKeyIdStr = configuration.getString(OBJECTSTORAGE_ACCESKEYID, null);
        String secretKeyStr = configuration.getString(OBJECTSTORAGE_SECRETKEY, null);
        String endPointStr = configuration.getString(OBJECTSTORAGE_ENDPOINT, null);

        Preconditions.checkArgument(Strings.isNullOrEmpty(accessKeyIdStr),
                OBJECTSTORAGE_ACCESKEYID + " is a mandatory configuration value");
        Preconditions.checkArgument(Strings.isNullOrEmpty(secretKeyStr),
                OBJECTSTORAGE_SECRETKEY + " is a mandatory configuration value");
        Preconditions.checkArgument(Strings.isNullOrEmpty(endPointStr),
                OBJECTSTORAGE_ENDPOINT + " is a mandatory configuration value");

        URI endpoint = URI.create(endPointStr);

        // TODO
        return AwsS3AuthObjectStorage.configBuilder()
                .endpoint(endpoint)
                .accessKeyId(accessKeyIdStr)
                .secretKey(secretKeyStr)
                .build();
    }
}

