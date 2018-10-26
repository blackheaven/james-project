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

package org.apache.james.mailbox.cassandra.ids;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.james.mailbox.model.MessageId;
import org.apache.mailet.AttributeValue;
import org.apache.mailet.QueueSerializable;

import com.datastax.driver.core.utils.UUIDs;
import com.google.common.base.MoreObjects;

public class CassandraMessageId implements MessageId {

    public static class Factory implements MessageId.Factory {

        @Override
        public CassandraMessageId generate() {
            return of(UUIDs.timeBased());
        }

        public CassandraMessageId of(UUID uuid) {
            return new CassandraMessageId(uuid);
        }

        @Override
        public Optional<QueueSerializable> deserialize(Serializable serializable) {
            return Optional.of(serializable.getValue().getValue())
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(uuid -> of(UUID.fromString(uuid)));
        }
    }

    private final UUID uuid;

    private CassandraMessageId(UUID uuid) {
        this.uuid = uuid;
    }
    
    @Override
    public String asString() {
        return uuid.toString();
    }

    public UUID get() {
        return uuid;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof CassandraMessageId) {
            CassandraMessageId other = (CassandraMessageId) o;
            return Objects.equals(uuid, other.uuid);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(uuid);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("uuid", uuid)
            .toString();
    }

    @Override
    public Serializable serialize() {
        return new Serializable(AttributeValue.of(asString()), Factory.class);
    }
}
