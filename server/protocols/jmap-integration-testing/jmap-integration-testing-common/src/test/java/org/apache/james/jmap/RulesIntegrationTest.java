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

package org.apache.james.jmap;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.with;
import static org.apache.james.jmap.HttpJmapAuthentication.authenticateJamesUser;
import static org.apache.james.jmap.JmapURIBuilder.baseUri;
import static org.apache.james.jmap.TestingConstants.ARGUMENTS;
import static org.apache.james.jmap.TestingConstants.DOMAIN;
import static org.apache.james.jmap.TestingConstants.SECOND_ARGUMENTS;
import static org.apache.james.jmap.TestingConstants.SECOND_NAME;
import static org.apache.james.jmap.TestingConstants.calmlyAwait;
import static org.apache.james.jmap.TestingConstants.jmapRequestSpecBuilder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.james.GuiceJamesServer;
import org.apache.james.jmap.api.access.AccessToken;
import org.apache.james.mailbox.DefaultMailboxes;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.store.probe.MailboxProbe;
import org.apache.james.modules.MailboxProbeImpl;
import org.apache.james.probe.DataProbe;
import org.apache.james.utils.DataProbeImpl;
import org.apache.james.utils.JmapGuiceProbe;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.restassured.RestAssured;

public abstract class RulesIntegrationTest {

    private static final String USER_1 = "orpheus@" + DOMAIN;
    private static final String USER_2 = "eurydice@" + DOMAIN;
    private static final String PASSWORD = "secret";
    private static final String USER1_FILTERED_MAILBOX = "filtered";
    private static final String MATCHING_SUBJECT = "The path is complicated";
    private static final String NOT_MATCHING_SUBJECT = "The path is simple";
    public static final String ORIGINAL_MESSAGE_TEXT_BODY = "I have your back";

    private GuiceJamesServer guiceJamesServer;
    private JmapGuiceProbe jmapGuiceProbe;

    protected abstract GuiceJamesServer createJmapServer() throws IOException;

    protected abstract void await();

    @Before
    public void setUp() throws Exception {
        guiceJamesServer = createJmapServer();
        guiceJamesServer.start();

        DataProbe dataProbe = guiceJamesServer.getProbe(DataProbeImpl.class);
        dataProbe.addDomain(DOMAIN);
        dataProbe.addUser(USER_1, PASSWORD);
        dataProbe.addUser(USER_2, PASSWORD);
        MailboxProbe mailboxProbe = guiceJamesServer.getProbe(MailboxProbeImpl.class);
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, USER_2, DefaultMailboxes.OUTBOX);
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, USER_1, DefaultMailboxes.SENT);
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, USER_2, DefaultMailboxes.SENT);
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, USER_1, DefaultMailboxes.INBOX);
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, USER_1, USER1_FILTERED_MAILBOX);
        await();

        jmapGuiceProbe = guiceJamesServer.getProbe(JmapGuiceProbe.class);
        RestAssured.requestSpecification = jmapRequestSpecBuilder
            .setPort(jmapGuiceProbe.getJmapPort())
            .build();
    }

    @After
    public void teardown() {
        guiceJamesServer.stop();
    }

    @Test
    public void jmapRuleShouldFilterAMatchingMailWhenSet() throws Exception {
        /* Test scenario :
            - User 1 orpheus@mydomain.tld sets a rule set
            - User 2 eurydice@mydomain.tld sends User 1 a mail matching the Rule
            - User 1 should well receive this mail in the specified mailbox
        */

        // Given
        AccessToken user1AccessToken = authenticateJamesUser(baseUri(guiceJamesServer), USER_1, PASSWORD);
        AccessToken user2AccessToken = authenticateJamesUser(baseUri(guiceJamesServer), USER_2, PASSWORD);
        // User 1 orpheus@mydomain.tld sets a rule set
        setEffectiveRule(user1AccessToken);

        // When
        // User 2 eurydice@mydomain.tld sends User 1 a mail
        sendMatchingMail(user2AccessToken, getOutboxId(user2AccessToken), "user|inbox|1");

        // Then
        // User 1 should well receive this mail in the filtered mailbox
        calmlyAwait.atMost(30, TimeUnit.SECONDS)
            .until(() -> isTextMessageReceived(user1AccessToken, getFilteredMailboxId(user1AccessToken), ORIGINAL_MESSAGE_TEXT_BODY, USER_2, USER_1));
    }

    @Test
    public void jmapRuleShouldNotFilterAMatchingMailWhenSet() throws Exception {
        /* Test scenario :
            - User 1 orpheus@mydomain.tld sets a rule set
            - User 2 eurydice@mydomain.tld sends User 1 a mail not matching the Rule
            - User 1 should well receive this mail in the inbox mailbox
        */

        // Given
        AccessToken user1AccessToken = authenticateJamesUser(baseUri(guiceJamesServer), USER_1, PASSWORD);
        AccessToken user2AccessToken = authenticateJamesUser(baseUri(guiceJamesServer), USER_2, PASSWORD);
        // User 1 orpheus@mydomain.tld sets a rule set
        setEffectiveRule(user1AccessToken);

        // When
        // User 2 eurydice@mydomain.tld sends User 1 a mail
        sendNotMatchingMail(user2AccessToken, getOutboxId(user2AccessToken), "user|inbox|1");

        // Then
        // User 1 should well receive this mail in the filtered mailbox
        calmlyAwait.atMost(30, TimeUnit.SECONDS)
            .until(() -> isTextMessageReceived(user1AccessToken, getInboxId(user1AccessToken), ORIGINAL_MESSAGE_TEXT_BODY, USER_2, USER_1));
    }

    @Test
    public void jmapRuleShouldNotFilterAMailWhenNotSet() throws Exception {
        /* Test scenario :
            - User 2 eurydice@mydomain.tld sends User 1 a mail
            - User 1 should well receive this mail in the inbox mailbox
        */

        // Given
        AccessToken user1AccessToken = authenticateJamesUser(baseUri(guiceJamesServer), USER_1, PASSWORD);
        AccessToken user2AccessToken = authenticateJamesUser(baseUri(guiceJamesServer), USER_2, PASSWORD);

        // When
        // User 2 eurydice@mydomain.tld sends User 1 a mail
        sendMatchingMail(user2AccessToken, getOutboxId(user2AccessToken), "user|inbox|1");

        // Then
        // User 1 should well receive this mail in the filtered mailbox
        calmlyAwait.atMost(30, TimeUnit.SECONDS)
            .until(() -> isTextMessageReceived(user1AccessToken, getFilteredMailboxId(user1AccessToken), ORIGINAL_MESSAGE_TEXT_BODY, USER_2, USER_1));
    }

    @Test
    public void jmapRuleShouldNotFilterAMailWhenSetEmpty() throws Exception {
        /* Test scenario :
            - User 1 orpheus@mydomain.tld sets an empty rule set
            - User 2 eurydice@mydomain.tld sends User 1 a mail
            - User 1 should well receive this mail in the inbox mailbox
        */

        // Given
        AccessToken user1AccessToken = authenticateJamesUser(baseUri(guiceJamesServer), USER_1, PASSWORD);
        AccessToken user2AccessToken = authenticateJamesUser(baseUri(guiceJamesServer), USER_2, PASSWORD);
        // User 1 orpheus@mydomain.tld sets an empty rule set
        setEmptyRule(user1AccessToken);

        // When
        // User 2 eurydice@mydomain.tld sends User 1 a mail
        sendMatchingMail(user2AccessToken, getOutboxId(user2AccessToken), "user|inbox|1");

        // Then
        // User 1 should well receive this mail in the filtered mailbox
        calmlyAwait.atMost(30, TimeUnit.SECONDS)
            .until(() -> isTextMessageReceived(user1AccessToken, getFilteredMailboxId(user1AccessToken), ORIGINAL_MESSAGE_TEXT_BODY, USER_2, USER_1));
    }

    @Test
    public void jmapRuleShouldFilterAMatchingMailWhenSetEmptyThenEffectively() throws Exception {
        /* Test scenario :
            - User 1 orpheus@mydomain.tld sets an empty rule set
            - User 1 orpheus@mydomain.tld sets a rule set
            - User 2 eurydice@mydomain.tld sends User 1 a mail matching the Rule
            - User 1 should well receive this mail in the specified mailbox
        */

        // Given
        AccessToken user1AccessToken = authenticateJamesUser(baseUri(guiceJamesServer), USER_1, PASSWORD);
        AccessToken user2AccessToken = authenticateJamesUser(baseUri(guiceJamesServer), USER_2, PASSWORD);
        // User 1 orpheus@mydomain.tld sets an empty rule set
        setEmptyRule(user1AccessToken);
        // User 1 orpheus@mydomain.tld sets a rule set
        setEffectiveRule(user1AccessToken);

        // When
        // User 2 eurydice@mydomain.tld sends User 1 a mail
        sendMatchingMail(user2AccessToken, getOutboxId(user2AccessToken), "user|inbox|1");

        // Then
        // User 1 should well receive this mail in the filtered mailbox
        calmlyAwait.atMost(30, TimeUnit.SECONDS)
            .until(() -> isTextMessageReceived(user1AccessToken, getFilteredMailboxId(user1AccessToken), ORIGINAL_MESSAGE_TEXT_BODY, USER_2, USER_1));
    }

    private void setEffectiveRule(AccessToken user1AccessToken) {
        String bodyRequest = "[[" +
            "\"setFilter\", " +
            "{" +
            "  \"singleton\" : [{" +
            "    \"id\": \"42-ac\"," +
            "    \"name\": \"My first rule\"" +
            "    \"condition\": {" +
            "      \"field\": \"subject\"," +
            "      \"comparator\": \"exactly-equals\"" +
            "      \"value\": \"" + MATCHING_SUBJECT + "\"\n" +
            "    }," +
            "    \"action\": {\n" +
            "      \"mailboxIds\": [\"" + getFilteredMailboxId(user1AccessToken) + "\"]\n" +
            "    }" +
            "  }]" +
            "}, \"#0\"" +
            "]]";
        given()
            .header("Authorization", user1AccessToken.serialize())
            .body(bodyRequest)
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);
    }

    private void setEmptyRule(AccessToken user1AccessToken) {
        String bodyRequest = "[[" +
            "\"setFilter\", " +
            "{" +
            "  \"singleton\" : []" +
            "}, \"#0\"" +
            "]]";
        given()
            .header("Authorization", user1AccessToken.serialize())
            .body(bodyRequest)
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);
    }

    private void sendNotMatchingMail(AccessToken user2AccessToken, String outboxId, String mailId) {
        sendMail(user2AccessToken, outboxId, mailId, NOT_MATCHING_SUBJECT);
    }

    private void sendMatchingMail(AccessToken user2AccessToken, String outboxId, String mailId) {
        sendMail(user2AccessToken, outboxId, mailId, MATCHING_SUBJECT);
    }

    private void sendMail(AccessToken user2AccessToken, String outboxId, String mailId, String subject) {
        String requestBody = "[" +
            "  [" +
            "    \"setMessages\"," +
            "    {" +
            "      \"create\": { \"" + mailId + "\" : {" +
            "        \"from\": { \"email\": \"" + USER_2 + "\"}," +
            "        \"to\": [{ \"name\": \"Orpheus\", \"email\": \"" + USER_1 + "\"}]," +
            "        \"subject\": \"" + subject + "\"," +
            "        \"textBody\": \"" + ORIGINAL_MESSAGE_TEXT_BODY + "\"," +
            "        \"mailboxIds\": [\"" + outboxId + "\"]" +
            "      }}" +
            "    }," +
            "    \"#0\"" +
            "  ]" +
            "]";
        given()
            .header("Authorization", user2AccessToken.serialize())
            .body(requestBody)
        .when()
            .post("/jmap");
    }

    private boolean isTextMessageReceived(AccessToken recipientToken, String mailboxId, String expectedTextBody, String expectedFrom, String expectedTo) {
        try {
            assertOneMessageReceived(recipientToken, mailboxId, expectedTextBody, expectedFrom, expectedTo);
            return true;
        } catch (AssertionError e) {
            return false;
        }
    }

    private void assertOneMessageReceived(AccessToken recipientToken, String mailboxId, String expectedTextBody, String expectedFrom, String expectedTo) {
        with()
            .header("Authorization", recipientToken.serialize())
            .body("[[\"getMessageList\", " +
                "{" +
                "  \"fetchMessages\": true, " +
                "  \"fetchMessageProperties\": [\"textBody\", \"from\", \"to\", \"mailboxIds\"]," +
                "  \"filter\": {" +
                "    \"inMailboxes\":[\"" + mailboxId + "\"]" +
                "  }" +
                "}, \"#0\"]]")
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(SECOND_NAME, equalTo("messages"))
            .body(SECOND_ARGUMENTS + ".list", hasSize(1))
            .body(SECOND_ARGUMENTS + ".list[0].textBody", equalTo(expectedTextBody))
            .body(SECOND_ARGUMENTS + ".list[0].from.email", equalTo(expectedFrom))
            .body(SECOND_ARGUMENTS + ".list[0].to.email", hasSize(1))
            .body(SECOND_ARGUMENTS + ".list[0].to.email[0]", equalTo(expectedTo));
    }

    private String getOutboxId(AccessToken accessToken) {
        return getMailboxIdByRole(accessToken, DefaultMailboxes.OUTBOX);
    }

    private String getInboxId(AccessToken accessToken) {
        return getMailboxIdByRole(accessToken, DefaultMailboxes.INBOX);
    }

    private String getFilteredMailboxId(AccessToken accessToken) {
        return getMailboxIdByName(accessToken, USER1_FILTERED_MAILBOX);
    }


    private String getMailboxIdByRole(AccessToken accessToken, String role) {
        return getAllMailboxesIds(accessToken).stream()
            .filter(x -> x.get("role").equalsIgnoreCase(role))
            .map(x -> x.get("id"))
            .findFirst()
            .get();
    }

    private String getMailboxIdByName(AccessToken accessToken, String name) {
        return getAllMailboxesIds(accessToken).stream()
            .filter(x -> x.get("name").equalsIgnoreCase(name))
            .map(x -> x.get("id"))
            .findFirst()
            .get();
    }

    private List<Map<String, String>> getAllMailboxesIds(AccessToken accessToken) {
        return with()
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMailboxes\", {\"properties\": [\"name\", \"id\"]}, \"#0\"]]")
            .post("/jmap")
        .andReturn()
            .body()
            .jsonPath()
            .getList(ARGUMENTS + ".list");
    }
}
