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
package org.apache.james.transport.mailets;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.mailet.Attribute;
import org.apache.mailet.AttributeName;
import org.apache.mailet.AttributeUtils;
import org.apache.mailet.AttributeValue;
import org.apache.mailet.BytesArrayDto;
import org.apache.mailet.Mail;
import org.apache.mailet.MailetException;
import org.apache.mailet.base.GenericMailet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.Throwing;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

/**
 * This mailet replace the mail attribute map of key to MimePart
 * by a map of key to the MimePart content (as bytes).
 * <br />
 * It takes only one parameter:
 * <ul>
 * <li>attribute (mandatory): mime content to be decoded, expected to be a Map&lt;String, byte[]&gt;
 * </ul>
 *
 * Then all this map attribute values will be replaced by their content.
 */
public class MimeDecodingMailet extends GenericMailet {
    private static final Logger LOGGER = LoggerFactory.getLogger(MimeDecodingMailet.class);

    public static final String ATTRIBUTE_PARAMETER_NAME = "attribute";

    private AttributeName attribute;

    @Override
    public void init() throws MessagingException {
        String attributeRaw = getInitParameter(ATTRIBUTE_PARAMETER_NAME);
        if (Strings.isNullOrEmpty(attributeRaw)) {
            throw new MailetException("No value for " + ATTRIBUTE_PARAMETER_NAME
                    + " parameter was provided.");
        }
        attribute = AttributeName.of(attributeRaw);
    }

    @Override
    public void service(Mail mail) throws MessagingException {
        if (!mail.getAttribute(attribute).isPresent()) {
            return;
        }

        mail.setAttribute(new Attribute(attribute, AttributeValue.of(getExtractedContent(mail))));
    }

    private ImmutableMap<String, AttributeValue<?>> getExtractedContent(Mail mail) throws MailetException {
        return AttributeUtils
                .getValueAndCastFromMail(mail, attribute, Serializable.class)
                .map(this::extractAttributeContent)
                .orElse(ImmutableMap.<String, AttributeValue<?>>of());
    }

    @SuppressWarnings("unchecked")
    private ImmutableMap<String, AttributeValue<?>> extractAttributeContent(Serializable attributeContent) {
        if (! (attributeContent instanceof Map)) {
            LOGGER.debug("Invalid attribute found into attribute {} class Map expected but {} found.",
                    attribute, attributeContent.getClass());
            return ImmutableMap.of();
        }

        Map<String, AttributeValue<?>> attributeMap = (Map<String, AttributeValue<?>>) attributeContent;
        return attributeMap
                .entrySet()
                .stream()
                .flatMap(this::extractObjectToByteArrayDto)
                .collect(ImmutableMap.toImmutableMap(Pair::getLeft, Pair::getRight));
    }

    public Stream<Pair<String, AttributeValue<BytesArrayDto>>> extractObjectToByteArrayDto(Entry<String, AttributeValue<?>> entry) {
        return convertToByteArray(entry.getValue().getValue())
                .flatMap(Throwing.function(this::extractContent).sneakyThrow())
                    .map(extractedContent -> Pair.of(entry.getKey(), AttributeValue.of(extractedContent)))
                    .map(Stream::of)
                    .orElse(Stream.of());
    }

    private Optional<byte[]> convertToByteArray(Object value) {
        if (value instanceof BytesArrayDto) {
            return Optional.of(((BytesArrayDto) value).getValues());
        } else if (value instanceof String) {
            return Optional.of(((String) value).getBytes(StandardCharsets.UTF_8));
        } 
        return Optional.empty();
    }

    private Optional<BytesArrayDto> extractContent(byte[] rawMime) throws MessagingException {
        try {
            MimeBodyPart mimeBodyPart = new MimeBodyPart(new ByteArrayInputStream(rawMime));
            return Optional.ofNullable(IOUtils.toByteArray(mimeBodyPart.getInputStream())).map(BytesArrayDto::new);
        } catch (IOException e) {
            LOGGER.error("Error while extracting content from mime part", e);
            return Optional.empty();
        } catch (ClassCastException e) {
            LOGGER.error("Invalid map attribute types.", e);
            return Optional.empty();
        }
    }

    @Override
    public String getMailetInfo() {
        return "MimeDecodingMailet";
    }

}
