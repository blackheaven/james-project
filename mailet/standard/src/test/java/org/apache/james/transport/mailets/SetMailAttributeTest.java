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

import javax.mail.MessagingException;

import org.apache.james.util.MimeMessageUtil;
import org.apache.mailet.Attribute;
import org.apache.mailet.AttributeName;
import org.apache.mailet.AttributeUtils;
import org.apache.mailet.AttributeValue;
import org.apache.mailet.Mail;
import org.apache.mailet.Mailet;
import org.apache.mailet.base.test.FakeMailetConfig;
import org.apache.mailet.base.test.MailUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SetMailAttributeTest {

    private Mailet mailet;

    @BeforeEach
    void setupMailet() {
        mailet = new SetMailAttribute();
    }

    @Test
    void shouldAddConfiguredAttributes() throws MessagingException {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .setProperty("org.apache.james.junit1", "true")
                .setProperty("org.apache.james.junit2", "happy")
                .build();

        mailet.init(mailetConfig);

        Mail mail = MailUtil.createMockMail2Recipients(MimeMessageUtil.defaultMimeMessage());
        
        mailet.service(mail);

        assertThat(AttributeUtils.getValueAndCastFromMail(mail, AttributeName.of("org.apache.james.junit1"), String.class))
            .isEqualTo("true");
        assertThat(AttributeUtils.getValueAndCastFromMail(mail, AttributeName.of("org.apache.james.junit2"), String.class))
            .isEqualTo("happy");
    }
    
    @Test
    void shouldAddNothingWhenNoConfiguredAttribute() throws MessagingException {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .build();
     
        mailet.init(mailetConfig);

        Mail mail = MailUtil.createMockMail2Recipients(MimeMessageUtil.defaultMimeMessage());
        
        mailet.service(mail);

        assertThat(mail.attributeNames()).isEmpty();
    }
    
    @Test
    void shouldOverwriteAttributeWhenAttributeAlreadyPresent() throws MessagingException {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .setProperty("org.apache.james.junit1", "bar")
                .build();
        
        mailet.init(mailetConfig);
        
        Mail mail = MailUtil.createMockMail2Recipients(MimeMessageUtil.defaultMimeMessage());
        mail.setAttribute(new Attribute(AttributeName.of("org.apache.james.junit1"), AttributeValue.of("foo")));
        
        mailet.service(mail);

        assertThat(AttributeUtils.getValueAndCastFromMail(mail, AttributeName.of("org.apache.james.junit1"), String.class))
            .isEqualTo("bar");
    }
}
