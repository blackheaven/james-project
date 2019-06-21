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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

import org.apache.james.backend.rabbitmq.RabbitMQExtension;
import org.apache.james.util.concurrency.ConcurrentTestRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.fge.lambdas.Throwing;
import com.rabbitmq.client.Channel;

import reactor.core.publisher.Mono;
import reactor.rabbitmq.ChannelPool;
import reactor.rabbitmq.ChannelPoolOptions;

class LazyChannelPoolTest implements ChannelPoolContract {

    @RegisterExtension
    static RabbitMQExtension rabbitMQExtension = RabbitMQExtension.singletonRabbitMQ();

    private List<LazyChannelPool> channelPools;

    @BeforeEach
    void beforeEach() {
        channelPools = new ArrayList<>();
    }

    @AfterEach
    void afterEach() {
        channelPools.forEach(LazyChannelPool::close);
    }

    @Override
    public ChannelPool getChannelPool(int poolSize) {
        LazyChannelPool channelPool = new LazyChannelPool(
            rabbitMQExtension.getRabbitConnectionPool().getResilientConnection(),
            new ChannelPoolOptions().maxCacheSize(poolSize));
        channelPools.add(channelPool);

        return channelPool;
    }

    @Disabled("It doesn't force client to wait but create immediately a new channel, and number of open channels" +
        " exceeds channel pool size")
    @Test
    @Override
    public void channelPoolShouldWaitTillTheNextReleaseWhenAllChannelsAreTaken() {
    }

    @Disabled("It doesn't force client to wait but create immediately a new channel, and number of open channels" +
        " exceeds channel pool size")
    @Test
    @Override
    public void channelPoolShouldThrowAfterTimeoutWhenAllChannelsAreTaken() {
    }

    // Flux null mapping is unexpected exception
    @Test
    void concurrentRequestOnChannelMonoLeadToChannelsHungryExceptionInFlux() {
        Mono<? extends Channel> channelMono = getChannelPool(10).getChannelMono();

        ConcurrentLinkedQueue<Channel> listChannels = new ConcurrentLinkedQueue<>();
        assertThatThrownBy(() -> ConcurrentTestRunner.builder()
                .operation((threadNumber, step) -> listChannels.add(channelMono.block()))
                .threadCount(20)
                .operationCount(10000)
                .runSuccessfullyWithin(Duration.ofMinutes(1)))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("java.lang.NullPointerException: The mapper returned a null value");

        listChannels.forEach(Throwing.consumer(Channel::close));
    }
}
