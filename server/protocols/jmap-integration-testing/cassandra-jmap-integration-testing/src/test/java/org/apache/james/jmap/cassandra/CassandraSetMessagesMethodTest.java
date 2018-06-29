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

package org.apache.james.jmap.cassandra;

import static com.jayway.restassured.RestAssured.given;
import static org.apache.james.jmap.TestingConstants.ARGUMENTS;
import static org.apache.james.jmap.TestingConstants.NAME;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.mail.Flags;

import org.apache.james.CassandraJmapTestRule;
import org.apache.james.DockerCassandraRule;
import org.apache.james.GuiceJamesServer;
import org.apache.james.backends.cassandra.ContainerLifecycleConfiguration;
import org.apache.james.jmap.methods.integration.SetMessagesMethodTest;
import org.apache.james.mailbox.DefaultMailboxes;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.MessageManager.AppendCommand;
import org.apache.james.mailbox.cassandra.ids.CassandraMessageId;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.modules.MailboxProbeImpl;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.github.fge.lambdas.Throwing;

public class CassandraSetMessagesMethodTest extends SetMessagesMethodTest {
    private static final Integer NUMBER_OF_MAIL_TO_CREATE = 250;

    @ClassRule
    public static DockerCassandraRule cassandra = new DockerCassandraRule();

    public static ContainerLifecycleConfiguration cassandraLifecycleConfiguration = ContainerLifecycleConfiguration.withDefaultIterationsBetweenRestart().container(cassandra.getRawContainer()).build();

    @Rule
    public CassandraJmapTestRule rule = CassandraJmapTestRule.defaultTestRule();

    @Rule
    public TestRule cassandraLifecycleTestRule = cassandraLifecycleConfiguration.asTestRule();

    @Override
    protected GuiceJamesServer createJmapServer() throws IOException {
        return rule.jmapServer(cassandra.getModule());
    }

    @Override
    protected void await() {
        rule.await();
    }
    
    @Override
    protected MessageId randomMessageId() {
        return new CassandraMessageId.Factory().generate();
    }

    @Ignore("JAMES-2221 Temporally ignored failed test")
    @Override
    @Test
    public void attachmentsShouldBeRetrievedWhenChainingSetMessagesAndGetMessagesTextAttachment() throws Exception {

    }

    @Test
    public void setMessagesShouldWorkForHugeNumberOfEmailsToTrashWhenChunksConfigurationAreLowEnough() throws Throwable {
        teardown();
        cassandra = new DockerCassandraRule(configuration -> configuration.addProperty("chunk.size.expunge", 85));
        setup();
        try {
            MailboxPath mailboxPath = MailboxPath.forUser(USERNAME, DefaultMailboxes.TRASH);
            ZonedDateTime dateTime = ZonedDateTime.parse("2014-10-30T14:12:00Z");
            AppendCommand command = MessageManager.AppendCommand.builder()
                .notRecent()
                .withFlags(new Flags())
                .build(Message.Builder
                    .of()
                    .setSubject("my test subject")
                    .setBody("testmail", StandardCharsets.UTF_8)
                    .setDate(Date.from(dateTime.toInstant())));

            String mailIds = IntStream
                .rangeClosed(1, NUMBER_OF_MAIL_TO_CREATE)
                .mapToObj(Throwing.intFunction(i ->
                    ((MailboxProbeImpl) mailboxProbe)
                        .appendMessage(USERNAME, mailboxPath, command)
                        .getMessageId()
                        .serialize()).sneakyThrow())
                .map(id -> '"' + id + '"')
                .collect(Collectors.joining(","));

            given()
                .header("Authorization", accessToken.serialize())
                .body("[[\"setMessages\", {\"destroy\": [\"" + mailIds + "\"]}, \"#0\"]]")
            .when()
                .post("/jmap")
            .then()
                .statusCode(200)
                .log().ifValidationFails()
                .body(NAME, equalTo("messagesSet"))
                .body(ARGUMENTS + ".destroyed", hasSize(NUMBER_OF_MAIL_TO_CREATE));
        } finally {
            teardown();
            cassandra = new DockerCassandraRule();
            setup();
        }
    }

    @Test
    public void setMessagesShouldFailForHugeNumberOfEmailsToTrashWhenChunksConfigurationAreTooBig() throws Throwable {
        teardown();
        cassandra = new DockerCassandraRule(configuration -> configuration.addProperty("chunk.size.expunge", NUMBER_OF_MAIL_TO_CREATE));
        setup();
        try {
            MailboxPath mailboxPath = MailboxPath.forUser(USERNAME, DefaultMailboxes.TRASH);
            ZonedDateTime dateTime = ZonedDateTime.parse("2014-10-30T14:12:00Z");
            AppendCommand command = MessageManager.AppendCommand.builder()
                .notRecent()
                .withFlags(new Flags())
                .build(Message.Builder
                    .of()
                    .setSubject("my test subject")
                    .setBody("testmail", StandardCharsets.UTF_8)
                    .setDate(Date.from(dateTime.toInstant())));

            String mailIds = IntStream
                .rangeClosed(1, NUMBER_OF_MAIL_TO_CREATE)
                .mapToObj(Throwing.intFunction(i ->
                    ((MailboxProbeImpl) mailboxProbe)
                        .appendMessage(USERNAME, mailboxPath, command)
                        .getMessageId()
                        .serialize()).sneakyThrow())
                .map(id -> '"' + id + '"')
                .collect(Collectors.joining(","));

            given()
                .header("Authorization", accessToken.serialize())
                .body("[[\"setMessages\", {\"destroy\": [\"" + mailIds + "\"]}, \"#0\"]]")
            .when()
                .post("/jmap")
            .then()
                .statusCode(400);
        } finally {
            teardown();
            cassandra = new DockerCassandraRule();
            setup();
        }
    }
}
