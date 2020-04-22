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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Optional;

import org.apache.james.util.Host;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

class SpamAssassinConfigurationTest {

    @Test
    void spamAssassinConfigurationShouldRespectBeanContract() {
        EqualsVerifier.forClass(SpamAssassinConfiguration.class)
            .verify();
    }

    @Test
    void isEnableShouldReturnFalseWhenEmpty() {
        SpamAssassinConfiguration configuration = SpamAssassinConfiguration.disabled();
        assertThat(configuration.isEnable()).isFalse();
    }

    @Test
    void isEnableShouldReturnTrueWhenConfigured() {
        int port = 1;
        int parallelism = 4;
        SpamAssassinConfiguration configuration = SpamAssassinConfiguration.enabled(Host.from("hostname", port), parallelism);
        assertThat(configuration.isEnable()).isTrue();
    }

    @Test
    void givingZeroAsParallelismShouldThrow() {
        int port = 1;
        int parallelism = 0;
        assertThatCode(() -> SpamAssassinConfiguration.enabled(Host.from("hostname", port), parallelism))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givingMinusOneAsParallelismShouldThrow() {
        int port = 1;
        int parallelism = -1;
        assertThatCode(() -> SpamAssassinConfiguration.enabled(Host.from("hostname", port), parallelism))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
