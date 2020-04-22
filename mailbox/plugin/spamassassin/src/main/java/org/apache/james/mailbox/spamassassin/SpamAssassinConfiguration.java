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

import java.util.Objects;
import java.util.Optional;

import org.apache.james.util.Host;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

public class SpamAssassinConfiguration {

    private final Optional<Host> host;
    private final Optional<Integer> concurrency;

    public static SpamAssassinConfiguration disabled() {
        return new SpamAssassinConfiguration(Optional.empty(), Optional.empty());
    }

    public static SpamAssassinConfiguration enabled(Host host, Integer concurrency) {
        Preconditions.checkArgument(concurrency > 0, "Concurrency should be at least one");
        return new SpamAssassinConfiguration(Optional.of(host), Optional.of(concurrency));
    }

    private SpamAssassinConfiguration(Optional<Host> host, Optional<Integer> concurrency) {
        this.host = host;
        this.concurrency = concurrency;
    }

    public boolean isEnable() {
        return host.isPresent();
    }

    public Optional<Host> getHost() {
        return host;
    }

    public Optional<Integer> getConcurrency() {
        return concurrency;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof SpamAssassinConfiguration) {
            SpamAssassinConfiguration that = (SpamAssassinConfiguration) o;

            return Objects.equals(this.host, that.host)
                && Objects.equals(this.concurrency, that.concurrency);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(host, concurrency);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("host", host)
                .toString();
    }
}
