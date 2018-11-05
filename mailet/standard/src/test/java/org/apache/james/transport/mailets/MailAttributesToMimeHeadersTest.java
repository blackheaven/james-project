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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.mail.MessagingException;

import org.apache.james.core.builder.MimeMessageBuilder;
import org.apache.mailet.Attribute;
import org.apache.mailet.AttributeName;
import org.apache.mailet.AttributeValue;
import org.apache.mailet.Mailet;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.FakeMailetConfig;
import org.apache.mailet.base.test.MailUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MailAttributesToMimeHeadersTest {

    private Mailet mailet;

    private static final String HEADER_NAME1 = "JUNIT";
    private static final String HEADER_NAME2 = "JUNIT2";

    private static final String RAW_MAIL_ATTRIBUTE_VALUE1 = "test1";
    private static final String RAW_MAIL_ATTRIBUTE_VALUE2 = "test2";
    private static final AttributeValue<String> MAIL_ATTRIBUTE_VALUE1 = AttributeValue.of(RAW_MAIL_ATTRIBUTE_VALUE1);
    private static final AttributeValue<String> MAIL_ATTRIBUTE_VALUE2 = AttributeValue.of(RAW_MAIL_ATTRIBUTE_VALUE2);

    private static final AttributeName MAIL_ATTRIBUTE_NAME1 = AttributeName.of("org.apache.james.test");
    private static final AttributeName MAIL_ATTRIBUTE_NAME2 = AttributeName.of("org.apache.james.test2");

    @BeforeEach
    void setup() {
        mailet = new MailAttributesToMimeHeaders();
    }

    @Test
    void shouldThrowMessagingExceptionIfMappingIsNotGiven() {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
            .mailetName("Test")
            .build();

        assertThatThrownBy(() -> mailet.init(mailetConfig))
            .isInstanceOf(MessagingException.class);
    }

    @Test
    void shouldThrowMessagingExceptionIfMappingIsEmpty() {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
            .mailetName("Test")
            .setProperty("simplemmapping", "")
            .build();

        assertThatThrownBy(() -> mailet.init(mailetConfig))
            .isInstanceOf(MessagingException.class);
    }

    @Test
    void shouldIgnoreAttributeOfMappingThatDoesNotExistOnTheMessage() throws MessagingException {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
            .mailetName("Test")
            .setProperty("simplemapping",
                MAIL_ATTRIBUTE_NAME1 + "; " + HEADER_NAME1 +
                    "," + MAIL_ATTRIBUTE_NAME2 + "; " + HEADER_NAME2 +
                    "," + "another.attribute" + "; " + "Another-Header")
            .build();

        mailet.init(mailetConfig);

        FakeMail mockedMail = MailUtil.createMockMail2Recipients(MailUtil.createMimeMessage());
        mockedMail.setAttribute(new Attribute(MAIL_ATTRIBUTE_NAME1, MAIL_ATTRIBUTE_VALUE1));
        mockedMail.setAttribute(new Attribute(MAIL_ATTRIBUTE_NAME2, MAIL_ATTRIBUTE_VALUE2));

        mailet.service(mockedMail);
        assertThat(mockedMail.getMessage().getHeader("another.attribute")).isNull();
    }

    @Test
    void shouldWorkWithMappingWithASingleBinding() throws MessagingException {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
            .mailetName("Test")
            .setProperty("simplemapping",
                MAIL_ATTRIBUTE_NAME1.asString() + "; " + HEADER_NAME1)
            .build();

        mailet.init(mailetConfig);

        FakeMail mockedMail = MailUtil.createMockMail2Recipients(MailUtil.createMimeMessage());
        mockedMail.setAttribute(new Attribute(MAIL_ATTRIBUTE_NAME1, MAIL_ATTRIBUTE_VALUE1));

        mailet.service(mockedMail);
        assertThat(mockedMail.getMessage().getHeader(HEADER_NAME1)).containsExactly(RAW_MAIL_ATTRIBUTE_VALUE1);
    }

    @Test
    void shouldPutAttributesIntoHeadersWhenMappingDefined() throws MessagingException {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .setProperty("simplemapping", 
                        MAIL_ATTRIBUTE_NAME1.asString() + "; " + HEADER_NAME1 +
                        "," + MAIL_ATTRIBUTE_NAME2.asString() + "; " + HEADER_NAME2 + 
                        "," + "another.attribute" + "; " + "Another-Header")
                .build();
        mailet.init(mailetConfig);
        
        FakeMail mockedMail = MailUtil.createMockMail2Recipients(MailUtil.createMimeMessage());
        mockedMail.setAttribute(new Attribute(MAIL_ATTRIBUTE_NAME1, MAIL_ATTRIBUTE_VALUE1));
        mockedMail.setAttribute(new Attribute(MAIL_ATTRIBUTE_NAME2, MAIL_ATTRIBUTE_VALUE2));
        mockedMail.setAttribute(new Attribute(AttributeName.of("unmatched.attribute"), AttributeValue.of("value")));

        mailet.service(mockedMail);

        assertThat(mockedMail.getMessage().getHeader(HEADER_NAME1)).containsExactly(RAW_MAIL_ATTRIBUTE_VALUE1);
        assertThat(mockedMail.getMessage().getHeader(HEADER_NAME2)).containsExactly(RAW_MAIL_ATTRIBUTE_VALUE2);
    }

    @Test
    void shouldAddAttributeIntoHeadersWhenHeaderAlreadyPresent() throws MessagingException {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .setProperty("simplemapping", MAIL_ATTRIBUTE_NAME1.asString() + "; " + HEADER_NAME1)
                .build();
        mailet.init(mailetConfig);

        FakeMail mockedMail = MailUtil.createMockMail2Recipients(MimeMessageBuilder.mimeMessageBuilder()
            .addHeader(HEADER_NAME1, "first value")
            .build());
        mockedMail.setAttribute(new Attribute(MAIL_ATTRIBUTE_NAME1, MAIL_ATTRIBUTE_VALUE1));
        
        mailet.service(mockedMail);

        assertThat(mockedMail.getMessage().getHeader(HEADER_NAME1)).containsExactly("first value", RAW_MAIL_ATTRIBUTE_VALUE1);
    }

    
    @Test
    void shouldThrowAtInitWhenNoSemicolumnInConfigurationEntry() {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .setProperty("simplemapping", "invalidConfigEntry")
                .build();
        assertThatThrownBy(() -> mailet.init(mailetConfig))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowAtInitWhenTwoSemicolumnsInConfigurationEntry() {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .setProperty("simplemapping", "first;second;third")
                .build();
        assertThatThrownBy(() -> mailet.init(mailetConfig))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowAtInitWhenNoConfigurationEntry() {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .build();
        assertThatThrownBy(() -> mailet.init(mailetConfig))
            .isInstanceOf(MessagingException.class);
    }
}
