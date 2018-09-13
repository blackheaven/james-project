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

package org.apache.james.queue.rabbitmq.view.cassandra;

import static org.apache.james.queue.rabbitmq.view.cassandra.model.BucketedSlices.BucketId;
import static org.apache.james.queue.rabbitmq.view.cassandra.model.BucketedSlices.Slice;
import static org.apache.james.queue.rabbitmq.view.cassandra.model.BucketedSlices.Slice.allSlicesTill;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.james.queue.api.ManageableMailQueue;
import org.apache.james.queue.rabbitmq.MailQueueName;
import org.apache.james.queue.rabbitmq.view.cassandra.model.EnqueuedMail;
import org.apache.james.util.FluentFutureStream;

class BrowseHelper {

    private final BrowseStartDAO browseStartDao;
    private final DeletedMailsDAO deletedMailsDao;
    private final EnqueuedMailsDAO enqueuedMailsDao;
    private final CassandraMailQueueViewConfiguration configuration;
    private final Clock clock;

    @Inject
    BrowseHelper(BrowseStartDAO browseStartDao,
                 DeletedMailsDAO deletedMailsDao,
                 EnqueuedMailsDAO enqueuedMailsDao,
                 CassandraMailQueueViewConfiguration configuration, Clock clock) {
        this.browseStartDao = browseStartDao;
        this.deletedMailsDao = deletedMailsDao;
        this.enqueuedMailsDao = enqueuedMailsDao;
        this.configuration = configuration;
        this.clock = clock;
    }

    CompletableFuture<Stream<ManageableMailQueue.MailQueueItemView>> browse(MailQueueName queueName) {
        return browseReferences(queueName)
            .map(EnqueuedMail::getMail)
            .map(ManageableMailQueue.MailQueueItemView::new)
            .completableFuture();
    }

    FluentFutureStream<EnqueuedMail> browseReferences(MailQueueName queueName) {
        return FluentFutureStream.of(browseStartDao.findBrowseStart(queueName)
            .thenApply(this::calculateAllSlicesFromBrowseStart))
            .thenFlatMap(currentSlice -> browseOnlyEnqueuedInOrder(queueName, currentSlice));
    }

    private FluentFutureStream<EnqueuedMail> browseOnlyEnqueuedInOrder(MailQueueName queueName, Slice currentSlice) {
        return FluentFutureStream.ofFluentFutureStreams(
            allBucketIds()
                .map(bucketId -> browseOnlyEnqueuedForBucket(queueName, currentSlice, bucketId)))
            .sorted(Comparator.comparing(EnqueuedMail::getEnqueuedTime));
    }

    private FluentFutureStream<EnqueuedMail> browseOnlyEnqueuedForBucket(MailQueueName queueName, Slice currentSlice, BucketId bucketId) {
        return FluentFutureStream.of(
            enqueuedMailsDao.selectEnqueuedMails(queueName, currentSlice, bucketId))
                .thenFilter(mailReference -> deletedMailsDao.isStillEnqueued(queueName, mailReference.getMailKey()));
    }

    private Stream<Slice> calculateAllSlicesFromBrowseStart(Optional<Instant> maybeBrowseStartInstant) {
        return maybeBrowseStartInstant
            .map(browseStartInstant -> Slice.of(browseStartInstant, configuration.getSliceWindow()))
            .map(startSlice -> allSlicesTill(startSlice, clock.instant()))
            .orElse(Stream.empty());
    }

    private Stream<BucketId> allBucketIds() {
        return IntStream
            .range(0, configuration.getBucketCount())
            .mapToObj(BucketId::of);
    }
}
