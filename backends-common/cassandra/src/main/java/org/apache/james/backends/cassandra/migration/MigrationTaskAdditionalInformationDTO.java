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

import java.time.Instant;
import java.util.List;

import org.apache.james.backends.cassandra.versions.SchemaVersion;
import org.apache.james.json.DTOConverter;
import org.apache.james.json.DTOModule;
import org.apache.james.server.task.json.dto.AdditionalInformationDTO;
import org.apache.james.server.task.json.dto.AdditionalInformationDTOModule;
import org.apache.james.task.TaskExecutionDetails;
import org.apache.james.util.OptionalUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.steveash.guavate.Guavate;

public class MigrationTaskAdditionalInformationDTO implements AdditionalInformationDTO {

    public static final AdditionalInformationDTOModule<MigrationTask.AdditionalInformation, MigrationTaskAdditionalInformationDTO> serializationModule(DTOConverter<TaskExecutionDetails.AdditionalInformation, AdditionalInformationDTO> nestedDtoConverter) {
        return DTOModule.forDomainObject(MigrationTask.AdditionalInformation.class)
            .convertToDTO(MigrationTaskAdditionalInformationDTO.class)
            .toDomainObjectConverter(dto ->
                new MigrationTask.AdditionalInformation(
                    new SchemaVersion(dto.getTargetVersion()),
                    dto.timestamp,
                    dto.migrationsDetails.stream().map(nestedDtoConverter::toDomainObject).flatMap(OptionalUtils::toStream).collect(Guavate.toImmutableList())))
            .toDTOConverter((details, type) ->
                new MigrationTaskAdditionalInformationDTO(
                    type,
                    details.getToVersion(),
                    details.timestamp(),
                    details.getMigrationsDetails().stream().map(nestedDtoConverter::toDTO).flatMap(OptionalUtils::toStream).collect(Guavate.toImmutableList())))
            .typeName(MigrationTask.CASSANDRA_MIGRATION.asString())
            .withFactory(AdditionalInformationDTOModule::new);
    }

    private final String type;
    private final int targetVersion;
    private final Instant timestamp;
    private final List<AdditionalInformationDTO> migrationsDetails;

    public MigrationTaskAdditionalInformationDTO(@JsonProperty("type") String type,
                                                 @JsonProperty("targetVersion") int targetVersion,
                                                 @JsonProperty("timestamp") Instant timestamp,
                                                 @JsonProperty("migrationsDetails") List<AdditionalInformationDTO> migrationsDetails) {
        this.type = type;
        this.targetVersion = targetVersion;
        this.timestamp = timestamp;
        this.migrationsDetails = migrationsDetails;
    }

    @Override
    public String getType() {
        return type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getTargetVersion() {
        return targetVersion;
    }

    public List<AdditionalInformationDTO> getMigrationsDetails() {
        return migrationsDetails;
    }
}
