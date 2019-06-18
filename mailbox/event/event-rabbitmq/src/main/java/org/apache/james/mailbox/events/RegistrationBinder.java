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

package org.apache.james.mailbox.events;

import static org.apache.james.mailbox.events.RabbitMQEventBus.MAILBOX_EVENT_EXCHANGE_NAME;

import javax.inject.Provider;

import reactor.core.publisher.Mono;
import reactor.rabbitmq.BindingSpecification;
import reactor.rabbitmq.ResourceManagementOptions;
import reactor.rabbitmq.Sender;

class RegistrationBinder {
    private final Sender sender;
    private final RegistrationQueueName registrationQueue;
    private final Provider<ResourceManagementOptions> resourceManagement;

    RegistrationBinder(Sender sender, RegistrationQueueName registrationQueue, Provider<ResourceManagementOptions> resourceManagement) {
        this.sender = sender;
        this.registrationQueue = registrationQueue;
        this.resourceManagement = resourceManagement;
    }

    Mono<Void> bind(RegistrationKey key) {
        return sender.bind(bindingSpecification(key), resourceManagement.get())
            .then();
    }

    Mono<Void> unbind(RegistrationKey key) {
        return sender.unbind(bindingSpecification(key), resourceManagement.get())
            .then();
    }

    private BindingSpecification bindingSpecification(RegistrationKey key) {
        RoutingKeyConverter.RoutingKey routingKey = RoutingKeyConverter.RoutingKey.of(key);
        return BindingSpecification.binding()
            .exchange(MAILBOX_EVENT_EXCHANGE_NAME)
            .queue(registrationQueue.asString())
            .routingKey(routingKey.asString());
    }
}