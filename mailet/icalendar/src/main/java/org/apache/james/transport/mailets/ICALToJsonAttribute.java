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

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.core.MailAddress;
import org.apache.james.transport.mailets.model.ICAL;
import org.apache.james.util.OptionalUtils;
import org.apache.james.util.StreamUtils;
import org.apache.mailet.Attribute;
import org.apache.mailet.AttributeName;
import org.apache.mailet.AttributeUtils;
import org.apache.mailet.AttributeValue;
import org.apache.mailet.BytesArrayDto;
import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMailet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.fge.lambdas.Throwing;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import net.fortuna.ical4j.model.Calendar;

/**
 * ICALToJsonAttribute takes a map of ICAL4J objects attached as attribute, and output the map of corresponding json bytes as
 * an other attribute, with unique String keys.
 *
 * The JSON contains the following fields :
 *
 * <ul>
 *     <li><b>ical</b> : the raw ical string, in UTF-8</li>
 *     <li><b>sender</b> : the sender of the mail (compulsory, mail without sender will be discarded)</li>
 *     <li><b>recipient</b> : the recipient of the mail. If the mail have several recipients, each recipient will have
 *     its own JSON.</li>
 *     <li><b>uid</b> : the UID of the ical (optional)</li>
 *     <li><b>sequence</b> : the sequence of the ical (optional)</li>
 *     <li><b>dtstamp</b> : the date stamp of the ical (optional)</li>
 *     <li><b>method</b> : the method of the ical (optional)</li>
 *     <li><b>recurrence-id</b> : the recurrence-id of the ical (optional)</li>
 * </ul>
 *
 * Example are included in test call ICalToJsonAttributeTest.
 *
 *  Configuration example :
 *
 * <pre>
 *     <code>
 *         &lt;mailet matcher=??? class=ICALToJsonAttribute&gt;
 *             &lt;sourceAttribute&gt;icalendars&lt;/sourceAttribute&gt;
 *             &lt;destinationAttribute&gt;icalendarJson&lt;/destinationAttribute&gt;
 *         &lt;/mailet&gt;
 *     </code>
 * </pre>
 */
public class ICALToJsonAttribute extends GenericMailet {
    @SuppressWarnings("unchecked")
    private static final Class<Map<String, AttributeValue<Calendar>>> MAP_CALENDAR_CLASS = (Class<Map<String, AttributeValue<Calendar>>>)(Object) Map.class;
    @SuppressWarnings("unchecked")
    private static final Class<Map<String, AttributeValue<BytesArrayDto>>> MAP_BYTES_CLASS = (Class<Map<String, AttributeValue<BytesArrayDto>>>)(Object) Map.class;

    private static final Logger LOGGER = LoggerFactory.getLogger(ICALToJsonAttribute.class);

    @VisibleForTesting static final String SOURCE_CONFIG_ATTRIBUTE_NAME = "source";
    @VisibleForTesting static final String RAW_SOURCE_CONFIG_ATTRIBUTE_NAME = "rawSource";
    @VisibleForTesting static final String DESTINATION_CONFIG_ATTRIBUTE_NAME = "destination";
    @VisibleForTesting static final String DEFAULT_SOURCE_CONFIG_ATTRIBUTE_NAME = "icalendar";
    @VisibleForTesting static final String DEFAULT_RAW_SOURCE_CONFIG_ATTRIBUTE_NAME = "attachments";
    @VisibleForTesting static final String DEFAULT_DESTINATION_CONFIG_ATTRIBUTE_NAME = "icalendarJson";

    static {
        ICal4JConfigurator.configure();
    }

    private final ObjectMapper objectMapper;
    private AttributeName sourceAttributeName;
    private AttributeName rawSourceAttributeName;
    private AttributeName destinationAttributeName;

    public ICALToJsonAttribute() {
        this.objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module());
    }

    public AttributeName getSourceAttributeName() {
        return sourceAttributeName;
    }

    public AttributeName getRawSourceAttributeName() {
        return rawSourceAttributeName;
    }

    public AttributeName getDestinationAttributeName() {
        return destinationAttributeName;
    }

    @Override
    public String getMailetInfo() {
        return "ICALToJson Mailet";
    }

    @Override
    public void init() throws MessagingException {
        String extractedSourceAttributeName = getInitParameter(SOURCE_CONFIG_ATTRIBUTE_NAME, DEFAULT_SOURCE_CONFIG_ATTRIBUTE_NAME);
        String extractedRawSourceAttributeName = getInitParameter(RAW_SOURCE_CONFIG_ATTRIBUTE_NAME, DEFAULT_RAW_SOURCE_CONFIG_ATTRIBUTE_NAME);
        String extractedDestinationAttributeName = getInitParameter(DESTINATION_CONFIG_ATTRIBUTE_NAME, DEFAULT_DESTINATION_CONFIG_ATTRIBUTE_NAME);
        if (Strings.isNullOrEmpty(extractedSourceAttributeName)) {
            throw new MessagingException(SOURCE_CONFIG_ATTRIBUTE_NAME + " configuration parameter can not be null or empty");
        }
        if (Strings.isNullOrEmpty(extractedRawSourceAttributeName)) {
            throw new MessagingException(RAW_SOURCE_CONFIG_ATTRIBUTE_NAME + " configuration parameter can not be null or empty");
        }
        if (Strings.isNullOrEmpty(extractedDestinationAttributeName)) {
            throw new MessagingException(DESTINATION_CONFIG_ATTRIBUTE_NAME + " configuration parameter can not be null or empty");
        }
        sourceAttributeName = AttributeName.of(extractedSourceAttributeName);
        rawSourceAttributeName = AttributeName.of(extractedRawSourceAttributeName);
        destinationAttributeName = AttributeName.of(extractedDestinationAttributeName);
    }

    @Override
    public void service(Mail mail) throws MessagingException {
        if (!mail.getAttribute(sourceAttributeName).isPresent()) {
            return;
        }
        if (!mail.getAttribute(rawSourceAttributeName).isPresent()) {
            return;
        }

        retrieveSender(mail)
            .map(sender -> fillDestinationAttribute(mail, sender))
            .orElseGet(() -> {
                LOGGER.info("Skipping {} because no sender and no from", mail.getName());
                return null;
            });
    }

    public Object fillDestinationAttribute(Mail mail, String sender) {
        try {
            getCalendarMap(mail).ifPresent(calendars -> {
                getRawCalendarMap(mail).ifPresent(rawCalendars -> {
                    Map<String, AttributeValue<?>> jsonsInByteForm = calendars.entrySet()
                        .stream()
                        .flatMap(calendar -> toJson(calendar, rawCalendars, mail, sender))
                        .collect(ImmutableMap.toImmutableMap(Pair::getKey, Pair::getValue));
                    mail.setAttribute(new Attribute(destinationAttributeName, AttributeValue.of(jsonsInByteForm)));
                });
            });
        } catch (ClassCastException e) {
            LOGGER.error("Received a mail with {} not being an ICAL object for mail {}", sourceAttributeName, mail.getName(), e);
        }
        return null;
    }

    private Optional<Map<String, AttributeValue<Calendar>>> getCalendarMap(Mail mail) {
        return AttributeUtils.getValueAndCastFromMail(mail, sourceAttributeName, MAP_CALENDAR_CLASS);
    }

    private Optional<Map<String, AttributeValue<BytesArrayDto>>> getRawCalendarMap(Mail mail) {
        return AttributeUtils.getValueAndCastFromMail(mail, rawSourceAttributeName, MAP_BYTES_CLASS);
    }

    private Stream<Pair<String, AttributeValue<BytesArrayDto>>> toJson(Map.Entry<String, AttributeValue<Calendar>> entry, Map<String, AttributeValue<BytesArrayDto>> rawCalendars, Mail mail, String sender) {
        return mail.getRecipients()
            .stream()
            .flatMap(recipient -> toICAL(entry, rawCalendars, recipient, sender))
            .flatMap(ical -> toJson(ical, mail.getName()))
            .map(json -> Pair.of(UUID.randomUUID().toString(), AttributeValue.of(new BytesArrayDto(json.getBytes(StandardCharsets.UTF_8)))));
    }

    private Stream<String> toJson(ICAL ical, String mailName) {
        try {
            String writeValueAsString = objectMapper.writeValueAsString(ical);
            return Stream.of(writeValueAsString);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error while serializing Calendar for mail {}", mailName, e);
            return Stream.of();
        } catch (Exception e) {
            LOGGER.error("Exception caught while attaching ICAL to the email as JSON for mail {}", mailName, e);
            return Stream.of();
        }
    }

    private Stream<ICAL> toICAL(Map.Entry<String, AttributeValue<Calendar>> entry, Map<String, AttributeValue<BytesArrayDto>> rawCalendars, MailAddress recipient, String sender) {
        Calendar calendar = entry.getValue().getValue();
        Optional<AttributeValue<BytesArrayDto>> rawICal = Optional.ofNullable(rawCalendars.get(entry.getKey()));
        if (!rawICal.isPresent()) {
            LOGGER.debug("Cannot find matching raw ICAL from key: {}", entry.getKey());
            return Stream.of();
        }
        try {
            return Stream.of(ICAL.builder()
                .from(calendar, rawICal.get().getValue().getValues())
                .recipient(recipient)
                .sender(sender)
                .build());
        } catch (Exception e) {
            LOGGER.error("Exception while converting calendar to ICAL", e);
            return Stream.of();
        }
    }

    private Optional<String> retrieveSender(Mail mail) throws MessagingException {
        Optional<String> fromMime = StreamUtils.ofOptional(
            Optional.ofNullable(mail.getMessage())
                .map(Throwing.function(MimeMessage::getFrom).orReturn(new Address[]{})))
            .map(address -> (InternetAddress) address)
            .map(InternetAddress::getAddress)
            .findFirst();
        Optional<String> fromEnvelope = mail.getMaybeSender().asOptional()
            .map(MailAddress::asString);

        return OptionalUtils.or(
            fromMime,
            fromEnvelope);
    }
}
