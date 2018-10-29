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
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.TestMessageId;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import nl.jqno.equalsverifier.EqualsVerifier;

class AttributeValueTest {
    @Test
    void shouldRespectBeanContract() {
        EqualsVerifier.forClass(AttributeValue.class).verify();
    }

    @Test
    void ofShouldThrowAnExceptionOnNullValue() {
        assertThatNullPointerException().
                isThrownBy(() -> AttributeValue.of((String) null));
    }

    @Nested
    class StringSerialization {
        @Test
        void stringShouldBeSerializedAndBack() {
            AttributeValue<String> expected = AttributeValue.of("value");

            JsonNode json = expected.toJson();
            AttributeValue<?> actual = AttributeValue.fromJson(json);

            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void emptyStringShouldBeSerializedAndBack() {
            AttributeValue<String> expected = AttributeValue.of("");

            JsonNode json = expected.toJson();
            AttributeValue<?> actual = AttributeValue.fromJson(json);

            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void nullStringShouldThrowAnException() {
            assertThatNullPointerException().
                isThrownBy(() -> AttributeValue.of((String) null));
        }

        @Test
        void fromJsonStringShouldReturnStringAttributeValueWhenString() throws Exception {
            AttributeValue<String> expected = AttributeValue.of("value");

            AttributeValue<?> actual = AttributeValue.fromJsonString("{\"serializer\":\"StringSerializer\",\"value\": \"value\"}");

            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void fromJsonStringShouldThrowOnMalformedFormattedJson() {
            assertThatIllegalStateException()
                .isThrownBy(() -> AttributeValue.fromJsonString("{\"serializer\":\"StringSerializer\",\"value\": []}"));
        }
    }

    @Nested
    class BooleanSerialization {
        @Test
        void trueShouldBeSerializedAndBack() {
            AttributeValue<Boolean> expected = AttributeValue.of(true);

            JsonNode json = expected.toJson();
            AttributeValue<?> actual = AttributeValue.fromJson(json);

            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void falseShouldBeSerializedAndBack() {
            AttributeValue<Boolean> expected = AttributeValue.of(true);

            JsonNode json = expected.toJson();
            AttributeValue<?> actual = AttributeValue.fromJson(json);

            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void nullBooleanShouldThrowAnException() {
            assertThatNullPointerException().
                isThrownBy(() -> AttributeValue.of((Boolean) null));
        }

        @Test
        void fromJsonStringShouldReturnBooleanAttributeValueWhenBoolean() throws Exception {
            AttributeValue<Boolean> expected = AttributeValue.of(true);

            AttributeValue<?> actual = AttributeValue.fromJsonString("{\"serializer\":\"BooleanSerializer\",\"value\": true}");

            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void fromJsonStringShouldThrowOnMalformedFormattedJson() {
            assertThatIllegalStateException()
                .isThrownBy(() -> AttributeValue.fromJsonString("{\"serializer\":\"BooleanSerializer\",\"value\": []}"));
        }
    }

    @Nested
    class IntegerSerialization {
        @Test
        void intShouldBeSerializedAndBack() {
            AttributeValue<Integer> expected = AttributeValue.of(42);

            JsonNode json = expected.toJson();
            AttributeValue<?> actual = AttributeValue.fromJson(json);

            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void nullIntShouldThrowAnException() {
            assertThatNullPointerException().
                isThrownBy(() -> AttributeValue.of((Integer) null));
        }

        @Test
        void fromJsonStringShouldReturnIntAttributeValueWhenInt() throws Exception {
            AttributeValue<Integer> expected = AttributeValue.of(42);

            AttributeValue<?> actual = AttributeValue.fromJsonString("{\"serializer\":\"IntSerializer\",\"value\": 42}");

            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void fromJsonStringShouldThrowOnMalformedFormattedJson() {
            assertThatIllegalStateException()
                .isThrownBy(() -> AttributeValue.fromJsonString("{\"serializer\":\"IntSerializer\",\"value\": []}"));
        }
    }

    @Nested
    class LongSerialization {
        @Test
        void longShouldBeSerializedAndBack() {
            AttributeValue<Long> expected = AttributeValue.of(42L);

            JsonNode json = expected.toJson();
            AttributeValue<?> actual = AttributeValue.fromJson(json);

            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void nullLongShouldThrowAnException() {
            assertThatNullPointerException().
                isThrownBy(() -> AttributeValue.of((Long) null));
        }

        @Test
        void fromJsonStringShouldReturnLongAttributeValueWhenLong() throws Exception {
            AttributeValue<Long> expected = AttributeValue.of(42L);

            AttributeValue<?> actual = AttributeValue.fromJsonString("{\"serializer\":\"LongSerializer\",\"value\":42}");

            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void fromJsonStringShouldThrowOnMalformedFormattedJson() {
            assertThatIllegalStateException()
                .isThrownBy(() -> AttributeValue.fromJsonString("{\"serializer\":\"LongSerializer\",\"value\": []}"));
        }
    }

    @Nested
    class FloatSerialization {
        @Test
        void floatShouldBeSerializedAndBack() {
            AttributeValue<Float> expected = AttributeValue.of(1.0f);

            JsonNode json = expected.toJson();
            AttributeValue<?> actual = AttributeValue.fromJson(json);

            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void nullFloatShouldThrowAnException() {
            assertThatNullPointerException().
                isThrownBy(() -> AttributeValue.of((Float) null));
        }

        @Test
        void fromJsonStringShouldReturnFloatAttributeValueWhenFloat() throws Exception {
            AttributeValue<Float> expected = AttributeValue.of(1.0f);

            AttributeValue<?> actual = AttributeValue.fromJsonString("{\"serializer\":\"FloatSerializer\",\"value\":1.0}");

            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void fromJsonStringShouldThrowOnMalformedFormattedJson() {
            assertThatIllegalStateException()
                .isThrownBy(() -> AttributeValue.fromJsonString("{\"serializer\":\"FloatSerializer\",\"value\": []}"));
        }
    }

    @Nested
    class DoubleSerialization {
        @Test
        void doubleShouldBeSerializedAndBack() {
            AttributeValue<Double> expected = AttributeValue.of(1.0d);

            JsonNode json = expected.toJson();
            AttributeValue<?> actual = AttributeValue.fromJson(json);

            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void nullDoubleShouldThrowAnException() {
            assertThatNullPointerException().
                isThrownBy(() -> AttributeValue.of((Double) null));
        }

        @Test
        void fromJsonStringShouldReturnDoubleAttributeValueWhenDouble() throws Exception {
            AttributeValue<Double> expected = AttributeValue.of(1.0);

            AttributeValue<?> actual = AttributeValue.fromJsonString("{\"serializer\":\"DoubleSerializer\",\"value\":1.0}");

            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void fromJsonStringShouldThrowOnMalformedFormattedJson() {
            assertThatIllegalStateException()
                .isThrownBy(() -> AttributeValue.fromJsonString("{\"serializer\":\"DoubleSerializer\",\"value\": []}"));
        }
    }

    @Nested
    class QueueSerializableTest {
        @Test
        void queueSerializableShouldBeSerializedAndBack() {
            AttributeValue<TestArbitrarySerializable> expected = AttributeValue.of(new TestArbitrarySerializable(42));

            JsonNode json = expected.toJson();
            AttributeValue<?> actual = AttributeValue.fromJson(json);

            assertThat(actual).isEqualTo(expected);
        }


        @Test
        void fromJsonStringShouldReturnQueueSerializableAttributeValueWhenQueueSerializable() throws Exception {
            AttributeValue<TestArbitrarySerializable> expected = AttributeValue.of(new TestArbitrarySerializable(42));

            AttributeValue<?> actual = AttributeValue.fromJsonString("{\"serializer\":\"ArbitrarySerializableSerializer\",\"value\":{\"factory\":\"org.apache.mailet.AttributeValueTest$TestArbitrarySerializable$Factory\",\"value\":{\"serializer\":\"IntSerializer\",\"value\":42}}}");

            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    class UrlSerialization {
        @Test
        void urlShouldBeSerializedAndBack() throws MalformedURLException {
            AttributeValue<URL> expected = AttributeValue.of(new URL("https://james.apache.org/"));

            JsonNode json = expected.toJson();
            AttributeValue<?> actual = AttributeValue.fromJson(json);

            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void nullURLShouldThrowAnException() {
            assertThatNullPointerException().
                isThrownBy(() -> AttributeValue.of((URL) null));
        }

        @Test
        void fromJsonStringShouldReturnUrlAttributeValueWhenUrl() throws Exception {
            AttributeValue<URL> expected = AttributeValue.of(new URL("https://james.apache.org/"));

            AttributeValue<?> actual = AttributeValue.fromJsonString("{\"serializer\":\"UrlSerializer\",\"value\": \"https://james.apache.org/\"}");

            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void fromJsonStringShouldThrowOnMalformedUrl() {
            assertThatIllegalStateException()
                .isThrownBy(() -> AttributeValue.fromJsonString("{\"serializer\":\"UrlSerializer\",\"value\": \"htps://bad/\"}"));
        }

        @Test
        void fromJsonStringShouldThrowOnMalformedFormattedJson() {
            assertThatIllegalStateException()
                .isThrownBy(() -> AttributeValue.fromJsonString("{\"serializer\":\"UrlSerializer\",\"value\": {}}"));
        }
    }

    @Nested
    class ListSerialization {
        @Test
        void nullStringListShouldThrowAnException() {
            assertThatNullPointerException().
                isThrownBy(() -> AttributeValue.ofAny((List<String>) null));
        }

        @Test
        void emptyStringListShouldBeSerializedAndBack() {
            AttributeValue<?> expected = AttributeValue.ofAny(ImmutableList.<String>of());

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
        void fromJsonStringShouldThrowOnMalformedFormattedJson() {
            assertThatIllegalStateException()
                .isThrownBy(() -> AttributeValue.fromJsonString("{\"serializer\":\"CollectionSerializer\",\"value\": {}}"));
        }
    }

    @Nested
    class MapSerialization {
        @Test
        void nullMapShouldThrowAnException() {
            assertThatNullPointerException().
                isThrownBy(() -> AttributeValue.of((Map) null));
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
        void fromJsonStringShouldReturnEmptyMapWhenEmptyMap() throws Exception {
            AttributeValue<Map<String, AttributeValue<?>>> expected = AttributeValue.of(ImmutableMap.of());

            AttributeValue<?> actual = AttributeValue.fromJsonString("{\"serializer\":\"MapSerializer\",\"value\":{}}");

            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void fromJsonStringShouldReturnMapWhenMap() throws Exception {
            AttributeValue<Map<String, AttributeValue<?>>> expected = AttributeValue.of(
                ImmutableMap.of(
                    "a", AttributeValue.of(1),
                    "b", AttributeValue.of(2)));

            AttributeValue<?> actual = AttributeValue.fromJsonString("{\"serializer\":\"MapSerializer\",\"value\":{\"a\":{\"serializer\":\"IntSerializer\",\"value\":1},\"b\":{\"serializer\":\"IntSerializer\",\"value\":2}}}");

            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void fromJsonStringShouldThrowOnMalformedFormattedJson() {
            assertThatIllegalStateException()
                .isThrownBy(() -> AttributeValue.fromJsonString("{\"serializer\":\"MapSerializer\",\"value\": []}"));
        }
    }

    @Test
    void fromJsonStringShouldThrowOnUnknownSerializer() {
        assertThatIllegalStateException()
            .isThrownBy(() -> AttributeValue.fromJsonString("{\"serializer\":\"unknown\",\"value\": \"value\"}"));
    }

    @Test
    void fromJsonStringShouldThrowOnBrokenJson() {
        assertThatThrownBy(() ->AttributeValue.fromJsonString("{\"serializer\":\"StringSerializer\",\"value\": \"Missing closing bracket\""))
            .isInstanceOf(JsonEOFException.class);
    }

    @Test
    void fromJsonStringShouldThrowOnMissingSerializerField() {
        assertThatIllegalStateException()
            .isThrownBy(() -> AttributeValue.fromJsonString("{\"value\": \"value\"}"));
    }

    @Test
    void fromJsonStringShouldThrowOnMissingValueField() {
        assertThatIllegalStateException()
            .isThrownBy(() -> AttributeValue.fromJsonString("{\"serializer\":\"MapSerializer\"}"));
    }

    @Test
    void fromJsonStringShouldReturnMessageIdAttributeValue() throws Exception {
        AttributeValue<MessageId> expected = AttributeValue.of(TestMessageId.of(42));

        AttributeValue<?> actual = AttributeValue.fromJsonString("{\"serializer\":\"MessageIdSerializer\",\"value\":\"42\"}");

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void messageIdShouldBeSerializedAndBack() {
        AttributeValue<MessageId> expected = AttributeValue.of(TestMessageId.of(42));

        JsonNode json = expected.toJson();
        AttributeValue<?> actual = AttributeValue.fromJson(json);

        assertThat(actual).isEqualTo(expected);
    }

    private static class TestArbitrarySerializable implements ArbitrarySerializable<TestArbitrarySerializable> {
        public static class Factory implements ArbitrarySerializable.Factory<TestArbitrarySerializable> {
            @Override
            public Optional<TestArbitrarySerializable> deserialize(Serializable<TestArbitrarySerializable> serializable) {
                return Optional.of(serializable.getValue().value())
                        .filter(Integer.class::isInstance)
                        .map(Integer.class::cast)
                        .map(TestArbitrarySerializable::new);
            }
        }

        private final Integer value;

        public TestArbitrarySerializable(Integer value) {
            this.value = value;
        }

        @Override
        public Serializable<TestArbitrarySerializable> serialize() {
            return new Serializable<>(AttributeValue.of(value), Factory.class);
        }

        @Override
        public final boolean equals(Object o) {
            if (o instanceof TestArbitrarySerializable) {
                TestArbitrarySerializable that = (TestArbitrarySerializable) o;

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
