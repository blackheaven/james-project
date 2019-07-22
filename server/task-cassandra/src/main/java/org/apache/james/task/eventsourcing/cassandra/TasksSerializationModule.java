/** **************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 * http://www.apache.org/licenses/LICENSE-2.0                   *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 * ***************************************************************/

package org.apache.james.task.eventsourcing.cassandra;

import java.util.function.Function;

import org.apache.james.eventsourcing.eventstore.cassandra.dto.EventDTOModule;
import org.apache.james.server.task.json.JsonTaskSerializer;
import org.apache.james.task.eventsourcing.CancelRequested;
import org.apache.james.task.eventsourcing.Cancelled;
import org.apache.james.task.eventsourcing.Completed;
import org.apache.james.task.eventsourcing.Created;
import org.apache.james.task.eventsourcing.Failed;
import org.apache.james.task.eventsourcing.Started;

public interface TasksSerializationModule {
    Function<JsonTaskSerializer, EventDTOModule<Created, DTO.CreatedDTO>> CREATED = jsonTaskSerializer -> EventDTOModule
        .forEvent(Created.class)
        .convertToDTO(DTO.CreatedDTO.class)
        .toDomainObjectConverter(dto -> dto.toDomainObject(jsonTaskSerializer))
        .toDTOConverter((event, typeName) -> TaskSerializer.fromDomainObject(event, typeName, jsonTaskSerializer))
        .typeName("task-manager-created")
        .withFactory(EventDTOModule::new);

    Function<JsonTaskSerializer, EventDTOModule<Started, DTO.StartedDTO>> STARTED = jsonTaskSerializer -> EventDTOModule
        .forEvent(Started.class)
        .convertToDTO(DTO.StartedDTO.class)
        .toDomainObjectConverter(dto -> dto.toDomainObject(jsonTaskSerializer))
        .toDTOConverter(TaskSerializer::fromDomainObject)
        .typeName("task-manager-started")
        .withFactory(EventDTOModule::new);

    Function<JsonTaskSerializer, EventDTOModule<CancelRequested, DTO.CancelRequestedDTO>> CANCEL_REQUESTED = jsonTaskSerializer -> EventDTOModule
        .forEvent(CancelRequested.class)
        .convertToDTO(DTO.CancelRequestedDTO.class)
        .toDomainObjectConverter(dto -> dto.toDomainObject(jsonTaskSerializer))
        .toDTOConverter(TaskSerializer::fromDomainObject)
        .typeName("task-manager-cancel-requested")
        .withFactory(EventDTOModule::new);

    Function<JsonTaskSerializer, EventDTOModule<Completed, DTO.CompletedDTO>> COMPLETED = jsonTaskSerializer -> EventDTOModule
        .forEvent(Completed.class)
        .convertToDTO(DTO.CompletedDTO.class)
        .toDomainObjectConverter(dto -> dto.toDomainObject(jsonTaskSerializer))
        .toDTOConverter(TaskSerializer::fromDomainObject)
        .typeName("task-manager-completed")
        .withFactory(EventDTOModule::new);

    Function<JsonTaskSerializer, EventDTOModule<Failed, DTO.FailedDTO>> FAILED = jsonTaskSerializer -> EventDTOModule
        .forEvent(Failed.class)
        .convertToDTO(DTO.FailedDTO.class)
        .toDomainObjectConverter(dto -> dto.toDomainObject(jsonTaskSerializer))
        .toDTOConverter(TaskSerializer::fromDomainObject)
        .typeName("task-manager-failed")
        .withFactory(EventDTOModule::new);

    Function<JsonTaskSerializer, EventDTOModule<Cancelled, DTO.CancelledDTO>> CANCELLED = jsonTaskSerializer -> EventDTOModule
        .forEvent(Cancelled.class)
        .convertToDTO(DTO.CancelledDTO.class)
        .toDomainObjectConverter(dto -> dto.toDomainObject(jsonTaskSerializer))
        .toDTOConverter(TaskSerializer::fromDomainObject)
        .typeName("task-manager-cancelled")
        .withFactory(EventDTOModule::new);

    Function<JsonTaskSerializer, TaskEventDTOModule> MODULES = jsonTaskSerializer -> new TaskEventDTOModule(
        CREATED.apply(jsonTaskSerializer),
        STARTED.apply(jsonTaskSerializer),
        CANCEL_REQUESTED.apply(jsonTaskSerializer),
        CANCELLED.apply(jsonTaskSerializer),
        COMPLETED.apply(jsonTaskSerializer),
        FAILED.apply(jsonTaskSerializer));
}
