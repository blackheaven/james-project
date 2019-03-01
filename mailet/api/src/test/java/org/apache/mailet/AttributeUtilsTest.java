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

package org.apache.mailet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class AttributeUtilsTest {
    @Test
    void getValueAndCastFromMailShouldCastValueWhenRightType() {
        Attribute attribute = Attribute.convertToAttribute("name", "value");
        Mail mail = mock(Mail.class);
        when(mail.getAttribute(attribute.getName())).thenReturn(Optional.of(attribute));

        assertThat(AttributeUtils.getValueAndCastFromMail(mail, attribute.getName(), String.class))
            .contains("value");
    }

    @Test
    void getValueAndCastFromMailShouldReturnEmptyWhenWrongType() {
        Attribute attribute = Attribute.convertToAttribute("name", "value");
        Mail mail = mock(Mail.class);
        when(mail.getAttribute(attribute.getName())).thenReturn(Optional.of(attribute));

        assertThat(AttributeUtils.getValueAndCastFromMail(mail, attribute.getName(), Boolean.class))
            .isEmpty();
    }



}