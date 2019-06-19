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

import static reactor.rabbitmq.ChannelCloseHandlers.SENDER_CHANNEL_CLOSE_HANDLER_INSTANCE;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.Throwing;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.ChannelPool;
import reactor.rabbitmq.ChannelPoolOptions;
import reactor.rabbitmq.RabbitFluxException;

public class ResilientChannelPool implements ChannelPool {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResilientChannelPool.class);
    private static final int DEFAULT_CHANNEL_POOL_SIZE = 5;
    private static final int NUM_RETRIES = 100;
    private static final Duration RETRY_DELAY = Duration.ofMillis(20);

    private final Mono<? extends Connection> connectionMono;
    private final BlockingQueue<Channel> channelsQueue;
    private final Scheduler subscriptionScheduler;
    private final AtomicBoolean isOpen;

    public static ChannelPool createChannelPool(Mono<? extends Connection> connectionMono) {
        return createChannelPool(connectionMono, new ChannelPoolOptions());
    }

    public static ChannelPool createChannelPool(Mono<? extends Connection> connectionMono, ChannelPoolOptions channelPoolOptions) {
        return new ResilientChannelPool(connectionMono, channelPoolOptions);
    }

    private ResilientChannelPool(Mono<? extends Connection> connectionMono, ChannelPoolOptions channelPoolOptions) {
        int channelsQueueCapacity = channelPoolOptions.getMaxCacheSize() == null ?
                DEFAULT_CHANNEL_POOL_SIZE : channelPoolOptions.getMaxCacheSize();
        this.channelsQueue = new LinkedBlockingQueue<>(channelsQueueCapacity);
        this.connectionMono = connectionMono;
        this.subscriptionScheduler = channelPoolOptions.getSubscriptionScheduler() == null ?
                Schedulers.newElastic("sender-channel-pool") : channelPoolOptions.getSubscriptionScheduler();

        isOpen = new AtomicBoolean(true);

        IntStream.rangeClosed(1, channelsQueueCapacity)
            .forEach(ignored -> addChannel());
        System.out.println("ResilientChannelPool.size " + channelsQueue.size());
    }

    public Mono<? extends Channel> getChannelMono() {
        return Mono.defer(Throwing.supplier(() -> {
            Mono<Channel> l = Mono.just(channelsQueue.take());
            System.out.println("take one channel");
            return l;
        }).sneakyThrow())
            .publishOn(subscriptionScheduler);
    }

    @Override
    public BiConsumer<SignalType, Channel> getChannelCloseHandler() {
        return Throwing.<SignalType, Channel>biConsumer((signalType, channel) -> {
            System.out.println("channel put back");
            LOGGER.debug("channel put back");
            if (channel.isOpen() && isOpen.get()) {
                channelsQueue.put(channel);
            } else {
                System.out.println("Channel closed");
                SENDER_CHANNEL_CLOSE_HANDLER_INSTANCE.accept(signalType, channel);
                addChannel();
            }
        }).sneakyThrow();
    }

    private void addChannel() {
        if (!isOpen.get()) {
            System.out.println("add channel shortcut closed");
            return;
        }
        System.out.println("add channel");
        LOGGER.debug("add channel");
        Channel newChannel = connectionMono.flatMap(connection -> {
            Channel channel =  createChannel(connection);
            if (channel == null) {
                System.out.println("channel is null on " + Thread.currentThread().getName());
                LOGGER.error("channel is null on " + Thread.currentThread().getName());
            }
            return Mono.justOrEmpty(channel);
        })
        .single()
        .retryWhen(companion -> companion
        .zipWith(Flux.range(0, NUM_RETRIES), (error, index) -> {
            if (!(error instanceof NoSuchElementException) || index == NUM_RETRIES) {
                throw Exceptions.propagate(error);
            }

            System.out.println("channel retry number " + index);
            LOGGER.warn("channel retry number " + index);
            return Mono.delay(RETRY_DELAY);
        }))
        .subscribeOn(Schedulers.elastic())
        .block();

        try {
            channelsQueue.put(newChannel);
            System.out.println("channel added");
            LOGGER.debug("channel added");
        } catch (InterruptedException e) {
            System.out.println("channel not added");
            LOGGER.debug("channel not added");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        isOpen.set(false);
        List<Channel> channels = new ArrayList<>();
        channelsQueue.drainTo(channels);
        System.out.println("closing " + channels.size() + " channels");
        channels.forEach(channel -> {
            SENDER_CHANNEL_CLOSE_HANDLER_INSTANCE.accept(SignalType.ON_COMPLETE, channel);
        });
    }

    private Channel createChannel(Connection connection) {
        try {
            return connection.createChannel();
        } catch (IOException e) {
            throw new RabbitFluxException("Error while creating channel", e);
        }
    }

}
