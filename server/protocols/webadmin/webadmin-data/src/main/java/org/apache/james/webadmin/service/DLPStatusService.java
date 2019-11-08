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

package org.apache.james.webadmin.service;

import javax.inject.Inject;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.james.modules.server.CamelMailetContainerModule;

public class DLPStatusService {

    private final CamelMailetContainerModule.MailetConfigurationProvider mailetConfigurationProvider;

    @Inject
    public DLPStatusService(CamelMailetContainerModule.MailetConfigurationProvider mailetConfigurationProvider) {
        this.mailetConfigurationProvider = mailetConfigurationProvider;
    }

    public boolean isActive() {
        try {
            HierarchicalConfiguration<ImmutableNode> mailets = mailetConfigurationProvider.forName("processors");
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("TODO");
    }
}
