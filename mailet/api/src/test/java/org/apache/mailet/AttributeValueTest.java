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
package org.apache.mailet;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.TestMessageId;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import nl.jqno.equalsverifier.EqualsVerifier;

public class AttributeValueTest {
    @Test
    void shouldRespectBeanContract() {
        EqualsVerifier.forClass(AttributeValue.class).verify();
    }

    @Test
    void stringShouldBeSerializedAndBack() {
        AttributeValue<String> expected = AttributeValue.of("value");

        JsonNode json = expected.toJson();
        AttributeValue<?> actual = AttributeValue.fromJson(json);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void booleanShouldBeSerializedAndBack() {
        AttributeValue<Boolean> expected = AttributeValue.of(true);

        JsonNode json = expected.toJson();
        AttributeValue<?> actual = AttributeValue.fromJson(json);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void intShouldBeSerializedAndBack() {
        AttributeValue<Integer> expected = AttributeValue.of(42);

        JsonNode json = expected.toJson();
        AttributeValue<?> actual = AttributeValue.fromJson(json);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void longShouldBeSerializedAndBack() {
        AttributeValue<Long> expected = AttributeValue.of(42L);

        JsonNode json = expected.toJson();
        AttributeValue<?> actual = AttributeValue.fromJson(json);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void floatShouldBeSerializedAndBack() {
        AttributeValue<Float> expected = AttributeValue.of(1.0f);

        JsonNode json = expected.toJson();
        AttributeValue<?> actual = AttributeValue.fromJson(json);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void doubleShouldBeSerializedAndBack() {
        AttributeValue<Double> expected = AttributeValue.of(1.0);

        JsonNode json = expected.toJson();
        AttributeValue<?> actual = AttributeValue.fromJson(json);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void queueSerializableShouldBeSerializedAndBack() {
        AttributeValue<QueueSerializable> expected = AttributeValue.of(new TestQueueSerializable(42));

        JsonNode json = expected.toJson();
        AttributeValue<?> actual = AttributeValue.fromJson(json);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void urlShouldBeSerializedAndBack() throws MalformedURLException {
        AttributeValue<URL> expected = AttributeValue.of(new URL("https://james.apache.org/"));

        JsonNode json = expected.toJson();
        AttributeValue<?> actual = AttributeValue.fromJson(json);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void MessageIdShouldBeSerializedAndBack() {
        AttributeValue<MessageId> expected = AttributeValue.of(TestMessageId.of(42));

        JsonNode json = expected.toJson();
        AttributeValue<?> actual = AttributeValue.fromJson(json);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void emptyStringListShouldBeSerializedAndBack() {
        AttributeValue<?> expected = AttributeValue.of(ImmutableList.<String>of());

        JsonNode json = expected.toJson();
        AttributeValue<?> actual = AttributeValue.fromJson(json);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void listShouldBeSerializedAndBack() {
        AttributeValue<?> expected = AttributeValue.of(ImmutableList.of(AttributeValue.of("first"), AttributeValue.of("second")));

        JsonNode json = expected.toJson();
        AttributeValue<?> actual = AttributeValue.fromJson(json);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void emptyMapShouldBeSerializedAndBack() {
        AttributeValue<?> expected = AttributeValue.of(ImmutableMap.of());
        JsonNode json = expected.toJson();
        AttributeValue<?> actual = AttributeValue.fromJson(json);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void mapWithPrimitiveTypesShouldBeSerializedAndBack() {
        AttributeValue<?> expected = AttributeValue.of(ImmutableMap.of("a", AttributeValue.of("value"), "b", AttributeValue.of(12)));
        JsonNode json = expected.toJson();
        AttributeValue<?> actual = AttributeValue.fromJson(json);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void fromJsonStringShouldReturnStringAttributeValueWhenString() throws Exception {
        AttributeValue<String> expected = AttributeValue.of("value");

        AttributeValue<?> actual = AttributeValue.fromJsonString("{\"serializer\":\"StringSerializer\",\"value\": \"value\"}");

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void fromJsonStringShouldReturnIntAttributeValueWhenInt() throws Exception {
        AttributeValue<Integer> expected = AttributeValue.of(42);

        AttributeValue<?> actual = AttributeValue.fromJsonString("{\"serializer\":\"IntSerializer\",\"value\": 42}");

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void fromJsonStringShouldReturnEmptyListAttributeValueWhenEmptyArray() throws Exception {
        AttributeValue<?> expected = AttributeValue.of(ImmutableList.of());

        AttributeValue<?> actual = AttributeValue.fromJsonString("{\"serializer\":\"CollectionSerializer\",\"value\": []}");

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void fromJsonStringShouldReturnListAttributeValueWhenArray() throws Exception {
        AttributeValue<?> expected = AttributeValue.of(ImmutableList.of(AttributeValue.of("first"), AttributeValue.of("second")));

        AttributeValue<?> actual = AttributeValue.fromJsonString("{\"serializer\":\"CollectionSerializer\",\"value\":[{\"serializer\":\"StringSerializer\",\"value\":\"first\"},{\"serializer\":\"StringSerializer\",\"value\":\"second\"}]}");

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void fromJsonStringShouldReturnLongAttributeValueWhenLong() throws Exception {
        AttributeValue<Long> expected = AttributeValue.of(42L);

        AttributeValue<?> actual = AttributeValue.fromJsonString("{\"serializer\":\"LongSerializer\",\"value\":42}");

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void fromJsonStringShouldReturnFloatAttributeValueWhenFloat() throws Exception {
        AttributeValue<Float> expected = AttributeValue.of(1.0f);

        AttributeValue<?> actual = AttributeValue.fromJsonString("{\"serializer\":\"FloatSerializer\",\"value\":1.0}");

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void fromJsonStringShouldReturnDoubleAttributeValueWhenDouble() throws Exception {
        AttributeValue<Double> expected = AttributeValue.of(1.0);

        AttributeValue<?> actual = AttributeValue.fromJsonString("{\"serializer\":\"DoubleSerializer\",\"value\":1.0}");

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void fromJsonStringShouldReturnQueueSerializableAttributeValueWhenQueueSerializable() throws Exception {
        AttributeValue<QueueSerializable> expected = AttributeValue.of(new TestQueueSerializable(42));

        AttributeValue<?> actual = AttributeValue.fromJsonString("{\"serializer\":\"QueueSerializableSerializer\",\"value\":{\"factory\":\"org.apache.mailet.AttributeValueTest$TestQueueSerializable$Factory\",\"value\":{\"serializer\":\"IntSerializer\",\"value\":42}}}");

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void fromJsonStringShouldReturnUrlAttributeValueWhenUrl() throws Exception {
        AttributeValue<URL> expected = AttributeValue.of(new URL("https://james.apache.org/"));

        AttributeValue<?> actual = AttributeValue.fromJsonString("{\"serializer\":\"UrlSerializer\",\"value\": \"https://james.apache.org/\"}");

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void fromJsonStringShouldReturnMessageIdAttributeValueWhenUrl() throws Exception {
        AttributeValue<MessageId> expected = AttributeValue.of(TestMessageId.of(42));

        AttributeValue<?> actual = AttributeValue.fromJsonString("{\"serializer\":\"MessageIdSerializer\",\"value\":\"42\"}");

        assertThat(actual).isEqualTo(expected);
    }

    private static class TestQueueSerializable implements QueueSerializable {
        public static class Factory implements QueueSerializable.Factory {
            @Override
            public Optional<QueueSerializable> deserialize(Serializable serializable) {
                return Optional.of(serializable.getValue().value())
                        .filter(Integer.class::isInstance)
                        .map(Integer.class::cast)
                        .map(TestQueueSerializable::new);
            }
        }

        private final Integer value;

        public TestQueueSerializable(Integer value) {
            this.value = value;
        }

        @Override
        public Serializable serialize() {
            return new Serializable(AttributeValue.of(value), Factory.class);
        }

        @Override
        public final boolean equals(Object o) {
            if (o instanceof TestQueueSerializable) {
                TestQueueSerializable that = (TestQueueSerializable) o;

                return Objects.equals(this.value, that.value);
            }
            return false;
        }

        @Override
        public final int hashCode() {
            return Objects.hash(value);
        }
    }
}
