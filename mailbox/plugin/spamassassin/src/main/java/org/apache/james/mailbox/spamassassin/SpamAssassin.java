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

import com.github.fge.lambdas.Throwing;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

public class SpamAssassin implements Closeable {

    private final MetricFactory metricFactory;
    private Optional<UnicastProcessor<Tuple2<InputStream, Username>>> hamQueue;
    private Optional<UnicastProcessor<Tuple2<InputStream, Username>>> spamQueue;
    private Optional<Disposable> hamHandle;
    private Optional<Disposable> spamHandle;

    @Inject
    public SpamAssassin(MetricFactory metricFactory, SpamAssassinConfiguration spamAssassinConfiguration) {
        this.metricFactory = metricFactory;

        createListeners(spamAssassinConfiguration);
    }

    public void learnSpam(Flux<InputStream> messages, Username username) {
        spamQueue.ifPresent(queue ->
            messages.zipWith(Mono.just(username).repeat())
                .doOnNext(queue::onNext)
                .then()
                .block()
        );
    }

    public void learnHam(Flux<InputStream> messages, Username username) {
        hamQueue.ifPresent(queue ->
            messages.zipWith(Mono.just(username).repeat())
                .doOnNext(queue::onNext)
                .then()
                .block()
        );
    }

    @Override
    @PreDestroy
    public void close() {
        hamHandle.ifPresent(Disposable::dispose);
        spamHandle.ifPresent(Disposable::dispose);
    }

    private void createListeners(SpamAssassinConfiguration spamAssassinConfiguration) {
        if (spamAssassinConfiguration.isEnable()) {
            Host host = spamAssassinConfiguration.getHost().get();
            SpamAssassinInvoker invoker = new SpamAssassinInvoker(metricFactory, host.getHostName(), host.getPort());

            UnicastProcessor<Tuple2<InputStream, Username>> ham = UnicastProcessor.create();
            hamQueue = Optional.of(ham);
            hamHandle = Optional.of(ham
                .doOnNext(Throwing.consumer(messageUsername -> invoker.learnAsHam(messageUsername.getT1(), messageUsername.getT2())))
                .subscribeOn(Schedulers.elastic())
                .subscribe());

            UnicastProcessor<Tuple2<InputStream, Username>> spam = UnicastProcessor.create();
            spamQueue = Optional.of(spam);
            spamHandle = Optional.of(spam
                .doOnNext(Throwing.consumer(messageUsername -> invoker.learnAsHam(messageUsername.getT1(), messageUsername.getT2())))
                .subscribeOn(Schedulers.elastic())
                .subscribe());
        } else {
            hamQueue = Optional.empty();
            hamHandle = Optional.empty();

            spamQueue = Optional.empty();
            spamHandle = Optional.empty();
        }
    }
}
