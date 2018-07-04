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

package org.apache.james.jmap.methods.integration;

import static com.jayway.restassured.RestAssured.given;
import static org.apache.james.jmap.HttpJmapAuthentication.authenticateJamesUser;
import static org.apache.james.jmap.JmapURIBuilder.baseUri;
import static org.apache.james.jmap.TestingConstants.ALICE;
import static org.apache.james.jmap.TestingConstants.ALICE_PASSWORD;
import static org.apache.james.jmap.TestingConstants.ARGUMENTS;
import static org.apache.james.jmap.TestingConstants.DOMAIN;
import static org.apache.james.jmap.TestingConstants.NAME;
import static org.apache.james.jmap.TestingConstants.jmapRequestSpecBuilder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsNull.nullValue;

import java.io.IOException;
import java.time.ZonedDateTime;

import org.apache.james.GuiceJamesServer;
import org.apache.james.jmap.FixedDateZonedDateTimeProvider;
import org.apache.james.jmap.api.access.AccessToken;
import org.apache.james.jmap.api.vacation.AccountId;
import org.apache.james.jmap.api.vacation.VacationPatch;
import org.apache.james.probe.DataProbe;
import org.apache.james.util.date.ZonedDateTimeProvider;
import org.apache.james.utils.DataProbeImpl;
import org.apache.james.utils.JmapGuiceProbe;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

public abstract class GetVacationResponseTest {
    public static final String SUBJECT = "subject";
    public static final ZonedDateTime DATE_2015 = ZonedDateTime.parse("2015-10-15T14:10:00Z");
    public static final ZonedDateTime DATE_2014 = ZonedDateTime.parse("2014-09-30T14:10:00+02:00");
    public static final ZonedDateTime DATE_2016 = ZonedDateTime.parse("2016-04-15T11:56:32.224+07:00[Asia/Vientiane]");
    private JmapGuiceProbe jmapGuiceProbe;

    protected abstract GuiceJamesServer createJmapServer(ZonedDateTimeProvider zonedDateTimeProvider) throws IOException;

    protected abstract void await();

    private AccessToken accessToken;
    private GuiceJamesServer jmapServer;
    private FixedDateZonedDateTimeProvider fixedDateZonedDateTimeProvider;

    @Before
    public void setup() throws Throwable {
        fixedDateZonedDateTimeProvider = new FixedDateZonedDateTimeProvider();
        fixedDateZonedDateTimeProvider.setFixedDateTime(DATE_2015);

        jmapServer = createJmapServer(fixedDateZonedDateTimeProvider);
        jmapServer.start();

        jmapGuiceProbe = jmapServer.getProbe(JmapGuiceProbe.class);
        RestAssured.requestSpecification = jmapRequestSpecBuilder
                .setPort(jmapGuiceProbe.getJmapPort())
                .build();

        DataProbe dataProbe = jmapServer.getProbe(DataProbeImpl.class);
        dataProbe.addDomain(DOMAIN);
        dataProbe.addUser(ALICE, ALICE_PASSWORD);
        accessToken = authenticateJamesUser(baseUri(jmapServer), ALICE, ALICE_PASSWORD);

        await();
    }

    @After
    public void teardown() {
        jmapServer.stop();
    }

    @Test
    public void getVacationResponseShouldReturnDefaultValue() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "\"getVacationResponse\", " +
                "{}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("vacationResponse"))
            .body(ARGUMENTS + ".accountId", equalTo(ALICE))
            .body(ARGUMENTS + ".list", hasSize(1))
            .body(ARGUMENTS + ".list[0].id", equalTo("singleton"))
            .body(ARGUMENTS + ".list[0].fromDate", nullValue())
            .body(ARGUMENTS + ".list[0].toDate", nullValue())
            .body(ARGUMENTS + ".list[0].isEnabled", equalTo(false))
            .body(ARGUMENTS + ".list[0].subject", nullValue())
            .body(ARGUMENTS + ".list[0].textBody", nullValue())
            .body(ARGUMENTS + ".list[0].htmlBody", nullValue());
    }

    @Test
    public void getVacationResponseShouldReturnStoredValue() {
        jmapGuiceProbe.modifyVacation(AccountId.fromString(ALICE),
            VacationPatch.builder()
                .isEnabled(true)
                .fromDate(ZonedDateTime.parse("2014-09-30T14:10:00Z"))
                .toDate(ZonedDateTime.parse("2014-10-30T14:10:00Z"))
                .textBody("Test explaining my vacations")
                .subject(SUBJECT)
                .htmlBody("<p>Test explaining my vacations</p>")
                .build());

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "\"getVacationResponse\", " +
                "{}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("vacationResponse"))
            .body(ARGUMENTS + ".accountId", equalTo(ALICE))
            .body(ARGUMENTS + ".list", hasSize(1))
            .body(ARGUMENTS + ".list[0].id", equalTo("singleton"))
            .body(ARGUMENTS + ".list[0].fromDate", equalTo("2014-09-30T14:10:00Z"))
            .body(ARGUMENTS + ".list[0].toDate", equalTo("2014-10-30T14:10:00Z"))
            .body(ARGUMENTS + ".list[0].isEnabled", equalTo(true))
            .body(ARGUMENTS + ".list[0].textBody", equalTo("Test explaining my vacations"))
            .body(ARGUMENTS + ".list[0].subject", equalTo(SUBJECT))
            .body(ARGUMENTS + ".list[0].htmlBody", equalTo("<p>Test explaining my vacations</p>"));
    }

    @Test
    public void getVacationResponseShouldReturnStoredValueWithNonDefaultTimezone() {
        jmapGuiceProbe.modifyVacation(AccountId.fromString(ALICE),
            VacationPatch.builder()
                .isEnabled(true)
                .fromDate(ZonedDateTime.parse("2014-09-30T14:10:00+02:00"))
                .toDate(ZonedDateTime.parse("2016-04-15T11:56:32.224+07:00[Asia/Vientiane]"))
                .textBody("Test explaining my vacations")
                .build());

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "\"getVacationResponse\", " +
                "{}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("vacationResponse"))
            .body(ARGUMENTS + ".accountId", equalTo(ALICE))
            .body(ARGUMENTS + ".list", hasSize(1))
            .body(ARGUMENTS + ".list[0].id", equalTo("singleton"))
            .body(ARGUMENTS + ".list[0].fromDate", equalTo("2014-09-30T14:10:00+02:00"))
            .body(ARGUMENTS + ".list[0].toDate", equalTo("2016-04-15T11:56:32.224+07:00[Asia/Vientiane]"))
            .body(ARGUMENTS + ".list[0].isEnabled", equalTo(true))
            .body(ARGUMENTS + ".list[0].textBody", equalTo("Test explaining my vacations"));
    }

    @Test
    public void getVacationResponseShouldReturnIsActivatedWhenInRange() {
        jmapGuiceProbe.modifyVacation(AccountId.fromString(ALICE),
            VacationPatch.builder()
                .isEnabled(true)
                .fromDate(DATE_2014)
                .toDate(DATE_2016)
                .textBody("Test explaining my vacations")
                .build());

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                    "\"getVacationResponse\", " +
                    "{}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("vacationResponse"))
            .body(ARGUMENTS + ".list[0].isActivated", equalTo(true));
    }

    @Test
    public void getVacationResponseShouldNotReturnIsActivatedWhenOutOfRange() {
        fixedDateZonedDateTimeProvider.setFixedDateTime(DATE_2014);

        jmapGuiceProbe.modifyVacation(AccountId.fromString(ALICE),
            VacationPatch.builder()
                .isEnabled(true)
                .fromDate(DATE_2015)
                .toDate(DATE_2016)
                .textBody("Test explaining my vacations")
                .build());

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                    "\"getVacationResponse\", " +
                    "{}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("vacationResponse"))
            .body(ARGUMENTS + ".list[0].isActivated", equalTo(false));
    }

    @Test
    public void accountIdIsNotSupported() {
        jmapGuiceProbe.modifyVacation(AccountId.fromString(ALICE),
            VacationPatch.builder()
                .isEnabled(true)
                .fromDate(ZonedDateTime.parse("2014-09-30T14:10:00+02:00"))
                .toDate(ZonedDateTime.parse("2014-10-30T14:10:00+02:00"))
                .textBody("Test explaining my vacations")
                .build());

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "\"getVacationResponse\", " +
                "{\"accountId\":\"1\"}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("error"))
            .body(ARGUMENTS + ".type", equalTo("invalidArguments"))
            .body(ARGUMENTS + ".description", equalTo("The field 'accountId' of 'GetVacationRequest' is not supported"));
    }

}
