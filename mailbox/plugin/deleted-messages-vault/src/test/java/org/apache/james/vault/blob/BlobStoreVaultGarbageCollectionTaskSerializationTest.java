/**
 * *************************************************************
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
 ***************************************************************/

package org.apache.james.vault.blob;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.ZonedDateTime;

import org.apache.james.blob.api.BucketName;
import org.apache.james.server.task.json.JsonTaskAdditionalInformationsSerializer;
import org.apache.james.server.task.json.JsonTaskSerializer;
import org.apache.james.task.Task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

class BlobStoreVaultGarbageCollectionTaskSerializationTest {

    private static final JsonTaskSerializer TASK_SERIALIZER = new JsonTaskSerializer(BlobStoreVaultGarbageCollectionTaskDTO.MODULE);
    private static final ZonedDateTime BEGINNING_OF_RETENTION_PERIOD = ZonedDateTime.parse("2019-09-03T15:26:13.356+02:00[Europe/Paris]");
    private static final ImmutableList<BucketName> BUCKET_IDS = ImmutableList.of(BucketName.of("1"), BucketName.of("2"), BucketName.of("3"));
    private static final Flux<BucketName> RETENTION_OPERATION = Flux.fromIterable(BUCKET_IDS);
    private static final BlobStoreVaultGarbageCollectionTask.AdditionalInformation DETAILS = new BlobStoreVaultGarbageCollectionTask.AdditionalInformation(BEGINNING_OF_RETENTION_PERIOD, BUCKET_IDS);
    private static final BlobStoreVaultGarbageCollectionTask TASK = new BlobStoreVaultGarbageCollectionTask(BEGINNING_OF_RETENTION_PERIOD, RETENTION_OPERATION);

    private static final String SERIALIZED_TASK = "{\"beginningOfRetentionPeriod\":\"2019-09-03T15:26:13.356+02:00[Europe/Paris]\",\"retentionOperation\":[\"1\", \"2\", \"3\"],\"type\":\"deletedMessages/blobStoreBasedGarbageCollection\"}";
    private static final String SERIALIZED_ADDITIONAL_INFORMATION_TASK = "{\"beginningOfRetentionPeriod\":\"2019-09-03T15:26:13.356+02:00[Europe/Paris]\",\"deletedBuckets\":[\"1\", \"2\", \"3\"]}";

    private static final JsonTaskAdditionalInformationsSerializer JSON_TASK_ADDITIONAL_INFORMATIONS_SERIALIZER = new JsonTaskAdditionalInformationsSerializer(BlobStoreVaultGarbageCollectionTaskAdditionalInformationDTO.MODULE);

    @Test
    void taskShouldBeSerializable() throws JsonProcessingException {
        assertThatJson(TASK_SERIALIZER.serialize(TASK))
            .isEqualTo(SERIALIZED_TASK);
    }

    @Test
    void taskShouldBeDeserializable() throws IOException {
        Task deserialized = TASK_SERIALIZER.deserialize(SERIALIZED_TASK);

        assertThat(deserialized).isInstanceOf(BlobStoreVaultGarbageCollectionTask.class);
        BlobStoreVaultGarbageCollectionTask blobStoreVaultGarbageCollectionTask = (BlobStoreVaultGarbageCollectionTask) deserialized;
        assertThat(blobStoreVaultGarbageCollectionTask.getBeginningOfRetentionPeriod())
            .isEqualTo(TASK.getBeginningOfRetentionPeriod());
        assertThat(blobStoreVaultGarbageCollectionTask
            .getRetentionOperation()
            .collectList()
            .block())
            .isEqualTo(TASK
                .getRetentionOperation()
                .collectList()
                .block());
    }

    @Test
    void additionalInformationShouldBeSerializable() throws JsonProcessingException {
        assertThatJson(JSON_TASK_ADDITIONAL_INFORMATIONS_SERIALIZER.serialize(DETAILS)).isEqualTo(SERIALIZED_ADDITIONAL_INFORMATION_TASK);
    }

    @Test
    void additonalInformationShouldBeDeserializable() throws IOException {
        assertThat(JSON_TASK_ADDITIONAL_INFORMATIONS_SERIALIZER.deserialize("deletedMessages/blobStoreBasedGarbageCollection", SERIALIZED_ADDITIONAL_INFORMATION_TASK))
            .isEqualToComparingFieldByField(DETAILS);
    }
}