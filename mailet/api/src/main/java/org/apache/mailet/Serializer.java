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

import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.james.util.streams.Iterators;
import org.nustaq.serialization.FSTConfiguration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/** 
 * Serializer
 * 
 * @since Mailet API v3.2
 */
public interface Serializer<T> {
    JsonNode serialize(T object);

    Optional<T> deserialize(JsonNode json);

    String getName();
    
    class Registry {

        private static ImmutableMap<String, Serializer<?>> serializers;

        static {
            serializers = Stream.<Serializer<?>>of(
                    BOOLEAN_SERIALIZER,
                    STRING_SERIALIZER,
                    INT_SERIALIZER,
                    URL_SERIALIZER,
                    new CollectionSerializer<>(),
                    new MapSerializer<>(),
                    new FSTSerializer())
                    .collect(ImmutableMap.toImmutableMap(Serializer::getName, Function.identity()));
        }
        
        static Optional<Serializer<?>> find(String name) {
            return Optional.ofNullable(serializers.get(name));
        }
    }

    class BooleanSerializer implements Serializer<Boolean> {
        @Override
        public JsonNode serialize(Boolean object) {
            return BooleanNode.valueOf(object);
        }

        @Override
        public Optional<Boolean> deserialize(JsonNode json) {
            if (json instanceof BooleanNode) {
                return Optional.of(json.asBoolean());
            } else {
                return Optional.empty();
            }
        }

        @Override
        public String getName() {
            return "BooleanSerializer";
        }

        @Override
        public boolean equals(Object other) {
            return this.getClass() == other.getClass();
        }
    }

    Serializer<Boolean> BOOLEAN_SERIALIZER = new BooleanSerializer();

    class StringSerializer implements Serializer<String> {
        @Override
        public JsonNode serialize(String object) {
            return TextNode.valueOf(object);
        }

        @Override
        public Optional<String> deserialize(JsonNode json) {
            if (json instanceof TextNode) {
                return Optional.of(json.asText());
            } else {
                return Optional.empty();
            }
        }

        @Override
        public String getName() {
            return "StringSerializer";
        }

        @Override
        public boolean equals(Object other) {
            return this.getClass() == other.getClass();
        }
    }

    Serializer<String> STRING_SERIALIZER = new StringSerializer();

    class IntSerializer implements Serializer<Integer> {
        @Override
        public JsonNode serialize(Integer object) {
            return IntNode.valueOf(object);
        }

        @Override
        public Optional<Integer> deserialize(JsonNode json) {
            if (json instanceof IntNode) {
                return Optional.of(json.asInt());
            } else {
                return Optional.empty();
            }
        }

        @Override
        public String getName() {
            return "IntSerializer";
        }

        @Override
        public boolean equals(Object other) {
            return this.getClass() == other.getClass();
        }
    }

    Serializer<Integer> INT_SERIALIZER = new IntSerializer();

    class UrlSerializer implements Serializer<URL> {
        @Override
        public JsonNode serialize(URL object) {
            return STRING_SERIALIZER.serialize(object.toString());
        }

        @Override
        public Optional<URL> deserialize(JsonNode json) {
            return STRING_SERIALIZER.deserialize(json).flatMap(url -> {
                try {
                    return Optional.of(new URL(url));
                } catch (MalformedURLException e) {
                    return Optional.empty();
                }
            });
        }

        @Override
        public String getName() {
            return "UrlSerializer";
        }

        @Override
        public boolean equals(Object other) {
            return this.getClass() == other.getClass();
        }
    }

    Serializer<URL> URL_SERIALIZER = new UrlSerializer();

    class CollectionSerializer<U> implements Serializer<Collection<AttributeValue<U>>> {
        @Override
        public JsonNode serialize(Collection<AttributeValue<U>> object) {
            List<JsonNode> jsons = object.stream()
                .map(AttributeValue::toJson)
                .collect(ImmutableList.toImmutableList());
            return new ArrayNode(JsonNodeFactory.instance, jsons);
        }

        @Override
        public Optional<Collection<AttributeValue<U>>> deserialize(JsonNode json) {
            if (json instanceof ArrayNode) {
                return Optional.of(Iterators.toStream(json.elements())
                        .map(value -> (AttributeValue<U>) AttributeValue.fromJson(value))
                        .collect(ImmutableList.toImmutableList()));
            } else {
                return Optional.empty();
            }
        }

        @Override
        public String getName() {
            return "CollectionSerializer";
        }

        @Override
        public boolean equals(Object other) {
            return this.getClass() == other.getClass();
        }
    }

    class MapSerializer<U> implements Serializer<Map<String, AttributeValue<U>>> {
        @Override
        public JsonNode serialize(Map<String, AttributeValue<U>> object) {
            Map<String, JsonNode> jsonMap = object.entrySet().stream()
                .collect(ImmutableMap.toImmutableMap(Entry::getKey, entry -> entry.getValue().toJson()));
            return new ObjectNode(JsonNodeFactory.instance, jsonMap);
        }

        @Override
        public Optional<Map<String, AttributeValue<U>>> deserialize(JsonNode json) {
            if (json instanceof ArrayNode) {
                return Optional.of(Iterators.toStream(json.fields())
                        .collect(ImmutableMap.toImmutableMap(
                            Map.Entry::getKey,
                            entry -> (AttributeValue<U>) AttributeValue.fromJson(entry.getValue())
                        )));
            } else {
                return Optional.empty();
            }
        }

        @Override
        public String getName() {
            return "MapSerializer";
        }

        @Override
        public boolean equals(Object other) {
            return this.getClass() == other.getClass();
        }
    }

    class FSTSerializer implements Serializer<Serializable> {
        @Override
        public JsonNode serialize(Serializable object) {
            FSTConfiguration conf = FSTConfiguration.createJsonConfiguration();
            String json = conf.asJsonString(object);
            try {
                return new ObjectMapper().reader().readTree(json);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public Optional<Serializable> deserialize(JsonNode json) {
            FSTConfiguration conf = FSTConfiguration.createJsonConfiguration();
            try {
                return Optional.of((Serializable) conf.asObject(new ObjectMapper().writer().writeValueAsBytes(json)));
            } catch (JsonProcessingException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public String getName() {
            return "FSTSerializer";
        }

        @Override
        public boolean equals(Object other) {
            return this.getClass() == other.getClass();
        }
    }

}
