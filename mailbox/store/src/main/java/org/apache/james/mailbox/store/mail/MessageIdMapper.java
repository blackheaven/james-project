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
package org.apache.james.mailbox.store.mail;

import java.util.Collection;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.UpdatedFlags;
import org.apache.james.mailbox.store.mail.MessageMapper.FetchType;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;

import com.google.common.collect.Multimap;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MessageIdMapper {

    Flux<MailboxMessage> find(Collection<MessageId> messageIds, FetchType fetchType);

    Flux<MailboxId> findMailboxes(MessageId messageId);

    Mono<Void> save(MailboxMessage mailboxMessage);

    Mono<Void> copyInMailbox(MailboxMessage mailboxMessage);

    Mono<Void> delete(MessageId messageId);

    Mono<Void> delete(MessageId messageId, Collection<MailboxId> mailboxIds);

    default Mono<Void> delete(Multimap<MessageId, MailboxId> ids) {
        return Flux.fromIterable(ids.asMap().entrySet())
            .flatMap(entry -> delete(entry.getKey(), entry.getValue()))
            .then();
    }

    /**
     * Updates the flags of the messages with the given MessageId in the supplied mailboxes
     *
     * More one message can be updated when a message is contained several time in the same mailbox with distinct
     * MessageUid.
     *
     * @return Metadata of the update, indexed by mailboxIds.
     */
    Mono<Multimap<MailboxId, UpdatedFlags>> setFlags(MessageId messageId, List<MailboxId> mailboxIds, Flags newState, MessageManager.FlagsUpdateMode updateMode);
}
