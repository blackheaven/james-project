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

package org.apache.james.blob.api;

import java.io.InputStream;
import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Preconditions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

public interface Store<T, I> {

    Mono<I> save(T t);

    Mono<T> read(I blobIds);

    class BlobType {
        private final String name;

        public BlobType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public final boolean equals(Object o) {
            if (o instanceof BlobType) {
                BlobType blobType = (BlobType) o;

                return Objects.equals(this.name, blobType.name);
            }
            return false;
        }

        @Override
        public final int hashCode() {
            return Objects.hash(name);
        }
    }

    class Impl<T, I extends BlobPartsId> implements Store<T, I> {
        private static final int READ_RETRIES_NUMBER = 5;
        private static final Duration READ_RETRIES_BACKOFF = Duration.ofMillis(100);

        public interface Encoder<T> {
            Stream<Pair<BlobType, FixedLengthInputStream>> encode(T t);
        }

        public interface Decoder<T> {
            T decode(Stream<Pair<BlobType, byte[]>> streams);
        }

        private final BlobPartsId.Factory<I> idFactory;
        private final Encoder<T> encoder;
        private final Decoder<T> decoder;
        private final BlobStore blobStore;

        public Impl(BlobPartsId.Factory<I> idFactory, Encoder<T> encoder, Decoder<T> decoder, BlobStore blobStore) {
            this.idFactory = idFactory;
            this.encoder = encoder;
            this.decoder = decoder;
            this.blobStore = blobStore;
        }

        @Override
        public Mono<I> save(T t) {
            return Flux.fromStream(encoder.encode(t))
                .flatMapSequential(this::saveEntry)
                .collectMap(Tuple2::getT1, Tuple2::getT2)
                .map(idFactory::generate);
        }

        private Mono<Tuple2<BlobType, BlobId>> saveEntry(Pair<BlobType, FixedLengthInputStream> entry) {
            return Mono.just(entry.getLeft())
                .zipWith(blobStore.save(entry.getRight().getInputStream(), entry.getRight().getContentLength()));
        }

        @Override
        public Mono<T> read(I blobIds) {
            return Flux.fromIterable(blobIds.asMap().entrySet())
                .flatMapSequential(
                    entry -> readBytes(blobStore, entry.getValue())
                        .zipWith(Mono.just(entry.getKey())))
                .map(entry -> Pair.of(entry.getT2(), entry.getT1()))
                .collectList()
                .map(Collection::stream)
                .map(decoder::decode);
        }

        private Mono<byte[]> readBytes(BlobStore blobStore, BlobId blobId) {
            return blobStore.readBytes(blobId)
                .retryBackoff(READ_RETRIES_NUMBER, READ_RETRIES_BACKOFF);
        }
    }

    class FixedLengthInputStream {

        private final InputStream inputStream;
        private final long contentLength;

        public FixedLengthInputStream(InputStream inputStream, long contentLength) {
            Preconditions.checkNotNull(inputStream, "'inputStream' is mandatory");
            Preconditions.checkArgument(contentLength >= 0, "'contentLength' should be greater than or equal to 0");
            this.inputStream = inputStream;
            this.contentLength = contentLength;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public long getContentLength() {
            return contentLength;
        }
    }
}
