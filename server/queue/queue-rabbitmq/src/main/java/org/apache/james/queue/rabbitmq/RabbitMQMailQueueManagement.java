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

package org.apache.james.queue.rabbitmq;

import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.james.backend.rabbitmq.RabbitMQConfiguration;
import org.apache.james.backend.rabbitmq.RabbitMQManagementAPI;
import org.apache.james.util.OptionalUtils;

import com.google.common.annotations.VisibleForTesting;

public class RabbitMQMailQueueManagement {

    private final RabbitMQManagementAPI api;

    @Inject
    RabbitMQMailQueueManagement(RabbitMQConfiguration configuration) {
        api = RabbitMQManagementAPI.from(configuration);
    }

    @VisibleForTesting
    RabbitMQMailQueueManagement(RabbitMQManagementAPI api) {
        this.api = api;
    }

    Stream<MailQueueName> listCreatedMailQueueNames() {
        return api.listQueues()
            .stream()
            .map(RabbitMQManagementAPI.MessageQueue::getName)
            .map(MailQueueName::fromRabbitWorkQueueName)
            .flatMap(OptionalUtils::toStream)
            .distinct();
    }

    public void deleteAllQueues() {
        api.listQueues()
            .forEach(queue -> api.deleteQueue("/", queue.getName()));
    }

    public Long getQueueSize(MailQueueName name) {
        return api.listQueues()
            .stream()
            .filter(queue -> queue.getName().equals(name.asString()))
            .map(RabbitMQManagementAPI.MessageQueue::getMessages)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Unable to get mail queue size of " + name));
    }
}