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
import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMailet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;
import com.google.common.base.Strings;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(ICALToJsonAttribute.class);

    private static final String SOURCE_CONFIG_ATTRIBUTE_NAME = "source";
    private static final String RAW_SOURCE_CONFIG_ATTRIBUTE_NAME = "rawSource";
    private static final String DESTINATION_CONFIG_ATTRIBUTE_NAME = "destination";
    private static final String DEFAULT_SOURCE_CONFIG_ATTRIBUTE_NAME = "icalendar";
    private static final String DEFAULT_RAW_SOURCE_CONFIG_ATTRIBUTE_NAME = "attachments";
    private static final String DEFAULT_DESTINATION_CONFIG_ATTRIBUTE_NAME = "icalendarJson";

    public static final AttributeName SOURCE_ATTRIBUTE_NAME = AttributeName.of(SOURCE_CONFIG_ATTRIBUTE_NAME);
    public static final AttributeName RAW_SOURCE_ATTRIBUTE_NAME = AttributeName.of(RAW_SOURCE_CONFIG_ATTRIBUTE_NAME);
    public static final AttributeName DESTINATION_ATTRIBUTE_NAME = AttributeName.of(DESTINATION_CONFIG_ATTRIBUTE_NAME);
    public static final AttributeName DEFAULT_SOURCE_ATTRIBUTE_NAME = AttributeName.of(DEFAULT_SOURCE_CONFIG_ATTRIBUTE_NAME);
    public static final AttributeName DEFAULT_RAW_SOURCE_ATTRIBUTE_NAME = AttributeName.of(DEFAULT_RAW_SOURCE_CONFIG_ATTRIBUTE_NAME);
    public static final AttributeName DEFAULT_DESTINATION_ATTRIBUTE_NAME = AttributeName.of(DEFAULT_DESTINATION_CONFIG_ATTRIBUTE_NAME);

    static {
        ICal4JConfigurator.configure();
    }

    private final ObjectMapper objectMapper;
    private String sourceAttributeName;
    private String rawSourceAttributeName;
    private String destinationAttributeName;

    public ICALToJsonAttribute() {
        this.objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module());
    }

    public String getSourceAttributeName() {
        return sourceAttributeName;
    }

    public String getRawSourceAttributeName() {
        return rawSourceAttributeName;
    }

    public String getDestinationAttributeName() {
        return destinationAttributeName;
    }

    @Override
    public String getMailetInfo() {
        return "ICALToJson Mailet";
    }

    @Override
    public void init() throws MessagingException {
        sourceAttributeName = getInitParameter(SOURCE_CONFIG_ATTRIBUTE_NAME, DEFAULT_SOURCE_CONFIG_ATTRIBUTE_NAME);
        rawSourceAttributeName = getInitParameter(RAW_SOURCE_CONFIG_ATTRIBUTE_NAME, DEFAULT_RAW_SOURCE_CONFIG_ATTRIBUTE_NAME);
        destinationAttributeName = getInitParameter(DESTINATION_CONFIG_ATTRIBUTE_NAME, DEFAULT_DESTINATION_CONFIG_ATTRIBUTE_NAME);
        if (Strings.isNullOrEmpty(sourceAttributeName)) {
            throw new MessagingException(SOURCE_CONFIG_ATTRIBUTE_NAME + " configuration parameter can not be null or empty");
        }
        if (Strings.isNullOrEmpty(rawSourceAttributeName)) {
            throw new MessagingException(RAW_SOURCE_CONFIG_ATTRIBUTE_NAME + " configuration parameter can not be null or empty");
        }
        if (Strings.isNullOrEmpty(destinationAttributeName)) {
            throw new MessagingException(DESTINATION_CONFIG_ATTRIBUTE_NAME + " configuration parameter can not be null or empty");
        }
    }

    @Override
    public void service(Mail mail) throws MessagingException {
        if (!mail.getAttribute(AttributeName.of(sourceAttributeName)).isPresent()) {
            return;
        }
        if (!mail.getAttribute(AttributeName.of(rawSourceAttributeName)).isPresent()) {
            return;
        }
        Optional<String> sender = retrieveSender(mail);
        if (!sender.isPresent()) {
            LOGGER.info("Skipping {} because no sender and no from", mail.getName());
            return;
        }
        try {
            getCalendarMap(mail)
                .ifPresent(calendars -> {
                    getRawCalendarMap(mail)
                        .ifPresent(rawCalendars -> {
                            Map<String, byte[]> jsonsInByteForm = calendars.entrySet()
                                .stream()
                                .flatMap(calendar -> toJson(calendar, rawCalendars, mail, sender.get()))
                                .collect(Guavate.toImmutableMap(Pair::getKey, Pair::getValue));
                            mail.setAttribute(new Attribute(AttributeName.of(destinationAttributeName), AttributeValue.of(jsonsInByteForm)));
                        });
                });
        } catch (ClassCastException e) {
            LOGGER.error("Received a mail with {} not being an ICAL object for mail {}", sourceAttributeName, mail.getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<Map<String, Calendar>> getCalendarMap(Mail mail) {
        return AttributeUtils.getValueAndCastFromMail(mail, AttributeName.of(sourceAttributeName), (Class<Map<String, Calendar>>)(Object) Map.class);
    }

    @SuppressWarnings("unchecked")
    private Optional<Map<String, byte[]>> getRawCalendarMap(Mail mail) {
        return AttributeUtils.getValueAndCastFromMail(mail, AttributeName.of(rawSourceAttributeName), (Class<Map<String, byte[]>>)(Object) Map.class);
    }

    private Stream<Pair<String, byte[]>> toJson(Map.Entry<String, Calendar> entry, Map<String, byte[]> rawCalendars, Mail mail, String sender) {
        return mail.getRecipients()
            .stream()
            .flatMap(recipient -> toICAL(entry, rawCalendars, recipient, sender))
            .flatMap(ical -> toJson(ical, mail.getName()))
            .map(json -> Pair.of(UUID.randomUUID().toString(), json.getBytes(StandardCharsets.UTF_8)));
    }

    private Stream<String> toJson(ICAL ical, String mailName) {
        try {
            return Stream.of(objectMapper.writeValueAsString(ical));
        } catch (JsonProcessingException e) {
            LOGGER.error("Error while serializing Calendar for mail {}", mailName, e);
            return Stream.of();
        } catch (Exception e) {
            LOGGER.error("Exception caught while attaching ICAL to the email as JSON for mail {}", mailName, e);
            return Stream.of();
        }
    }

    private Stream<ICAL> toICAL(Map.Entry<String, Calendar> entry, Map<String, byte[]> rawCalendars, MailAddress recipient, String sender) {
        Calendar calendar = entry.getValue();
        byte[] rawICal = rawCalendars.get(entry.getKey());
        if (rawICal == null) {
            LOGGER.debug("Cannot find matching raw ICAL from key: {}", entry.getKey());
            return Stream.of();
        }
        try {
            return Stream.of(ICAL.builder()
                .from(calendar, rawICal)
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
        Optional<String> fromEnvelope = Optional.ofNullable(mail.getSender())
            .map(MailAddress::asString);

        return OptionalUtils.or(
            fromMime,
            fromEnvelope);
    }
}
