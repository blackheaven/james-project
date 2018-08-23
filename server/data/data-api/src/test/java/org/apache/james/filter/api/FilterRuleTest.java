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

package org.apache.james.filter.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class FilterRuleTest {

    private static final List<String> ACTION_MAILBOXIDS = Arrays.asList("id-01");
    private static final String CONDITION_COMPARATOR = "contains";
    private static final String CONDITION_FIELD = "cc";
    private static final String NAME = "a name";
    public static final FilterRule.Condition CONDITION = FilterRule.Condition.of(CONDITION_FIELD, CONDITION_COMPARATOR, "something");
    public static final FilterRule.Action ACTION = FilterRule.Action.ofMailboxIds(ACTION_MAILBOXIDS);
    public static final FilterRule.Id UNIQUE_ID = FilterRule.Id.of("uniqueId");

    @Test
    void shouldMatchBeanContract() {
        EqualsVerifier.forClass(FilterRule.class)
            .verify();
    }

    @Test
    void innerClassConditionShouldMatchBeanContract() {
        EqualsVerifier.forClass(FilterRule.Condition.class)
            .verify();
    }

    @Test
    void innerClassActionShouldMatchBeanContract() {
        EqualsVerifier.forClass(FilterRule.Action.class)
            .verify();
    }

    @Test
    void innerClassIdShouldMatchBeanContract() {
        EqualsVerifier.forClass(FilterRule.Id.class)
            .verify();
    }

    @Test
    void idShouldThrowOnNull() {
        assertThatThrownBy(() -> FilterRule.Id.of(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void idShouldThrowOnEmpty() {
        assertThatThrownBy(() -> FilterRule.Id.of("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void idShouldThrowOnBlank() {
        assertThatThrownBy(() -> FilterRule.Id.of("   ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void idShouldBeMandatory() {
        assertThatThrownBy(() ->
            FilterRule.builder()
                .name(NAME)
                .condition(CONDITION)
                .action(ACTION)
                .build())
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void nameShouldBeMandatory() {
        assertThatThrownBy(() ->
            FilterRule.builder()
                .id(UNIQUE_ID)
                .condition(CONDITION)
                .action(ACTION)
                .build())
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void conditionShouldBeMandatory() {
        assertThatThrownBy(() ->
            FilterRule.builder()
                .id(UNIQUE_ID)
                .name(NAME)
                .action(ACTION)
                .build())
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void actionShouldBeMandatory() {
        assertThatThrownBy(() ->
            FilterRule.builder()
                .id(UNIQUE_ID)
                .name(NAME)
                .condition(CONDITION)
                .build())
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void builderShouldPreserveCondition() {
        FilterRule rule = FilterRule.builder()
            .id(UNIQUE_ID)
            .name(NAME)
            .condition(CONDITION)
            .action(ACTION)
            .build();

        assertThat(rule.getCondition()).isEqualTo(CONDITION);
    }

    @Test
    void builderShouldPreserveAction() {
        FilterRule rule = FilterRule.builder()
            .id(UNIQUE_ID)
            .name(NAME)
            .condition(CONDITION)
            .action(ACTION)
            .build();

        assertThat(rule.getAction()).isEqualTo(ACTION);
    }

    @Test
    void buildConditionShouldConserveField() {
        assertThat(CONDITION.getField().asString()).isEqualTo(CONDITION_FIELD);
    }

    @Test
    void buildConditionShouldConserveComparator() {
        assertThat(CONDITION.getComparator().asString()).isEqualTo(CONDITION_COMPARATOR);
    }

    @Test
    void buildActionShouldConserveMailboxIdsList() {
        assertThat(ACTION.getMailboxIds()).isEqualTo(ACTION_MAILBOXIDS);
    }
}
