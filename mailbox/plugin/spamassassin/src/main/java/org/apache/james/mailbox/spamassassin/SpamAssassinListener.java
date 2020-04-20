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
package org.apache.james.mailbox.spamassassin;

import java.io.InputStream;
import java.util.function.Function;

import javax.inject.Inject;

import org.apache.james.core.Username;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.Role;
import org.apache.james.mailbox.SystemMailboxesProvider;
import org.apache.james.mailbox.events.Event;
import org.apache.james.mailbox.events.Group;
import org.apache.james.mailbox.events.MessageMoveEvent;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.Mailbox;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.event.SpamEventListener;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.mail.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.Throwing;
import com.google.common.annotations.VisibleForTesting;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class SpamAssassinListener implements SpamEventListener {
    public static class SpamAssassinListenerGroup extends Group {

    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SpamAssassinListener.class);
    private static final int LIMIT = 1;
    private static final Group GROUP = new SpamAssassinListenerGroup();

    private final SpamAssassin spamAssassin;
    private final SystemMailboxesProvider systemMailboxesProvider;
    private final MailboxManager mailboxManager;
    private final MailboxSessionMapperFactory mapperFactory;
    private final ExecutionMode executionMode;

    @Inject
    public SpamAssassinListener(SpamAssassin spamAssassin, SystemMailboxesProvider systemMailboxesProvider, MailboxManager mailboxManager, MailboxSessionMapperFactory mapperFactory, ExecutionMode executionMode) {
        this.spamAssassin = spamAssassin;
        this.systemMailboxesProvider = systemMailboxesProvider;
        this.mailboxManager = mailboxManager;
        this.mapperFactory = mapperFactory;
        this.executionMode = executionMode;
    }

    @Override
    public Group getDefaultGroup() {
        return GROUP;
    }

    @Override
    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    @Override
    public boolean isHandling(Event event) {
        return event instanceof MessageMoveEvent || event instanceof Added;
    }

    @Override
    public void event(Event event) {
        Username username = Username.of(getClass().getCanonicalName());
        if (event instanceof MessageMoveEvent) {
            MailboxSession session = mailboxManager.createSystemSession(username);
            handleMessageMove(event, session, (MessageMoveEvent) event);
        }
        if (event instanceof Added) {
            MailboxSession session = mailboxManager.createSystemSession(username);
            handleAdded(event, session, (Added) event);
        }
    }

    private void handleAdded(Event event, MailboxSession session, Added addedEvent) {
        if (isAppendedToInbox(addedEvent)) {
            Mailbox mailbox = mapperFactory.getMailboxMapper(session).findMailboxById(addedEvent.getMailboxId()).block();
            MessageMapper messageMapper = mapperFactory.getMessageMapper(session);

            Flux<InputStream> contents = Mono.fromCallable(() -> MessageRange.toRanges(addedEvent.getUids()))
                .flatMapIterable(Function.identity())
                .flatMap(range -> retrieveMessages(messageMapper, mailbox, range))
                .map(Throwing.function(MailboxMessage::getFullContent));
            spamAssassin.learnHam(contents, event.getUsername());
        }
    }

    private void handleMessageMove(Event event, MailboxSession session, MessageMoveEvent messageMoveEvent) {
        if (isMessageMovedToSpamMailbox(messageMoveEvent)) {
            LOGGER.debug("Spam event detected");
            Flux<InputStream> messages = retrieveMessages(messageMoveEvent, session);
            spamAssassin.learnSpam(messages, event.getUsername());
        }
        if (isMessageMovedOutOfSpamMailbox(messageMoveEvent)) {
            Flux<InputStream> messages = retrieveMessages(messageMoveEvent, session);
            spamAssassin.learnHam(messages, event.getUsername());
        }
    }

    private Flux<MailboxMessage> retrieveMessages(MessageMapper messageMapper, Mailbox mailbox, MessageRange range) {
        return messageMapper.findInMailbox(mailbox, range, MessageMapper.FetchType.Full, LIMIT)
            .onErrorResume(MailboxException.class, e -> {
                LOGGER.warn("Can not retrieve message {} {}", mailbox.getMailboxId(), range.toString(), e);
                return Flux.just();
            });
    }

    private boolean isAppendedToInbox(Added addedEvent) {
        try {
            return systemMailboxesProvider.findMailbox(Role.INBOX, addedEvent.getUsername())
                .getId().equals(addedEvent.getMailboxId());
        } catch (MailboxException e) {
            LOGGER.warn("Could not resolve Inbox mailbox", e);
            return false;
        }
    }

    private Flux<InputStream> retrieveMessages(MessageMoveEvent messageMoveEvent, MailboxSession session) {
        return mapperFactory.getMessageIdMapper(session)
            .find(messageMoveEvent.getMessageIds(), MessageMapper.FetchType.Full)
            .map(Throwing.function(Message::getFullContent));
    }

    @VisibleForTesting
    boolean isMessageMovedToSpamMailbox(MessageMoveEvent event) {
        try {
            MailboxId spamMailboxId = systemMailboxesProvider.findMailbox(Role.SPAM, event.getUsername()).getId();

            return event.getMessageMoves().addedMailboxIds().contains(spamMailboxId);
        } catch (MailboxException e) {
            LOGGER.warn("Could not resolve Spam mailbox", e);
            return false;
        }
    }

    @VisibleForTesting
    boolean isMessageMovedOutOfSpamMailbox(MessageMoveEvent event) {
        try {
            MailboxId spamMailboxId = systemMailboxesProvider.findMailbox(Role.SPAM, event.getUsername()).getId();
            MailboxId trashMailboxId = systemMailboxesProvider.findMailbox(Role.TRASH, event.getUsername()).getId();

            return event.getMessageMoves().removedMailboxIds().contains(spamMailboxId)
                && !event.getMessageMoves().addedMailboxIds().contains(trashMailboxId);
        } catch (MailboxException e) {
            LOGGER.warn("Could not resolve Spam mailbox", e);
            return false;
        }
    }
}
