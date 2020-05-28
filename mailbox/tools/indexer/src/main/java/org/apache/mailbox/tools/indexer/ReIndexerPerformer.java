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

package org.apache.mailbox.tools.indexer;

import javax.inject.Inject;

import org.apache.james.core.Username;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.indexer.ReIndexingExecutionFailures;
import org.apache.james.mailbox.model.Mailbox;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxMetaData;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.model.search.MailboxQuery;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.search.ListeningMessageSearchIndex;
import org.apache.james.task.Task;
import org.apache.james.task.Task.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ReIndexerPerformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReIndexerPerformer.class);

    private static final int SINGLE_MESSAGE = 1;
    private static final int MESSAGE_CONCURRENCY = 50;
    private static final String RE_INDEXING = "re-indexing";
    private static final Username RE_INDEXER_PERFORMER_USER = Username.of(RE_INDEXING);
    public static final int NO_CONCURRENCY = 1;
    public static final int NO_PREFETCH = 1;

    private final MailboxManager mailboxManager;
    private final ListeningMessageSearchIndex messageSearchIndex;
    private final MailboxSessionMapperFactory mailboxSessionMapperFactory;

    @Inject
    public ReIndexerPerformer(MailboxManager mailboxManager,
                              ListeningMessageSearchIndex messageSearchIndex,
                              MailboxSessionMapperFactory mailboxSessionMapperFactory) {
        this.mailboxManager = mailboxManager;
        this.messageSearchIndex = messageSearchIndex;
        this.mailboxSessionMapperFactory = mailboxSessionMapperFactory;
    }

    Mono<Result> reIndex(MailboxId mailboxId, ReprocessingContext reprocessingContext) {
        MailboxSession mailboxSession = mailboxManager.createSystemSession(RE_INDEXER_PERFORMER_USER);

        return mailboxSessionMapperFactory.getMailboxMapper(mailboxSession)
            .findMailboxById(mailboxId)
            .flatMap(mailbox -> reIndex(reprocessingContext, mailboxSession, mailbox));
    }

    private Mono<Result> reIndex(ReprocessingContext reprocessingContext, MailboxSession mailboxSession, Mailbox mailbox) {
        LOGGER.info("Attempt to reindex mailbox with mailboxId {}", mailbox.getMailboxId().serialize());
        return messageSearchIndex.deleteAll(mailboxSession, mailbox.getMailboxId())
            .then(mailboxSessionMapperFactory.getMessageMapper(mailboxSession)
                .listAllMessageUids(mailbox)
                .flatMap(uid -> handleMessageReIndexing(mailboxSession, mailbox, uid, reprocessingContext), MESSAGE_CONCURRENCY)
                .reduce(Task::combine)
                .switchIfEmpty(Mono.just(Result.COMPLETED))
                .doFinally(any -> LOGGER.info("Finish to reindex mailbox with mailboxId {}", mailbox.getMailboxId().serialize())));
    }

    Mono<Result> reIndex(ReprocessingContext reprocessingContext, ReIndexingExecutionFailures previousReIndexingFailures) {
        return Flux.fromIterable(previousReIndexingFailures.failures())
            .flatMap(previousFailure -> reIndex(reprocessingContext, previousFailure), MESSAGE_CONCURRENCY)
            .reduce(Task::combine)
            .switchIfEmpty(Mono.just(Result.COMPLETED));
    }

    private Mono<Result> reIndex(ReprocessingContext reprocessingContext, ReIndexingExecutionFailures.ReIndexingFailure previousReIndexingFailure) {
        MailboxId mailboxId = previousReIndexingFailure.getMailboxId();
        MessageUid uid = previousReIndexingFailure.getUid();

        return handleMessageReIndexing(mailboxId, uid, reprocessingContext)
            .onErrorResume(e -> {
                LOGGER.warn("ReIndexing failed for {} {}", mailboxId, uid, e);
                reprocessingContext.recordFailureDetailsForMessage(mailboxId, uid);
                return Mono.just(Result.PARTIAL);
            });
    }

    Mono<Result> reIndex(ReprocessingContext reprocessingContext) {
        MailboxSession mailboxSession = mailboxManager.createSystemSession(RE_INDEXER_PERFORMER_USER);
        LOGGER.info("Starting a full reindex");
        return mailboxSessionMapperFactory.getMailboxMapper(mailboxSession).list()
            .flatMap(mailbox -> reIndex(reprocessingContext, mailboxSession, mailbox), NO_CONCURRENCY, NO_PREFETCH)
            .reduce(Task::combine)
            .switchIfEmpty(Mono.just(Result.COMPLETED))
            .doFinally(any -> LOGGER.info("Full reindex finished"));
    }

    Mono<Result> reIndex(Username username, ReprocessingContext reprocessingContext) {
        MailboxSession mailboxSession = mailboxManager.createSystemSession(username);
        LOGGER.info("Starting a reindex for user {}", username.asString());

        MailboxQuery mailboxQuery = MailboxQuery.privateMailboxesBuilder(mailboxSession).build();

        return mailboxManager.search(mailboxQuery, mailboxSession)
            .map(MailboxMetaData::getId)
            .flatMap(id -> reIndex(id, reprocessingContext), NO_CONCURRENCY, NO_PREFETCH)
            .reduce(Task::combine)
            .switchIfEmpty(Mono.just(Result.COMPLETED))
            .doFinally(any -> LOGGER.info("User {} reindex finished", username.asString()));
    }

    Mono<Result> handleMessageReIndexing(MailboxId mailboxId, MessageUid uid, ReprocessingContext reprocessingContext) {
        MailboxSession mailboxSession = mailboxManager.createSystemSession(RE_INDEXER_PERFORMER_USER);

        return mailboxSessionMapperFactory.getMailboxMapper(mailboxSession)
            .findMailboxById(mailboxId)
            .flatMap(mailbox -> handleMessageReIndexing(mailboxSession, mailbox, uid, reprocessingContext));
    }

    Mono<Result> handleMessageIdReindexing(MessageId messageId) {
        MailboxSession session = mailboxManager.createSystemSession(RE_INDEXER_PERFORMER_USER);

        return mailboxSessionMapperFactory.getMessageIdMapper(session)
            .find(ImmutableList.of(messageId), MessageMapper.FetchType.Full)
            .flatMap(mailboxMessage -> reIndex(mailboxMessage, session))
            .reduce(Task::combine)
            .switchIfEmpty(Mono.just(Result.COMPLETED))
            .onErrorResume(e -> {
                LOGGER.warn("Failed to re-index {}", messageId, e);
                return Mono.just(Result.PARTIAL);
            });
    }

    private Mono<Result> reIndex(MailboxMessage mailboxMessage, MailboxSession session) {
        return mailboxSessionMapperFactory.getMailboxMapper(session)
            .findMailboxById(mailboxMessage.getMailboxId())
            .flatMap(mailbox -> messageSearchIndex.add(session, mailbox, mailboxMessage))
            .thenReturn(Result.COMPLETED)
            .onErrorResume(e -> {
                LOGGER.warn("Failed to re-index {} in {}", mailboxMessage.getUid(), mailboxMessage.getMailboxId(), e);
                return Mono.just(Result.PARTIAL);
            });
    }

    private Mono<Result> handleMessageReIndexing(MailboxSession mailboxSession, Mailbox mailbox, MessageUid uid, ReprocessingContext reprocessingContext) {
        return fullyReadMessage(mailboxSession, mailbox, uid)
            .flatMap(message -> messageSearchIndex.add(mailboxSession, mailbox, message))
            .thenReturn(Result.COMPLETED)
            .doOnNext(any -> reprocessingContext.recordSuccess())
            .onErrorResume(e -> {
                LOGGER.warn("ReIndexing failed for {} {}", mailbox.generateAssociatedPath(), uid, e);
                reprocessingContext.recordFailureDetailsForMessage(mailbox.getMailboxId(), uid);
                return Mono.just(Result.PARTIAL);
            });
    }

    private Mono<MailboxMessage> fullyReadMessage(MailboxSession mailboxSession, Mailbox mailbox, MessageUid mUid) {
        return mailboxSessionMapperFactory.getMessageMapper(mailboxSession)
            .findInMailbox(mailbox, MessageRange.one(mUid), MessageMapper.FetchType.Full, SINGLE_MESSAGE)
            .next();
    }
}
