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

package org.apache.james.rrt.api;

import java.util.Objects;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

public class RecipientRewriteTableConfiguration {

    // The maximum mappings which will process before throwing exception
    private final int mappingLimit;

    private final boolean recursive;

    @VisibleForTesting
    public RecipientRewriteTableConfiguration(boolean recursive, int mappingLimit) {
        Preconditions.checkArgument(mappingLimit == 0 || recursive, "mappingLimit can not be different that 0 when recursive mode is disabled");
        this.recursive = recursive;
        this.mappingLimit = mappingLimit;
    }

    public static RecipientRewriteTableConfiguration fromConfiguration(HierarchicalConfiguration<ImmutableNode> config) throws ConfigurationException {
        boolean recursive = config.getBoolean("recursiveMapping", true);
        int mappingLimit;
        if (recursive) {
            mappingLimit = config.getInt("mappingLimit", 10);
            checkMappingLimit(mappingLimit);
        } else {
            mappingLimit = 0;
        }
        return new RecipientRewriteTableConfiguration(recursive, mappingLimit);
    }

    private static void checkMappingLimit(int mappingLimit) throws ConfigurationException {
        if (mappingLimit < 1) {
            throw new ConfigurationException("The minimum mappingLimit is 1");
        }
    }

    public int getMappingLimit() {
        return mappingLimit;
    }

    public boolean isRecursive() {
        return recursive;
    }

    @Override
    public final boolean equals(Object other) {
        if (other instanceof RecipientRewriteTableConfiguration) {
            RecipientRewriteTableConfiguration that = (RecipientRewriteTableConfiguration) other;
            return Objects.equals(mappingLimit, that.mappingLimit) && Objects.equals(recursive, that.recursive);
        }

        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(mappingLimit, recursive);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("mappingLimit", mappingLimit)
            .add("recursive", recursive)
            .toString();
    }
}
