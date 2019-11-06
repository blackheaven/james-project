/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.james;

import static org.awaitility.Duration.ONE_HUNDRED_MILLISECONDS;

import org.apache.commons.lang3.StringUtils;
import org.apache.james.core.Domain;
import org.apache.james.core.builder.MimeMessageBuilder;
import org.apache.james.modules.protocols.ImapGuiceProbe;
import org.apache.james.modules.protocols.SmtpGuiceProbe;
import org.apache.james.server.core.MailImpl;
import org.apache.james.utils.DataProbeImpl;
import org.apache.james.utils.IMAPMessageReader;
import org.apache.james.utils.SMTPMessageSender;
import org.apache.james.utils.SpoolerProbe;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.Test;

interface MailsShouldBeWellReceived {

    String JAMES_SERVER_HOST = "127.0.0.1";
    String DOMAIN = "domain";
    String JAMES_USER = "james-user@" + DOMAIN;
    String PASSWORD = "secret";
    ConditionFactory CALMLY_AWAIT = Awaitility
        .with().pollInterval(ONE_HUNDRED_MILLISECONDS)
        .and().pollDelay(ONE_HUNDRED_MILLISECONDS)
        .await();

    @Test
    default void simpleMailShouldBeWellReceived(GuiceJamesServer server) throws Exception {
        server.getProbe(DataProbeImpl.class).fluent()
            .addDomain(DOMAIN)
            .addUser(JAMES_USER, PASSWORD);

        try (SMTPMessageSender sender = new SMTPMessageSender(Domain.LOCALHOST.asString())) {
            sender.connect(JAMES_SERVER_HOST, server.getProbe(SmtpGuiceProbe.class).getSmtpPort())
                .sendMessage("bob@any.com", JAMES_USER);
        }

        CALMLY_AWAIT.until(() -> server.getProbe(SpoolerProbe.class).processingFinished());

        try (IMAPMessageReader reader = new IMAPMessageReader()) {
            reader.connect(JAMES_SERVER_HOST, server.getProbe(ImapGuiceProbe.class).getImapPort())
                .login(JAMES_USER, PASSWORD)
                .select(IMAPMessageReader.INBOX)
                .awaitMessage(CALMLY_AWAIT);
        }
    }

    @Test
    default void hugeMailShouldBeWellReceived(GuiceJamesServer server) throws Exception {
        server.getProbe(DataProbeImpl.class).fluent()
            .addDomain(DOMAIN)
            .addUser(JAMES_USER, PASSWORD);

        try (SMTPMessageSender sender = new SMTPMessageSender(Domain.LOCALHOST.asString())) {
            String word = "word ";
            int bodySize = 1024 * 1024;
            String body = StringUtils.repeat(word, word.length()/bodySize);
            sender.connect(JAMES_SERVER_HOST, server.getProbe(SmtpGuiceProbe.class).getSmtpPort())
                .sendMessage(MailImpl.builder()
                    .name("big-mail")
                    .sender("bob@any.com")
                    .addRecipient(JAMES_USER)
                    .mimeMessage(MimeMessageBuilder.mimeMessageBuilder()
                        .setSubject("I have a big body")
                        .setText(body))
                    .build());
        }

        CALMLY_AWAIT.until(() -> server.getProbe(SpoolerProbe.class).processingFinished());

        try (IMAPMessageReader reader = new IMAPMessageReader()) {
            reader.connect(JAMES_SERVER_HOST, server.getProbe(ImapGuiceProbe.class).getImapPort())
                .login(JAMES_USER, PASSWORD)
                .select(IMAPMessageReader.INBOX)
                .awaitMessage(CALMLY_AWAIT);
        }
    }
}
