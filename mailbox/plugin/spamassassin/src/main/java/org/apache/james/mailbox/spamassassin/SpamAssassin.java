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

import java.io.Closeable;
import java.io.InputStream;
import java.util.Optional;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.apache.james.core.Username;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.spamassassin.SpamAssassinInvoker;
import org.apache.james.util.Host;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;
import reactor.core.scheduler.Schedulers;
import reactor.util.concurrent.Queues;
import reactor.util.function.Tuple3;

public class SpamAssassin implements Closeable {
    private enum Kind { HAM, SPAM }

    private final MetricFactory metricFactory;
    private Optional<UnicastProcessor<Tuple3<InputStream, Username, Kind>>> queue;
    private Optional<Disposable> handle;

    @Inject
    public SpamAssassin(MetricFactory metricFactory, SpamAssassinConfiguration spamAssassinConfiguration) {
        this.metricFactory = metricFactory;

        createListeners(spamAssassinConfiguration);
    }

    public void learnSpam(Flux<InputStream> messages, Username username) {
        queue.ifPresent(queue ->
            Flux.zip(messages, Mono.just(username).repeat(), Mono.just(Kind.SPAM).repeat())
                .doOnNext(queue::onNext)
                .then()
                .block()
        );
    }

    public void learnHam(Flux<InputStream> messages, Username username) {
        queue.ifPresent(queue ->
            Flux.zip(messages, Mono.just(username).repeat(), Mono.just(Kind.HAM).repeat())
                .doOnNext(queue::onNext)
                .then()
                .block()
        );
    }

    @Override
    @PreDestroy
    public void close() {
        handle.ifPresent(Disposable::dispose);
    }

    private void createListeners(SpamAssassinConfiguration configuration) {
        if (configuration.isEnable()) {
            Host host = configuration.getHost().get();
            SpamAssassinInvoker invoker = new SpamAssassinInvoker(metricFactory, host.getHostName(), host.getPort());

            UnicastProcessor<Tuple3<InputStream, Username, Kind>> processor = UnicastProcessor.create(Queues.<Tuple3<InputStream, Username, Kind>>one().get());
            queue = Optional.of(processor);
            handle = Optional.of(processor
                .flatMap(messageUsername -> Mono.fromCallable(() -> {
                    switch (messageUsername.getT3()) {
                        case HAM:
                            invoker.learnAsHam(messageUsername.getT1(), messageUsername.getT2());
                            break;
                        case SPAM:
                            invoker.learnAsSpam(messageUsername.getT1(), messageUsername.getT2());
                            break;
                    }

                    return false;
                }), configuration.getConcurrency().get(), 1)
                .subscribeOn(Schedulers.elastic())
                .subscribe());
        } else {
            queue = Optional.empty();
            handle = Optional.empty();
        }
    }
}
