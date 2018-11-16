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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.mail.MessagingException;

import org.apache.james.core.MailAddress;
import org.apache.james.core.builder.MimeMessageBuilder;
import org.apache.james.util.ClassLoaderUtils;
import org.apache.james.util.MimeMessageUtil;
import org.apache.mailet.Attribute;
import org.apache.mailet.AttributeName;
import org.apache.mailet.AttributeUtils;
import org.apache.mailet.AttributeValue;
import org.apache.mailet.BytesArrayDto;
import org.apache.mailet.Mail;
import org.apache.mailet.base.MailAddressFixture;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.FakeMailetConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.fasterxml.jackson.core.util.BufferRecyclers;
import com.google.common.collect.ImmutableMap;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;

public class ICALToJsonAttributeTest {
    public static final MailAddress SENDER = MailAddressFixture.ANY_AT_JAMES;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private ICALToJsonAttribute testee;

    @Before
    public void setUp() {
        testee = new ICALToJsonAttribute();
    }

    @Test
    public void getMailetInfoShouldReturnExpectedValue() {
        assertThat(testee.getMailetInfo()).isEqualTo("ICALToJson Mailet");
    }

    @Test
    public void initShouldSetAttributesWhenAbsent() throws Exception {
        testee.init(FakeMailetConfig.builder().build());

        assertThat(testee.getSourceAttributeName()).isEqualTo(ICALToJsonAttribute.DEFAULT_SOURCE_ATTRIBUTE_NAME);
        assertThat(testee.getDestinationAttributeName()).isEqualTo(ICALToJsonAttribute.DEFAULT_DESTINATION_ATTRIBUTE_NAME);
    }

    @Test
    public void initShouldThrowOnEmptySourceAttribute() throws Exception {
        expectedException.expect(MessagingException.class);

        testee.init(FakeMailetConfig.builder()
            .setProperty(ICALToJsonAttribute.SOURCE_ATTRIBUTE_NAME.asString(), "")
            .build());
    }

    @Test
    public void initShouldThrowOnEmptyRawSourceAttribute() throws Exception {
        expectedException.expect(MessagingException.class);

        testee.init(FakeMailetConfig.builder()
            .setProperty(ICALToJsonAttribute.RAW_SOURCE_ATTRIBUTE_NAME.asString(), "")
            .build());
    }

    @Test
    public void initShouldThrowOnEmptyDestinationAttribute() throws Exception {
        expectedException.expect(MessagingException.class);

        testee.init(FakeMailetConfig.builder()
            .setProperty(ICALToJsonAttribute.DESTINATION_ATTRIBUTE_NAME.asString(), "")
            .build());
    }

    @Test
    public void initShouldSetAttributesWhenPresent() throws Exception {
        AttributeName destination = AttributeName.of("myDestination");
        AttributeName source = AttributeName.of("mySource");
        AttributeName raw = AttributeName.of("myRaw");
        testee.init(FakeMailetConfig.builder()
            .setProperty(ICALToJsonAttribute.SOURCE_ATTRIBUTE_NAME.asString(), source.asString())
            .setProperty(ICALToJsonAttribute.DESTINATION_ATTRIBUTE_NAME.asString(), destination.asString())
            .setProperty(ICALToJsonAttribute.RAW_SOURCE_ATTRIBUTE_NAME.asString(), raw.asString())
            .build());

        assertThat(testee.getSourceAttributeName()).isEqualTo(source);
        assertThat(testee.getDestinationAttributeName()).isEqualTo(destination);
        assertThat(testee.getRawSourceAttributeName()).isEqualTo(raw);
    }

    @Test
    public void serviceShouldFilterMailsWithoutICALs() throws Exception {
        testee.init(FakeMailetConfig.builder().build());

        Mail mail = FakeMail.builder()
            .sender(SENDER)
            .recipient(MailAddressFixture.OTHER_AT_JAMES)
            .build();
        testee.service(mail);

        assertThat(AttributeUtils.getAttributeValueFromMail(mail, ICALToJsonAttribute.DEFAULT_DESTINATION_ATTRIBUTE_NAME))
            .isEmpty();
    }

    @Test
    public void serviceShouldNotFailOnWrongAttributeType() throws Exception {
        testee.init(FakeMailetConfig.builder().build());

        Mail mail = FakeMail.builder()
            .sender(SENDER)
            .recipient(MailAddressFixture.OTHER_AT_JAMES)
            .attribute(new Attribute(ICALToJsonAttribute.DEFAULT_SOURCE_ATTRIBUTE_NAME, AttributeValue.of("wrong type")))
            .build();
        testee.service(mail);

        assertThat(AttributeUtils.getAttributeValueFromMail(mail, ICALToJsonAttribute.DEFAULT_DESTINATION_ATTRIBUTE_NAME))
            .isEmpty();
    }

    @Test
    public void serviceShouldNotFailOnWrongRawAttributeType() throws Exception {
        testee.init(FakeMailetConfig.builder().build());

        Mail mail = FakeMail.builder()
            .sender(SENDER)
            .recipient(MailAddressFixture.OTHER_AT_JAMES)
            .attribute(new Attribute(ICALToJsonAttribute.DEFAULT_RAW_SOURCE_ATTRIBUTE_NAME, AttributeValue.of("wrong type")))
            .build();
        testee.service(mail);

        assertThat(AttributeUtils.getAttributeValueFromMail(mail, ICALToJsonAttribute.DEFAULT_DESTINATION_ATTRIBUTE_NAME))
            .isEmpty();
    }

    @Test
    public void serviceShouldNotFailOnWrongAttributeParameter() throws Exception {
        testee.init(FakeMailetConfig.builder().build());

        ImmutableMap<String, AttributeValue<?>> wrongParametrizedMap = ImmutableMap.of("key", AttributeValue.of("value"));
        Mail mail = FakeMail.builder()
            .sender(SENDER)
            .recipient(MailAddressFixture.OTHER_AT_JAMES)
            .attribute(new Attribute(ICALToJsonAttribute.DEFAULT_SOURCE_ATTRIBUTE_NAME, AttributeValue.of(wrongParametrizedMap)))
            .build();
        testee.service(mail);

        assertThat(AttributeUtils.getAttributeValueFromMail(mail, ICALToJsonAttribute.DEFAULT_DESTINATION_ATTRIBUTE_NAME))
            .isEmpty();
    }

    @Test
    public void serviceShouldNotFailOnWrongRawAttributeParameter() throws Exception {
        testee.init(FakeMailetConfig.builder().build());

        ImmutableMap<String, AttributeValue<?>> wrongParametrizedMap = ImmutableMap.of("key", AttributeValue.of("value"));
        Mail mail = FakeMail.builder()
            .sender(SENDER)
            .recipient(MailAddressFixture.OTHER_AT_JAMES)
            .attribute(new Attribute(ICALToJsonAttribute.DEFAULT_RAW_SOURCE_ATTRIBUTE_NAME, AttributeValue.of(wrongParametrizedMap)))
            .build();
        testee.service(mail);

        assertThat(AttributeUtils.getAttributeValueFromMail(mail, ICALToJsonAttribute.DEFAULT_DESTINATION_ATTRIBUTE_NAME))
            .isEmpty();
    }

    @Test
    public void serviceShouldFilterMailsWithoutSender() throws Exception {
        testee.init(FakeMailetConfig.builder().build());

        byte[] ics = ClassLoaderUtils.getSystemResourceAsByteArray("ics/meeting.ics");
        Calendar calendar = new CalendarBuilder().build(new ByteArrayInputStream(ics));
        ImmutableMap<String, AttributeValue<?>> icals = ImmutableMap.of("key", AttributeValue.ofSerializable(calendar));
        Mail mail = FakeMail.builder()
            .recipient(MailAddressFixture.OTHER_AT_JAMES)
            .attribute(new Attribute(ICALToJsonAttribute.DEFAULT_SOURCE_ATTRIBUTE_NAME, AttributeValue.of(icals)))
            .attribute(new Attribute(ICALToJsonAttribute.DEFAULT_RAW_SOURCE_ATTRIBUTE_NAME, AttributeValue.of(icals)))
            .build();
        testee.service(mail);

        assertThat(AttributeUtils.getAttributeValueFromMail(mail, ICALToJsonAttribute.DEFAULT_DESTINATION_ATTRIBUTE_NAME))
            .isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void serviceShouldAttachEmptyListWhenNoRecipient() throws Exception {
        testee.init(FakeMailetConfig.builder().build());

        byte[] ics = ClassLoaderUtils.getSystemResourceAsByteArray("ics/meeting.ics");
        Calendar calendar = new CalendarBuilder().build(new ByteArrayInputStream(ics));
        ImmutableMap<String, AttributeValue<?>> icals = ImmutableMap.of("key", AttributeValue.ofSerializable(calendar));
        ImmutableMap<String, AttributeValue<?>> rawIcals = ImmutableMap.of("key", AttributeValue.of(new BytesArrayDto(ics)));
        Mail mail = FakeMail.builder()
            .sender(SENDER)
            .attribute(new Attribute(ICALToJsonAttribute.DEFAULT_SOURCE_ATTRIBUTE_NAME, AttributeValue.of(icals)))
            .attribute(new Attribute(ICALToJsonAttribute.DEFAULT_RAW_SOURCE_ATTRIBUTE_NAME, AttributeValue.of(rawIcals)))
            .build();
        testee.service(mail);

        Optional<Map<?, ?>> result = AttributeUtils.getValueAndCastFromMail(mail, ICALToJsonAttribute.DEFAULT_DESTINATION_ATTRIBUTE_NAME, (Class<Map<?,?>>)(Object) Map.class);
        assertThat(result).isPresent();
        assertThat(result.get()).isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void serviceShouldAttachJson() throws Exception {
        testee.init(FakeMailetConfig.builder().build());

        byte[] ics = ClassLoaderUtils.getSystemResourceAsByteArray("ics/meeting.ics");
        Calendar calendar = new CalendarBuilder().build(new ByteArrayInputStream(ics));
        ImmutableMap<String, AttributeValue<?>> icals = ImmutableMap.of("key", AttributeValue.ofSerializable(calendar));
        ImmutableMap<String, AttributeValue<?>> rawIcals = ImmutableMap.of("key", AttributeValue.of(new BytesArrayDto(ics)));
        MailAddress recipient = MailAddressFixture.ANY_AT_JAMES2;
        Mail mail = FakeMail.builder()
            .sender(SENDER)
            .recipient(recipient)
            .attribute(new Attribute(ICALToJsonAttribute.DEFAULT_SOURCE_ATTRIBUTE_NAME, AttributeValue.of(icals)))
            .attribute(new Attribute(ICALToJsonAttribute.DEFAULT_RAW_SOURCE_ATTRIBUTE_NAME, AttributeValue.of(rawIcals)))
            .build();
        testee.service(mail);

        Optional<Map<String, AttributeValue<BytesArrayDto>>> jsons = AttributeUtils.getValueAndCastFromMail(mail, ICALToJsonAttribute.DEFAULT_DESTINATION_ATTRIBUTE_NAME, (Class<Map<String, AttributeValue<BytesArrayDto>>>)(Object) Map.class);
        assertThat(jsons).isPresent();
        assertThat(jsons.get()).hasSize(1);
        assertThatJson(new String(jsons.get().values().iterator().next().getValue().getValues(), StandardCharsets.UTF_8))
            .isEqualTo("{" +
                "\"ical\": \"" + toJsonValue(ics) + "\"," +
                "\"sender\": \"" + SENDER.asString() + "\"," +
                "\"recipient\": \"" + recipient.asString() + "\"," +
                "\"uid\": \"f1514f44bf39311568d640727cff54e819573448d09d2e5677987ff29caa01a9e047feb2aab16e43439a608f28671ab7c10e754ce92be513f8e04ae9ff15e65a9819cf285a6962bc\"," +
                "\"sequence\": \"0\"," +
                "\"dtstamp\": \"20170106T115036Z\"," +
                "\"method\": \"REQUEST\"," +
                "\"recurrence-id\": null" +
                "}");
    }

    private String toJsonValue(byte[] ics) {
        return new String(BufferRecyclers.getJsonStringEncoder().quoteAsUTF8(new String(ics, StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void serviceShouldAttachJsonForSeveralRecipient() throws Exception {
        testee.init(FakeMailetConfig.builder().build());

        byte[] ics = ClassLoaderUtils.getSystemResourceAsByteArray("ics/meeting.ics");
        Calendar calendar = new CalendarBuilder().build(new ByteArrayInputStream(ics));
        ImmutableMap<String, AttributeValue<?>> icals = ImmutableMap.of("key", AttributeValue.ofSerializable(calendar));
        ImmutableMap<String, AttributeValue<?>> rawIcals = ImmutableMap.of("key", AttributeValue.of(new BytesArrayDto(ics)));
        Mail mail = FakeMail.builder()
            .sender(SENDER)
            .recipients(MailAddressFixture.OTHER_AT_JAMES, MailAddressFixture.ANY_AT_JAMES2)
            .attribute(new Attribute(ICALToJsonAttribute.DEFAULT_SOURCE_ATTRIBUTE_NAME, AttributeValue.of(icals)))
            .attribute(new Attribute(ICALToJsonAttribute.DEFAULT_RAW_SOURCE_ATTRIBUTE_NAME, AttributeValue.of(rawIcals)))
            .build();
        testee.service(mail);

        Optional<Map<String, AttributeValue<BytesArrayDto>>> jsons = AttributeUtils.getValueAndCastFromMail(mail, ICALToJsonAttribute.DEFAULT_DESTINATION_ATTRIBUTE_NAME, (Class<Map<String, AttributeValue<BytesArrayDto>>>)(Object) Map.class);
        assertThat(jsons).isPresent();
        assertThat(jsons.get()).hasSize(2);
        List<String> actual = toSortedValueList(jsons.get());

        assertThatJson(actual.get(0)).isEqualTo("{" +
            "\"ical\": \"" + toJsonValue(ics) + "\"," +
            "\"sender\": \"" + SENDER.asString() + "\"," +
            "\"recipient\": \"" + MailAddressFixture.ANY_AT_JAMES2.asString() + "\"," +
            "\"uid\": \"f1514f44bf39311568d640727cff54e819573448d09d2e5677987ff29caa01a9e047feb2aab16e43439a608f28671ab7c10e754ce92be513f8e04ae9ff15e65a9819cf285a6962bc\"," +
            "\"sequence\": \"0\"," +
            "\"dtstamp\": \"20170106T115036Z\"," +
            "\"method\": \"REQUEST\"," +
            "\"recurrence-id\": null" +
            "}");
        assertThatJson(actual.get(1)).isEqualTo("{" +
            "\"ical\": \"" + toJsonValue(ics) + "\"," +
            "\"sender\": \"" + SENDER.asString() + "\"," +
            "\"recipient\": \"" + MailAddressFixture.OTHER_AT_JAMES.asString() + "\"," +
            "\"uid\": \"f1514f44bf39311568d640727cff54e819573448d09d2e5677987ff29caa01a9e047feb2aab16e43439a608f28671ab7c10e754ce92be513f8e04ae9ff15e65a9819cf285a6962bc\"," +
            "\"sequence\": \"0\"," +
            "\"dtstamp\": \"20170106T115036Z\"," +
            "\"method\": \"REQUEST\"," +
            "\"recurrence-id\": null" +
            "}");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void serviceShouldAttachJsonForSeveralICALs() throws Exception {
        testee.init(FakeMailetConfig.builder().build());

        byte[] ics = ClassLoaderUtils.getSystemResourceAsByteArray("ics/meeting.ics");
        byte[] ics2 = ClassLoaderUtils.getSystemResourceAsByteArray("ics/meeting_2.ics");
        Calendar calendar = new CalendarBuilder().build(new ByteArrayInputStream(ics));
        Calendar calendar2 = new CalendarBuilder().build(new ByteArrayInputStream(ics2));
        ImmutableMap<String, AttributeValue<?>> icals = ImmutableMap.of("key", AttributeValue.ofSerializable(calendar), "key2", AttributeValue.ofSerializable(calendar2));
        ImmutableMap<String, AttributeValue<?>> rawIcals = ImmutableMap.of("key", AttributeValue.of(new BytesArrayDto(ics)), "key2", AttributeValue.of(new BytesArrayDto(ics2)));
        MailAddress recipient = MailAddressFixture.OTHER_AT_JAMES;
        Mail mail = FakeMail.builder()
            .sender(SENDER)
            .recipient(recipient)
            .attribute(new Attribute(ICALToJsonAttribute.DEFAULT_SOURCE_ATTRIBUTE_NAME, AttributeValue.of(icals)))
            .attribute(new Attribute(ICALToJsonAttribute.DEFAULT_RAW_SOURCE_ATTRIBUTE_NAME, AttributeValue.of(rawIcals)))
            .build();
        testee.service(mail);

        Optional<Map<String, AttributeValue<BytesArrayDto>>> jsons = AttributeUtils.getValueAndCastFromMail(mail, ICALToJsonAttribute.DEFAULT_DESTINATION_ATTRIBUTE_NAME, (Class<Map<String, AttributeValue<BytesArrayDto>>>)(Object) Map.class);
        assertThat(jsons).isPresent();
        assertThat(jsons.get()).hasSize(2);
        List<String> actual = toSortedValueList(jsons.get());

        assertThatJson(actual.get(0)).isEqualTo("{" +
            "\"ical\": \"" + toJsonValue(ics2) + "\"," +
            "\"sender\": \"" + SENDER.asString() + "\"," +
            "\"recipient\": \"" + recipient.asString() + "\"," +
            "\"uid\": \"f1514f44bf39311568d64072ac247c17656ceafde3b4b3eba961c8c5184cdc6ee047feb2aab16e43439a608f28671ab7c10e754c301b1e32001ad51dd20eac2fc7af20abf4093bbe\"," +
            "\"sequence\": \"0\"," +
            "\"dtstamp\": \"20170103T103250Z\"," +
            "\"method\": \"REQUEST\"," +
            "\"recurrence-id\": null" +
            "}");
        assertThatJson(actual.get(1)).isEqualTo("{" +
            "\"ical\": \"" + toJsonValue(ics) + "\"," +
            "\"sender\": \"" + SENDER.asString() + "\"," +
            "\"recipient\": \"" + recipient.asString() + "\"," +
            "\"uid\": \"f1514f44bf39311568d640727cff54e819573448d09d2e5677987ff29caa01a9e047feb2aab16e43439a608f28671ab7c10e754ce92be513f8e04ae9ff15e65a9819cf285a6962bc\"," +
            "\"sequence\": \"0\"," +
            "\"dtstamp\": \"20170106T115036Z\"," +
            "\"method\": \"REQUEST\"," +
            "\"recurrence-id\": null" +
            "}");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void serviceShouldFilterInvalidICS() throws Exception {
        testee.init(FakeMailetConfig.builder().build());

        byte[] ics = ClassLoaderUtils.getSystemResourceAsByteArray("ics/meeting.ics");
        byte[] ics2 = ClassLoaderUtils.getSystemResourceAsByteArray("ics/meeting_without_uid.ics");
        Calendar calendar = new CalendarBuilder().build(new ByteArrayInputStream(ics));
        Calendar calendar2 = new CalendarBuilder().build(new ByteArrayInputStream(ics2));
        ImmutableMap<String, AttributeValue<?>> icals = ImmutableMap.of("key", AttributeValue.ofSerializable(calendar), "key2", AttributeValue.ofSerializable(calendar2));
        ImmutableMap<String, AttributeValue<?>> rawIcals = ImmutableMap.of("key", AttributeValue.of(new BytesArrayDto(ics)), "key2", AttributeValue.of(new BytesArrayDto(ics2)));
        MailAddress recipient = MailAddressFixture.OTHER_AT_JAMES;
        Mail mail = FakeMail.builder()
            .sender(SENDER)
            .recipient(recipient)
            .attribute(new Attribute(ICALToJsonAttribute.DEFAULT_SOURCE_ATTRIBUTE_NAME, AttributeValue.of(icals)))
            .attribute(new Attribute(ICALToJsonAttribute.DEFAULT_RAW_SOURCE_ATTRIBUTE_NAME, AttributeValue.of(rawIcals)))
            .build();
        testee.service(mail);

        Optional<Map<String, AttributeValue<BytesArrayDto>>> jsons = AttributeUtils.getValueAndCastFromMail(mail, ICALToJsonAttribute.DEFAULT_DESTINATION_ATTRIBUTE_NAME, (Class<Map<String, AttributeValue<BytesArrayDto>>>)(Object) Map.class);
        assertThat(jsons).isPresent();
        assertThat(jsons.get()).hasSize(1);
        List<String> actual = toSortedValueList(jsons.get());

        assertThatJson(actual.get(0)).isEqualTo("{" +
            "\"ical\": \"" + toJsonValue(ics) + "\"," +
            "\"sender\": \"" + SENDER.asString() + "\"," +
            "\"recipient\": \"" + recipient.asString() + "\"," +
            "\"uid\": \"f1514f44bf39311568d640727cff54e819573448d09d2e5677987ff29caa01a9e047feb2aab16e43439a608f28671ab7c10e754ce92be513f8e04ae9ff15e65a9819cf285a6962bc\"," +
            "\"sequence\": \"0\"," +
            "\"dtstamp\": \"20170106T115036Z\"," +
            "\"method\": \"REQUEST\"," +
            "\"recurrence-id\": null" +
            "}");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void serviceShouldFilterNonExistingKeys() throws Exception {
        testee.init(FakeMailetConfig.builder().build());

        byte[] ics = ClassLoaderUtils.getSystemResourceAsByteArray("ics/meeting.ics");
        byte[] ics2 = ClassLoaderUtils.getSystemResourceAsByteArray("ics/meeting_2.ics");
        Calendar calendar = new CalendarBuilder().build(new ByteArrayInputStream(ics));
        Calendar calendar2 = new CalendarBuilder().build(new ByteArrayInputStream(ics2));
        ImmutableMap<String, AttributeValue<?>> icals = ImmutableMap.of("key", AttributeValue.ofSerializable(calendar), "key2", AttributeValue.ofSerializable(calendar2));
        ImmutableMap<String, AttributeValue<?>> rawIcals = ImmutableMap.of("key", AttributeValue.of(new BytesArrayDto(ics)));
        MailAddress recipient = MailAddressFixture.OTHER_AT_JAMES;
        Mail mail = FakeMail.builder()
            .sender(SENDER)
            .recipient(recipient)
            .attribute(new Attribute(ICALToJsonAttribute.DEFAULT_SOURCE_ATTRIBUTE_NAME, AttributeValue.of(icals)))
            .attribute(new Attribute(ICALToJsonAttribute.DEFAULT_RAW_SOURCE_ATTRIBUTE_NAME, AttributeValue.of(rawIcals)))
            .build();
        testee.service(mail);

        Optional<Map<String, AttributeValue<BytesArrayDto>>> jsons = AttributeUtils.getValueAndCastFromMail(mail, ICALToJsonAttribute.DEFAULT_DESTINATION_ATTRIBUTE_NAME, (Class<Map<String, AttributeValue<BytesArrayDto>>>)(Object) Map.class);
        assertThat(jsons).isPresent();
        assertThat(jsons.get()).hasSize(1);
        List<String> actual = toSortedValueList(jsons.get());

        assertThatJson(actual.get(0)).isEqualTo("{" +
            "\"ical\": \"" + toJsonValue(ics) + "\"," +
            "\"sender\": \"" + SENDER.asString() + "\"," +
            "\"recipient\": \"" + recipient.asString() + "\"," +
            "\"uid\": \"f1514f44bf39311568d640727cff54e819573448d09d2e5677987ff29caa01a9e047feb2aab16e43439a608f28671ab7c10e754ce92be513f8e04ae9ff15e65a9819cf285a6962bc\"," +
            "\"sequence\": \"0\"," +
            "\"dtstamp\": \"20170106T115036Z\"," +
            "\"method\": \"REQUEST\"," +
            "\"recurrence-id\": null" +
            "}");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void serviceShouldUseFromWhenSpecified() throws Exception {
        testee.init(FakeMailetConfig.builder().build());

        byte[] ics = ClassLoaderUtils.getSystemResourceAsByteArray("ics/meeting.ics");
        Calendar calendar = new CalendarBuilder().build(new ByteArrayInputStream(ics));
        ImmutableMap<String, AttributeValue<?>> icals = ImmutableMap.of("key", AttributeValue.ofSerializable(calendar));
        ImmutableMap<String, AttributeValue<?>> rawIcals = ImmutableMap.of("key", AttributeValue.of(new BytesArrayDto(ics)));
        MailAddress recipient = MailAddressFixture.ANY_AT_JAMES2;
        String from = MailAddressFixture.OTHER_AT_JAMES.asString();
        Mail mail = FakeMail.builder()
            .sender(SENDER)
            .recipient(recipient)
            .mimeMessage(MimeMessageBuilder.mimeMessageBuilder()
                .addFrom(from))
            .attribute(new Attribute(ICALToJsonAttribute.DEFAULT_SOURCE_ATTRIBUTE_NAME, AttributeValue.of(icals)))
            .attribute(new Attribute(ICALToJsonAttribute.DEFAULT_RAW_SOURCE_ATTRIBUTE_NAME, AttributeValue.of(rawIcals)))
            .build();
        testee.service(mail);

        Optional<Map<String, AttributeValue<BytesArrayDto>>> jsons = AttributeUtils.getValueAndCastFromMail(mail, ICALToJsonAttribute.DEFAULT_DESTINATION_ATTRIBUTE_NAME, (Class<Map<String, AttributeValue<BytesArrayDto>>>)(Object) Map.class);
        assertThat(jsons).isPresent();
        assertThat(jsons.get()).hasSize(1);
        assertThatJson(new String(jsons.get().values().iterator().next().getValue().getValues(), StandardCharsets.UTF_8))
            .isEqualTo("{" +
                "\"ical\": \"" + toJsonValue(ics) + "\"," +
                "\"sender\": \"" + from + "\"," +
                "\"recipient\": \"" + recipient.asString() + "\"," +
                "\"uid\": \"f1514f44bf39311568d640727cff54e819573448d09d2e5677987ff29caa01a9e047feb2aab16e43439a608f28671ab7c10e754ce92be513f8e04ae9ff15e65a9819cf285a6962bc\"," +
                "\"sequence\": \"0\"," +
                "\"dtstamp\": \"20170106T115036Z\"," +
                "\"method\": \"REQUEST\"," +
                "\"recurrence-id\": null" +
                "}");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void serviceShouldSupportMimeMessagesWithoutFromFields() throws Exception {
        testee.init(FakeMailetConfig.builder().build());

        byte[] ics = ClassLoaderUtils.getSystemResourceAsByteArray("ics/meeting.ics");
        Calendar calendar = new CalendarBuilder().build(new ByteArrayInputStream(ics));
        ImmutableMap<String, AttributeValue<?>> icals = ImmutableMap.of("key", AttributeValue.ofSerializable(calendar));
        ImmutableMap<String, AttributeValue<?>> rawIcals = ImmutableMap.of("key", AttributeValue.of(new BytesArrayDto(ics)));
        MailAddress recipient = MailAddressFixture.ANY_AT_JAMES2;
        Mail mail = FakeMail.builder()
            .sender(SENDER)
            .recipient(recipient)
            .mimeMessage(MimeMessageUtil.defaultMimeMessage())
            .attribute(new Attribute(ICALToJsonAttribute.DEFAULT_SOURCE_ATTRIBUTE_NAME, AttributeValue.of(icals)))
            .attribute(new Attribute(ICALToJsonAttribute.DEFAULT_RAW_SOURCE_ATTRIBUTE_NAME, AttributeValue.of(rawIcals)))
            .build();
        testee.service(mail);

        Optional<Map<String, AttributeValue<BytesArrayDto>>> jsons = AttributeUtils.getValueAndCastFromMail(mail, ICALToJsonAttribute.DEFAULT_DESTINATION_ATTRIBUTE_NAME, (Class<Map<String, AttributeValue<BytesArrayDto>>>)(Object) Map.class);
        assertThat(jsons).isPresent();
        assertThat(jsons.get()).hasSize(1);
        assertThatJson(new String(jsons.get().values().iterator().next().getValue().getValues(), StandardCharsets.UTF_8))
            .isEqualTo("{" +
                "\"ical\": \"" + toJsonValue(ics) + "\"," +
                "\"sender\": \"" + SENDER.asString() + "\"," +
                "\"recipient\": \"" + recipient.asString() + "\"," +
                "\"uid\": \"f1514f44bf39311568d640727cff54e819573448d09d2e5677987ff29caa01a9e047feb2aab16e43439a608f28671ab7c10e754ce92be513f8e04ae9ff15e65a9819cf285a6962bc\"," +
                "\"sequence\": \"0\"," +
                "\"dtstamp\": \"20170106T115036Z\"," +
                "\"method\": \"REQUEST\"," +
                "\"recurrence-id\": null" +
                "}");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void serviceShouldUseFromWhenSpecifiedAndNoSender() throws Exception {
        testee.init(FakeMailetConfig.builder().build());

        byte[] ics = ClassLoaderUtils.getSystemResourceAsByteArray("ics/meeting.ics");
        Calendar calendar = new CalendarBuilder().build(new ByteArrayInputStream(ics));
        ImmutableMap<String, AttributeValue<?>> icals = ImmutableMap.of("key", AttributeValue.ofSerializable(calendar));
        ImmutableMap<String, AttributeValue<?>> rawIcals = ImmutableMap.of("key", AttributeValue.of(new BytesArrayDto(ics)));
        MailAddress recipient = MailAddressFixture.ANY_AT_JAMES2;
        String from = MailAddressFixture.OTHER_AT_JAMES.asString();
        Mail mail = FakeMail.builder()
            .recipient(recipient)
            .mimeMessage(MimeMessageBuilder.mimeMessageBuilder()
                .addFrom(from))
            .attribute(new Attribute(ICALToJsonAttribute.DEFAULT_SOURCE_ATTRIBUTE_NAME, AttributeValue.of(icals)))
            .attribute(new Attribute(ICALToJsonAttribute.DEFAULT_RAW_SOURCE_ATTRIBUTE_NAME, AttributeValue.of(rawIcals)))
            .build();
        testee.service(mail);

        Optional<Map<String, AttributeValue<BytesArrayDto>>> jsons = AttributeUtils.getValueAndCastFromMail(mail, ICALToJsonAttribute.DEFAULT_DESTINATION_ATTRIBUTE_NAME, (Class<Map<String, AttributeValue<BytesArrayDto>>>)(Object) Map.class);
        assertThat(jsons).isPresent();
        assertThat(jsons.get()).hasSize(1);
        assertThatJson(new String(jsons.get().values().iterator().next().getValue().getValues(), StandardCharsets.UTF_8))
            .isEqualTo("{" +
                "\"ical\": \"" + toJsonValue(ics) + "\"," +
                "\"sender\": \"" + from + "\"," +
                "\"recipient\": \"" + recipient.asString() + "\"," +
                "\"uid\": \"f1514f44bf39311568d640727cff54e819573448d09d2e5677987ff29caa01a9e047feb2aab16e43439a608f28671ab7c10e754ce92be513f8e04ae9ff15e65a9819cf285a6962bc\"," +
                "\"sequence\": \"0\"," +
                "\"dtstamp\": \"20170106T115036Z\"," +
                "\"method\": \"REQUEST\"," +
                "\"recurrence-id\": null" +
                "}");
    }

    private List<String> toSortedValueList(Map<String, AttributeValue<BytesArrayDto>> map) {
        return map.values()
                .stream()
                .map(bytes -> new String(bytes.getValue().getValues(), StandardCharsets.UTF_8))
                .sorted()
                .collect(Collectors.toList());
    }
}
