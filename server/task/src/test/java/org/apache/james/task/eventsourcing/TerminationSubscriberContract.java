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

import org.apache.james.eventsourcing.Event;
import org.apache.james.eventsourcing.EventId;
import org.apache.james.task.Task;
import org.apache.james.task.TaskId;

import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

public interface TerminationSubscriberContract {
    TerminationSubscriber subscriber();

    @Test
    default void handlingCompletedShouldBeListed() {
        TerminationSubscriber subscriber = subscriber();
        TaskEvent event = new Completed(new TaskAggregateId(TaskId.generateTaskId()), EventId.fromSerialized(42), Task.Result.COMPLETED);

        subscriber.handle(event);

        assertEvents(subscriber).containsOnly(event);
    }

    @Test
    default void handlingFailedShouldBeListed() {
        TerminationSubscriber subscriber = subscriber();
        TaskEvent event = new Failed(new TaskAggregateId(TaskId.generateTaskId()), EventId.fromSerialized(42));

        subscriber.handle(event);

        assertEvents(subscriber).containsOnly(event);
    }

    @Test
    default void handlingCancelledShouldBeListed() {
        TerminationSubscriber subscriber = subscriber();
        TaskEvent event = new Cancelled(new TaskAggregateId(TaskId.generateTaskId()), EventId.fromSerialized(42));

        subscriber.handle(event);

        assertEvents(subscriber).containsOnly(event);
    }

    @Test
    default void handlingNonTerminalEventShouldNotBeListed() {
        TerminationSubscriber subscriber = subscriber();
        TaskEvent event = new Started(new TaskAggregateId(TaskId.generateTaskId()), EventId.fromSerialized(42), new Hostname("foo"));

        subscriber.handle(event);

        assertEvents(subscriber).isEmpty();
    }

    @Test
    default void handlingMultipleEventsShouldBeListed() {
        TerminationSubscriber subscriber = subscriber();
        TaskEvent event1 = new Completed(new TaskAggregateId(TaskId.generateTaskId()), EventId.fromSerialized(42), Task.Result.COMPLETED);
        TaskEvent event2 = new Failed(new TaskAggregateId(TaskId.generateTaskId()), EventId.fromSerialized(42));
        TaskEvent event3 = new Cancelled(new TaskAggregateId(TaskId.generateTaskId()), EventId.fromSerialized(42));

        subscriber.handle(event1);
        subscriber.handle(event2);
        subscriber.handle(event3);

        assertEvents(subscriber).containsExactly(event1, event2, event3);
    }

    @Test
    default void listeningEventsShouldBeContinuous() {
        TerminationSubscriber subscriber = subscriber();
        TaskEvent event1 = new Completed(new TaskAggregateId(TaskId.generateTaskId()), EventId.fromSerialized(42), Task.Result.COMPLETED);
        TaskEvent event2 = new Failed(new TaskAggregateId(TaskId.generateTaskId()), EventId.fromSerialized(42));
        TaskEvent event3 = new Cancelled(new TaskAggregateId(TaskId.generateTaskId()), EventId.fromSerialized(42));

        Flux.just(event1, event2, event3)
            .subscribeOn(Schedulers.elastic())
            .delayElements(Duration.ofMillis(20))
            .doOnNext(subscriber::handle)
            .subscribe();

        assertEvents(subscriber).containsExactly(event1, event2, event3);
    }

    default ListAssert<Event> assertEvents(TerminationSubscriber subscriber) {
        return assertThat(Flux.from(subscriber.listenEvents())
            .subscribeOn(Schedulers.elastic())
            .take(Duration.ofMillis(200))
            .collectList()
            .block());
    }
}