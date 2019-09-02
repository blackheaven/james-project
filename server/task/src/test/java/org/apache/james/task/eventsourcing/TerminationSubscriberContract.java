/**
 * *************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                   *
 * *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ***************************************************************/

package org.apache.james.task.eventsourcing;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.apache.james.eventsourcing.Event;
import org.apache.james.eventsourcing.EventId;
import org.apache.james.task.Task;
import org.apache.james.task.TaskId;

import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public interface TerminationSubscriberContract {

    Completed COMPLETED_EVENT = new Completed(new TaskAggregateId(TaskId.generateTaskId()), EventId.fromSerialized(42), Task.Result.COMPLETED);
    Failed FAILED_EVENT = new Failed(new TaskAggregateId(TaskId.generateTaskId()), EventId.fromSerialized(42));
    Cancelled CANCELLED_EVENT = new Cancelled(new TaskAggregateId(TaskId.generateTaskId()), EventId.fromSerialized(42));
    int DELAY_BETWEEN_EVENTS = 50;
    int DELAY_BEFORE_PUBLISHING = 50;

    TerminationSubscriber subscriber();

    @Test
    default void handlingCompletedShouldBeListed() {
        TerminationSubscriber subscriber = subscriber();

        sendEvents(subscriber, COMPLETED_EVENT);

        assertEvents(subscriber).containsOnly(COMPLETED_EVENT);
    }

    @Test
    default void handlingFailedShouldBeListed() {
        TerminationSubscriber subscriber = subscriber();

        sendEvents(subscriber, FAILED_EVENT);

        assertEvents(subscriber).containsOnly(FAILED_EVENT);
    }

    @Test
    default void handlingCancelledShouldBeListed() {
        TerminationSubscriber subscriber = subscriber();

        sendEvents(subscriber, CANCELLED_EVENT);

        assertEvents(subscriber).containsOnly(CANCELLED_EVENT);
    }

    @Test
    default void handlingNonTerminalEventShouldNotBeListed() {
        TerminationSubscriber subscriber = subscriber();
        TaskEvent event = new Started(new TaskAggregateId(TaskId.generateTaskId()), EventId.fromSerialized(42), new Hostname("foo"));

        sendEvents(subscriber, event);

        assertEvents(subscriber).isEmpty();
    }

    @Test
    default void handlingMultipleEventsShouldBeListed() {
        TerminationSubscriber subscriber = subscriber();

        sendEvents(subscriber, COMPLETED_EVENT, FAILED_EVENT, CANCELLED_EVENT);

        assertEvents(subscriber).containsExactly(COMPLETED_EVENT, FAILED_EVENT, CANCELLED_EVENT);
    }

    @Test
    default void multipleListeningEventsShouldShareEvents() {
        TerminationSubscriber subscriber = subscriber();

        sendEvents(subscriber, COMPLETED_EVENT, FAILED_EVENT, CANCELLED_EVENT);

        List<List<Event>> listenedEvents = Flux.range(0, 2)
            .subscribeOn(Schedulers.elastic())
            .flatMap(ignored -> collectEvents(subscriber))
            .collectList()
            .block();
        assertThat(listenedEvents).hasSize(2);
        assertThat(listenedEvents.get(0)).containsExactly(COMPLETED_EVENT, FAILED_EVENT, CANCELLED_EVENT);
        assertThat(listenedEvents.get(1)).isEqualTo(listenedEvents.get(0));
    }

    @Test
    default void dynamicListeningEventsShouldGetOnlyNewEvents() {
        TerminationSubscriber subscriber = subscriber();

        sendEvents(subscriber, COMPLETED_EVENT, FAILED_EVENT, CANCELLED_EVENT);

        List<Event> listenedEvents = Mono.delay(Duration.ofMillis(Math.round(DELAY_BEFORE_PUBLISHING + 1.5 * DELAY_BETWEEN_EVENTS)))
            .then(Mono.defer(() -> collectEvents(subscriber)))
            .subscribeOn(Schedulers.elastic())
            .block();
        assertThat(listenedEvents).containsExactly(FAILED_EVENT, CANCELLED_EVENT);
    }

    default ListAssert<Event> assertEvents(TerminationSubscriber subscriber) {
        return assertThat(collectEvents(subscriber)
            .block());
    }

    default Mono<List<Event>> collectEvents(TerminationSubscriber subscriber) {
        return Flux.from(subscriber.listenEvents())
            .subscribeOn(Schedulers.elastic())
            .take(Duration.ofMillis(DELAY_BEFORE_PUBLISHING + 7 * DELAY_BETWEEN_EVENTS))
            .collectList();
    }

    default void sendEvents(TerminationSubscriber subscriber, Event... events) {
        Mono.delay(Duration.ofMillis(DELAY_BEFORE_PUBLISHING))
            .flatMapMany(ignored -> Flux.fromArray(events)
                .subscribeOn(Schedulers.elastic())
                .delayElements(Duration.ofMillis(DELAY_BETWEEN_EVENTS))
                .doOnNext(subscriber::handle))
            .subscribe();
    }
}