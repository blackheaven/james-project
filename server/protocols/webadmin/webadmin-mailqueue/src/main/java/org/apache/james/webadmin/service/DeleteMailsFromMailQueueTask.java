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

package org.apache.james.webadmin.service;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.james.core.MailAddress;
import org.apache.james.json.DTOModule;
import org.apache.james.queue.api.MailQueue;
import org.apache.james.queue.api.MailQueueFactory;
import org.apache.james.queue.api.ManageableMailQueue;
import org.apache.james.server.task.json.dto.TaskDTO;
import org.apache.james.server.task.json.dto.TaskDTOModule;
import org.apache.james.task.Task;
import org.apache.james.task.TaskExecutionDetails;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.fge.lambdas.Throwing;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Booleans;

public class DeleteMailsFromMailQueueTask implements Task {

    public static class AdditionalInformation implements TaskExecutionDetails.AdditionalInformation {
        private final String mailQueueName;
        private final Supplier<Long> countSupplier;
        private final long initialCount;

        private final Optional<String> sender;
        private final Optional<String> name;
        private final Optional<String> recipient;

        public AdditionalInformation(String mailQueueName, Supplier<Long> countSupplier,
                                     Optional<MailAddress> maybeSender, Optional<String> maybeName,
                                     Optional<MailAddress> maybeRecipient) {
            this.mailQueueName = mailQueueName;
            this.initialCount = countSupplier.get();
            this.countSupplier = countSupplier;

            sender = maybeSender.map(MailAddress::asString);
            name = maybeName;
            recipient = maybeRecipient.map(MailAddress::asString);
        }

        public String getMailQueueName() {
            return mailQueueName;
        }

        public long getRemainingCount() {
            return countSupplier.get();
        }

        public long getInitialCount() {
            return initialCount;
        }

        public Optional<String> getSender() {
            return sender;
        }

        public Optional<String> getName() {
            return name;
        }

        public Optional<String> getRecipient() {
            return recipient;
        }
    }

    private static class DeleteMailsFromFailQueueTaskDTO implements TaskDTO {

        private final String type;
        private final String queue;
        private final String sender;
        private final String name;
        private final String recipient;

        public DeleteMailsFromFailQueueTaskDTO(@JsonProperty("type") String type,
                                               @JsonProperty("queue") String queue,
                                               @JsonProperty("sender") String sender,
                                               @JsonProperty("name") String name,
                                               @JsonProperty("recipient") String recipient) {
            this.type = type;
            this.queue = queue;
            this.sender = sender;
            this.name = name;
            this.recipient = recipient;
        }

        public static DeleteMailsFromFailQueueTaskDTO toDTO(DeleteMailsFromMailQueueTask domainObject, String typeName) {
            return new DeleteMailsFromFailQueueTaskDTO(
                typeName,
                domainObject.queue.getName(),
                domainObject.maybeSender.map(MailAddress::asString).orElse(null),
                domainObject.maybeName.orElse(null),
                domainObject.maybeRecipient.map(MailAddress::asString).orElse(null)
            );
        }

        public DeleteMailsFromMailQueueTask fromDTO(MailQueueFactory<ManageableMailQueue> mailQueueFactory) {
            return new DeleteMailsFromMailQueueTask(
                    mailQueueFactory.getQueue(queue).orElseThrow(() -> new RuntimeException("Unable to retrieve '" + queue + "' queue")),
                    Optional.ofNullable(sender).map(Throwing.<String, MailAddress>function(MailAddress::new).sneakyThrow()),
                    Optional.ofNullable(name),
                    Optional.ofNullable(recipient).map(Throwing.<String, MailAddress>function(MailAddress::new).sneakyThrow())
            );
        }

        @Override
        public String getType() {
            return type;
        }

        public String getQueue() {
            return queue;
        }

        public String getSender() {
            return sender;
        }

        public String getName() {
            return name;
        }

        public String getRecipient() {
            return recipient;
        }
    }

    public static final String TYPE = "deleteMailsFromMailQueue";
    public static final Function<MailQueueFactory<ManageableMailQueue>, TaskDTOModule> MODULE = (mailQueueFactory) ->
        DTOModule
            .forDomainObject(DeleteMailsFromMailQueueTask.class)
            .convertToDTO(DeleteMailsFromFailQueueTaskDTO.class)
            .toDomainObjectConverter(dto -> dto.fromDTO(mailQueueFactory))
            .toDTOConverter(DeleteMailsFromFailQueueTaskDTO::toDTO)
            .typeName(TYPE)
            .withFactory(TaskDTOModule::new);

    private final ManageableMailQueue queue;
    private final Optional<MailAddress> maybeSender;
    private final Optional<String> maybeName;
    private final Optional<MailAddress> maybeRecipient;
    private final AdditionalInformation additionalInformation;

    public DeleteMailsFromMailQueueTask(ManageableMailQueue queue, Optional<MailAddress> maybeSender,
                                        Optional<String> maybeName, Optional<MailAddress> maybeRecipient) {
        Preconditions.checkArgument(
                Booleans.countTrue(maybeSender.isPresent(), maybeName.isPresent(), maybeRecipient.isPresent()) == 1,
                "You should provide one and only one of the query parameters 'sender', 'name' or 'recipient'.");

        this.queue = queue;
        this.maybeSender = maybeSender;
        this.maybeName = maybeName;
        this.maybeRecipient = maybeRecipient;

        additionalInformation = new AdditionalInformation(queue.getName(), this::getRemainingSize, maybeSender,
                maybeName, maybeRecipient);
    }

    @Override
    public Result run() {
        maybeSender.ifPresent(Throwing.consumer(
                (MailAddress sender) -> queue.remove(ManageableMailQueue.Type.Sender, sender.asString())));
        maybeName.ifPresent(Throwing.consumer(
                (String name) -> queue.remove(ManageableMailQueue.Type.Name, name)));
        maybeRecipient.ifPresent(Throwing.consumer(
                (MailAddress recipient) -> queue.remove(ManageableMailQueue.Type.Recipient, recipient.asString())));

        return Result.COMPLETED;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Optional<TaskExecutionDetails.AdditionalInformation> details() {
        return Optional.of(additionalInformation);
    }

    public long getRemainingSize() {
        try {
            return queue.getSize();
        } catch (MailQueue.MailQueueException e) {
            throw new RuntimeException(e);
        }
    }
}
