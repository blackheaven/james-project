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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.ChannelPool;
import reactor.rabbitmq.ChannelPoolOptions;
import reactor.rabbitmq.RabbitFluxException;

class LazyChannelPool implements ChannelPool {

    private static final int DEFAULT_CHANNEL_POOL_SIZE = 5;

    private final Mono<? extends Connection> connectionMono;
    private final BlockingQueue<Channel> channelsQueue;
    private final Scheduler subscriptionScheduler;
    private final int channelsQueueCapacity;

    LazyChannelPool(Mono<? extends Connection> connectionMono, ChannelPoolOptions channelPoolOptions) {
        channelsQueueCapacity = channelPoolOptions.getMaxCacheSize() == null ?
                DEFAULT_CHANNEL_POOL_SIZE : channelPoolOptions.getMaxCacheSize();
        this.channelsQueue = new LinkedBlockingQueue<>(channelsQueueCapacity);
        this.connectionMono = connectionMono;
        this.subscriptionScheduler = channelPoolOptions.getSubscriptionScheduler() == null ?
                Schedulers.newElastic("sender-channel-pool") : channelPoolOptions.getSubscriptionScheduler();
    }

    public Mono<? extends Channel> getChannelMono() {
        return connectionMono.map(connection -> {
            Channel channel = channelsQueue.poll();
            if (channel == null) {
                channel = createChannel(connection);
            }
            return channel;
        })
                .subscribeOn(subscriptionScheduler);
    }

    @Override
    public BiConsumer<SignalType, Channel> getChannelCloseHandler() {
        return (signalType, channel) -> {
            if (!channel.isOpen()) {
                return;
            }
            boolean offer = signalType == SignalType.ON_COMPLETE && channelsQueue.offer(channel);
            if (!offer) {
                SENDER_CHANNEL_CLOSE_HANDLER_INSTANCE.accept(signalType, channel);
            }
        };
    }

    @Override
    public void close() {
        List<Channel> channels = new ArrayList<>();
        channelsQueue.drainTo(channels);
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
