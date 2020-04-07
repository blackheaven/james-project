/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 * http://www.apache.org/licenses/LICENSE-2.0                   *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ***************************************************************/

package org.apache.james.backends.rabbitmq;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

public class RabbitMQServerVersion {
    private static final Pattern BEGINNING_DIGITS = Pattern.compile("^([\\d]+)");
    private static final Pattern POINT = Pattern.compile("\\.");
    private static final Pattern STOP = Pattern.compile("[\\-\\+~]");

    public static RabbitMQServerVersion of(String input) {
        Function<String, Integer> toInt = part -> {
            Matcher matcher = BEGINNING_DIGITS.matcher(part);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(0));
            } else {
                return 0;
            }
        };

        ImmutableList.Builder<Integer> versions = ImmutableList.builder();
        Iterable<String> parts = Splitter.on(POINT)
            .trimResults()
            .split(input);
        for (String part : parts) {
            versions.add(toInt.apply(part));

            if (STOP.matcher(part).find()) {
                break;
            }
        }

        return new RabbitMQServerVersion(versions.build());
    }

    @VisibleForTesting
    final ImmutableList<Integer> versions;

    private RabbitMQServerVersion(ImmutableList<Integer> versions) {
        this.versions = versions;
    }

    public boolean isAtLeast(RabbitMQServerVersion other) {
        Iterator<Integer> currentVersions = versions.iterator();
        Iterator<Integer> otherVersions = other.versions.iterator();

        while (currentVersions.hasNext() && otherVersions.hasNext()) {
            Integer otherVersion = otherVersions.next();
            Integer currentVersion = currentVersions.next();
            if (otherVersion < currentVersion) {
                return true;
            } else if (otherVersion > currentVersion) {
                return false;
            }
        }

        while (otherVersions.hasNext()) {
            if (otherVersions.next() != 0) {
                return false;
            }
        }

        return true;
    }

    public String asString() {
        return Joiner
            .on('.')
            .join(versions);
    }

    @Override
    public final boolean equals(Object other) {
        if (other instanceof RabbitMQServerVersion) {
            RabbitMQServerVersion that = (RabbitMQServerVersion) other;
            return Objects.equals(versions, that.versions);
        }

        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(versions);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("versions", versions)
            .toString();
    }
}
