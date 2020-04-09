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

package org.apache.james.mailbox.inmemory.mail;

import static org.apache.james.mailbox.store.mail.AbstractMessageMapper.UNLIMITED;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import javax.mail.Flags;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.mailbox.MessageManager.FlagsUpdateMode;
import org.apache.james.mailbox.model.Mailbox;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.model.UpdatedFlags;
import org.apache.james.mailbox.store.FlagsUpdateCalculator;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.MessageIdMapper;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;

import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class InMemoryMessageIdMapper implements MessageIdMapper {
    private final MailboxMapper mailboxMapper;
    private final InMemoryMessageMapper messageMapper;

    public InMemoryMessageIdMapper(MailboxMapper mailboxMapper, InMemoryMessageMapper messageMapper) {
        this.mailboxMapper = mailboxMapper;
        this.messageMapper = messageMapper;
    }

    @Override
    public Flux<MailboxMessage> find(Collection<MessageId> messageIds, MessageMapper.FetchType fetchType) {
        return mailboxMapper.list()
            .flatMap(mailbox -> messageMapper.findInMailbox(mailbox, MessageRange.all(), fetchType, UNLIMITED))
            .filter(message -> messageIds.contains(message.getMessageId()));
    }

    @Override
    public Flux<MailboxId> findMailboxes(MessageId messageId) {
        return find(ImmutableList.of(messageId), MessageMapper.FetchType.Metadata)
            .map(MailboxMessage::getMailboxId);
    }

    @Override
    public Mono<Void> save(MailboxMessage mailboxMessage) {
        return mailboxMapper.findMailboxById(mailboxMessage.getMailboxId())
            .flatMap(mailbox -> Mono.fromCallable(() -> messageMapper.save(mailbox, mailboxMessage)))
            .then();
    }

    @Override
    public Mono<Void> copyInMailbox(MailboxMessage mailboxMessage) {
        return findMailboxes(mailboxMessage.getMessageId())
            .collect(Guavate.toImmutableSet())
            .filter(mailboxIds -> !mailboxIds.contains(mailboxMessage.getMailboxId()))
            .flatMap(ignored -> save(mailboxMessage));
    }

    @Override
    public Mono<Void> delete(MessageId messageId) {
        return find(ImmutableList.of(messageId), MessageMapper.FetchType.Metadata)
            .flatMap(message -> mailboxMapper.findMailboxById(message.getMailboxId())
                    .doOnNext(mailbox -> messageMapper.delete(mailbox, message)))
            .then();
    }

    @Override
    public Mono<Void> delete(MessageId messageId, Collection<MailboxId> mailboxIds) {
        return find(ImmutableList.of(messageId), MessageMapper.FetchType.Metadata)
            .filter(message -> mailboxIds.contains(message.getMailboxId()))
            .flatMap(message -> mailboxMapper.findMailboxById(message.getMailboxId())
                    .doOnNext(mailbox -> messageMapper.delete(mailbox, message)))
            .then();
    }

    @Override
    public Mono<Multimap<MailboxId, UpdatedFlags>> setFlags(MessageId messageId, List<MailboxId> mailboxIds,
                                                      Flags newState, FlagsUpdateMode updateMode) {
        return find(ImmutableList.of(messageId), MessageMapper.FetchType.Metadata)
            .filter(message -> mailboxIds.contains(message.getMailboxId()))
            .flatMap(updateMessage(newState, updateMode))
            .distinct()
            .collect(Guavate.toImmutableListMultimap(
                Pair::getKey,
                Pair::getValue));
    }

    private Function<MailboxMessage, Mono<Pair<MailboxId, UpdatedFlags>>> updateMessage(Flags newState, FlagsUpdateMode updateMode) {
        return (MailboxMessage message) -> Mono.defer(() -> {
            FlagsUpdateCalculator flagsUpdateCalculator = new FlagsUpdateCalculator(newState, updateMode);
            if (flagsUpdateCalculator.buildNewFlags(message.createFlags()).equals(message.createFlags())) {
                return Mono.just(UpdatedFlags.builder()
                    .modSeq(message.getModSeq())
                    .uid(message.getUid())
                    .oldFlags(message.createFlags())
                    .newFlags(newState)
                    .build());
            }
            return mailboxMapper.findMailboxById(message.getMailboxId())
                .map(Throwing.<Mailbox, UpdatedFlags>function(mailbox ->
                    messageMapper.updateFlags(
                            mailbox,
                            flagsUpdateCalculator,
                            message.getUid().toRange())
                            .next()).sneakyThrow());
        })
        .map(updatedFlags -> Pair.of(message.getMailboxId(), updatedFlags));
    }
}
