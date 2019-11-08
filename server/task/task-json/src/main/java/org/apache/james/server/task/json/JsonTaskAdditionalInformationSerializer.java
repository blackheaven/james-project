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
package org.apache.james.server.task.json;

import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.james.json.JsonGenericSerializer;
import org.apache.james.server.task.json.dto.AdditionalInformationDTO;
import org.apache.james.server.task.json.dto.AdditionalInformationDTOModule;
import org.apache.james.task.TaskExecutionDetails;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableSet;

public class JsonTaskAdditionalInformationSerializer {
    @FunctionalInterface
    public interface RequestRootModules {
        RequestNestedModules root(AdditionalInformationDTOModule<?, ?>... modules);
    }

    @FunctionalInterface
    public interface RequestNestedModules {
        JsonTaskAdditionalInformationSerializer nested(AdditionalInformationDTOModule<?, ?>... modules);
    }

    public static RequestRootModules builder() {
        return root -> nested -> new JsonTaskAdditionalInformationSerializer(ImmutableSet.copyOf(root), ImmutableSet.copyOf(nested));
    }

    public static class InvalidAdditionalInformationException extends RuntimeException {
        public InvalidAdditionalInformationException(JsonGenericSerializer.InvalidTypeException original) {
            super(original);
        }
    }

    public static class UnknownAdditionalInformationException extends RuntimeException {
        public UnknownAdditionalInformationException(JsonGenericSerializer.UnknownTypeException original) {
            super(original);
        }
    }

    public static JsonTaskAdditionalInformationSerializer of(AdditionalInformationDTOModule<?, ?>... modules) {
        return new JsonTaskAdditionalInformationSerializer(ImmutableSet.copyOf(modules), ImmutableSet.of());
    }

    private JsonGenericSerializer<TaskExecutionDetails.AdditionalInformation, AdditionalInformationDTO> jsonGenericSerializer;

    @Inject
    private JsonTaskAdditionalInformationSerializer(Set<AdditionalInformationDTOModule<?, ?>> root, @Named("MigrationTransitionAdditionalInformationModules") Set<AdditionalInformationDTOModule<?, ?>> nested) {
        jsonGenericSerializer = JsonGenericSerializer.forModules(root).withNestedTypeModules(nested);
    }

    public String serialize(TaskExecutionDetails.AdditionalInformation additionalInformation) throws JsonProcessingException {
        try {
            return jsonGenericSerializer.serialize(additionalInformation);
        } catch (JsonGenericSerializer.UnknownTypeException e) {
            throw new UnknownAdditionalInformationException(e);
        }
    }

    public TaskExecutionDetails.AdditionalInformation deserialize(String value) throws IOException {
        try {
            return jsonGenericSerializer.deserialize(value);
        } catch (JsonGenericSerializer.UnknownTypeException e) {
            throw new UnknownAdditionalInformationException(e);
        } catch (JsonGenericSerializer.InvalidTypeException e) {
            throw new InvalidAdditionalInformationException(e);
        }
    }

}
