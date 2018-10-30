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

package org.apache.james.jmap.send;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.mailbox.model.TestMessageId;
import org.apache.james.queue.api.MailQueue;
import org.apache.james.queue.api.MailQueue.MailQueueItem;
import org.apache.james.queue.api.MailQueueFactory;
import org.apache.james.queue.api.RawMailQueueItemDecoratorFactory;
import org.apache.james.queue.memory.MemoryMailQueueFactory;
import org.apache.mailet.AttributeUtils;
import org.apache.mailet.base.test.FakeMail;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class MailSpoolTest {
    private static final String USERNAME = "user";
    private static final TestMessageId MESSAGE_ID = TestMessageId.of(1);
    private static final String NAME = "Name";

    private MailSpool mailSpool;
    private MailQueue myQueue;

    @Before
    public void setup() {
        MemoryMailQueueFactory mailQueueFactory = new MemoryMailQueueFactory(new RawMailQueueItemDecoratorFactory());
        myQueue = mailQueueFactory.createQueue(MailQueueFactory.SPOOL);

        mailSpool = new MailSpool(mailQueueFactory);
    }

    @Ignore("FIXME: AttributeValue.of(MessageId)")
    @Test
    public void sendShouldEnQueueTheMail() throws Exception {
        FakeMail mail = FakeMail.builder()
            .name(NAME)
            .build();

        mailSpool.send(mail, new MailMetadata(MESSAGE_ID, USERNAME));

        MailQueueItem actual = myQueue.deQueue();
        assertThat(actual.getMail().getName()).isEqualTo(NAME);
    }

    @Ignore("FIXME: AttributeValue.of(MessageId)")
    @Test
    public void sendShouldPositionJMAPRelatedMetadata() throws Exception {
        FakeMail mail = FakeMail.builder()
            .name(NAME)
            .build();

        mailSpool.send(mail, new MailMetadata(MESSAGE_ID, USERNAME));

        MailQueueItem actual = myQueue.deQueue();
        assertThat(AttributeUtils.getValueAndCastFromMail(actual.getMail(), MailMetadata.MAIL_METADATA_USERNAME_ATTRIBUTE, String.class))
            .isEqualTo(USERNAME);
        assertThat(AttributeUtils.getValueAndCastFromMail(actual.getMail(), MailMetadata.MAIL_METADATA_MESSAGE_ID_ATTRIBUTE, TestMessageIdDtop.class))
            .isEqualTo(MESSAGE_ID);
    }

}
