/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 * http://www.apache.org/licenses/LICENSE-2.0                   *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 * ***************************************************************/

package org.apache.james.webadmin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.apache.james.queue.api.MailQueueFactory;
import org.apache.james.queue.api.ManageableMailQueue;
import org.apache.james.server.task.json.JsonTaskSerializer;

import net.javacrumbs.jsonunit.assertj.JsonAssertions;
import org.junit.jupiter.api.Test;

class ClearMailQueueTaskTest {

    private static final String SERIALIZED = "{\"type\": \"clearMailQueue\", \"queue\": \"anyQueue\"}";

    @Test
    void taskShouldBeSerializable() throws Exception {
        MailQueueFactory<ManageableMailQueue> mailQueueFactory = mock(MailQueueFactory.class);
        ManageableMailQueue mockedQueue = mock(ManageableMailQueue.class);
        String queueName = "anyQueue";
        when(mockedQueue.getName()).thenReturn(queueName);
        when(mailQueueFactory.getQueue(anyString())).thenAnswer(arg -> Optional.of(mockedQueue));
        JsonTaskSerializer testee = new JsonTaskSerializer(ClearMailQueueTask.MODULE.apply(mailQueueFactory));

        ManageableMailQueue queue = mailQueueFactory.getQueue(queueName).get();
        ClearMailQueueTask task = new ClearMailQueueTask(queue);
        JsonAssertions.assertThatJson(testee.serialize(task)).isEqualTo(SERIALIZED);
    }

    @Test
    void taskShouldBeDeserializable() throws Exception {
        MailQueueFactory<ManageableMailQueue> mailQueueFactory = mock(MailQueueFactory.class);
        ManageableMailQueue mockedQueue = mock(ManageableMailQueue.class);
        String queueName = "anyQueue";
        when(mockedQueue.getName()).thenReturn(queueName);
        when(mailQueueFactory.getQueue(anyString())).thenAnswer(arg -> Optional.of(mockedQueue));
        JsonTaskSerializer testee = new JsonTaskSerializer(ClearMailQueueTask.MODULE.apply(mailQueueFactory));

        ManageableMailQueue queue = mailQueueFactory.getQueue(queueName).get();
        ClearMailQueueTask task = new ClearMailQueueTask(queue);
        assertThat(testee.deserialize(SERIALIZED)).isEqualToIgnoringGivenFields(task, "additionalInformation");
    }
}
