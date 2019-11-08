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
package org.apache.james.backends.cassandra.migration;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.apache.james.backends.cassandra.versions.CassandraSchemaVersionDAO;
import org.apache.james.backends.cassandra.versions.SchemaVersion;
import org.apache.james.json.DTOConverter;
import org.apache.james.json.DTOModule;
import org.apache.james.server.task.json.JsonTaskAdditionalInformationSerializer;
import org.apache.james.server.task.json.JsonTaskSerializer;
import org.apache.james.server.task.json.dto.AdditionalInformationDTO;
import org.apache.james.server.task.json.dto.AdditionalInformationDTOModule;
import org.apache.james.task.TaskExecutionDetails;
import org.apache.james.task.TaskType;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;

class MigrationTaskSerializationTest {

    private static class StepOneTask {
        private static class StepOneAdditionalInformationDTO implements AdditionalInformationDTO {

            private final String type;
            private final int x;

            public StepOneAdditionalInformationDTO(@JsonProperty("type") String type,
                                                   @JsonProperty("x") int x,
                                                   @JsonProperty("timestamp") String timestamp) {
                this.type = type;
                this.x = x;
            }

            @Override
            public String getType() {
                return type;
            }

            @Override
            public Instant getTimestamp() {
                return Instant.MIN;
            }

            public int getX() {
                return x;
            }
        }

        private static class AdditionalInformation implements TaskExecutionDetails.AdditionalInformation {
            private final int x;

            private AdditionalInformation(int x) {
                this.x = x;
            }

            @Override
            public Instant timestamp() {
                return Instant.MIN;
            }

            public int getX() {
                return x;
            }
        }

        public static final TaskType TYPE = TaskType.of("step-one");

        public static AdditionalInformationDTOModule<AdditionalInformation, StepOneAdditionalInformationDTO> ADDITIONAL_INFORMATION_MODULE = DTOModule
            .forDomainObject(StepOneTask.AdditionalInformation.class)
            .convertToDTO(StepOneAdditionalInformationDTO.class)
            .toDomainObjectConverter(dto -> new StepOneTask.AdditionalInformation(dto.getX()))
            .toDTOConverter((task, typeName) -> new StepOneAdditionalInformationDTO(typeName, task.x, null))
            .typeName(TYPE.asString())
            .withFactory(AdditionalInformationDTOModule::new);
    }

    private static class StepTwoTask {

        private static class AdditionalInformation implements TaskExecutionDetails.AdditionalInformation {
            private final String y;

            private AdditionalInformation(String y) {
                this.y = y;
            }

            @Override
            public Instant timestamp() {
                return Instant.MIN;
            }

            public String getY() {
                return y;
            }
        }

        private static class StepTwoAdditionalInformationDTO implements AdditionalInformationDTO {

            private final String type;
            private final String y;

            public StepTwoAdditionalInformationDTO(@JsonProperty("type") String type,
                                                   @JsonProperty("y") String y,
                                                   @JsonProperty("timestamp") String timestamp) {
                this.type = type;
                this.y = y;
            }

            @Override
            public String getType() {
                return type;
            }

            @Override
            public Instant getTimestamp() {
                return Instant.MIN;
            }

            public String getY() {
                return y;
            }
        }

        public static final TaskType TYPE = TaskType.of("step-two");

        public static AdditionalInformationDTOModule<AdditionalInformation, StepTwoAdditionalInformationDTO> ADDITIONAL_INFORMATION_MODULE = DTOModule
            .forDomainObject(StepTwoTask.AdditionalInformation.class)
            .convertToDTO(StepTwoAdditionalInformationDTO.class)
            .toDomainObjectConverter(dto -> new StepTwoTask.AdditionalInformation(dto.getY()))
            .toDTOConverter((task, typeName) -> new StepTwoAdditionalInformationDTO(typeName, task.y, null))
            .typeName(TYPE.asString())
            .withFactory(AdditionalInformationDTOModule::new);
    }

    private static final int SCHEMA_VERSION = 12;
    private static final String SERIALIZED_TASK = "{\"type\": \"cassandra-migration\", \"targetVersion\": 12}";
    private static final String SERIALIZED_ADDITIONAL_INFORMATION = "{\"type\": \"cassandra-migration\", \"targetVersion\": 12, \"timestamp\": \"2018-11-13T12:00:55Z\",\"migrationsDetails\":[{\"timestamp\":\"-1000000000-01-01T00:00:00Z\",\"type\":\"step-one\",\"x\":42},{\"type\":\"step-two\",\"y\":\"101010\",\"timestamp\":\"-1000000000-01-01T00:00:00Z\"}]}";
    private static final Instant TIMESTAMP = Instant.parse("2018-11-13T12:00:55Z");
    private static final List<TaskExecutionDetails.AdditionalInformation> ACHIEVED_MIGRATIONS = ImmutableList.of(new StepOneTask.AdditionalInformation(42), new StepTwoTask.AdditionalInformation("101010"));

    private final CassandraSchemaVersionDAO cassandraSchemaVersionDAO = mock(CassandraSchemaVersionDAO.class);
    private final CassandraSchemaTransitions transitions = mock(CassandraSchemaTransitions.class);
    private final MigrationTask.Factory factory = target -> new MigrationTask(cassandraSchemaVersionDAO, transitions, target);
    private final JsonTaskSerializer taskSerializer = JsonTaskSerializer.of(MigrationTaskDTO.module(factory));
    private JsonTaskAdditionalInformationSerializer jsonAdditionalInformationSerializer = JsonTaskAdditionalInformationSerializer
        .builder()
        .root(MigrationTaskAdditionalInformationDTO.serializationModule(DTOConverter.of(StepOneTask.ADDITIONAL_INFORMATION_MODULE, StepTwoTask.ADDITIONAL_INFORMATION_MODULE)))
        .nested(StepOneTask.ADDITIONAL_INFORMATION_MODULE, StepTwoTask.ADDITIONAL_INFORMATION_MODULE);

    @Test
    void taskShouldBeSerializable() throws JsonProcessingException {
        MigrationTask task = factory.create(new SchemaVersion(SCHEMA_VERSION));
        assertThatJson(taskSerializer.serialize(task)).isEqualTo(SERIALIZED_TASK);
    }

    @Test
    void taskShouldBeDeserializable() throws IOException {
        MigrationTask task = factory.create(new SchemaVersion(SCHEMA_VERSION));
        assertThat(taskSerializer.deserialize(SERIALIZED_TASK))
            .isEqualToComparingFieldByField(task);
    }

    @Test
    void additionalInformationShouldBeSerializable() throws JsonProcessingException {
        MigrationTask.AdditionalInformation details = new MigrationTask.AdditionalInformation(new SchemaVersion(SCHEMA_VERSION), TIMESTAMP, ACHIEVED_MIGRATIONS);
        assertThatJson(jsonAdditionalInformationSerializer.serialize(details)).isEqualTo(SERIALIZED_ADDITIONAL_INFORMATION);
    }

    @Test
    void additionalInformationShouldBeDeserializable() throws IOException {
        MigrationTask.AdditionalInformation details = new MigrationTask.AdditionalInformation(new SchemaVersion(SCHEMA_VERSION), TIMESTAMP, ACHIEVED_MIGRATIONS);
        assertThat(jsonAdditionalInformationSerializer.deserialize(SERIALIZED_ADDITIONAL_INFORMATION))
            .isEqualToComparingFieldByFieldRecursively(details);
    }
}
