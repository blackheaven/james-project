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
package org.apache.james.queue.api;

import org.apache.mailet.AttributeName;
import org.apache.mailet.AttributeValue;

/**
 * Supports Mail Priority handling
 */
public interface MailPrioritySupport {

    /** Handle mail with lowest priority */
    Integer LOW_PRIORITY = 0;
    AttributeValue<Integer> LOW_PRIORITY_ATTRIBUTE_VALUE = AttributeValue.of(LOW_PRIORITY);

    /** Handle mail with normal priority (this is the default) */
    Integer NORMAL_PRIORITY = 5;
    AttributeValue<Integer> NORMAL_PRIORITY_ATTRIBUTE_VALUE = AttributeValue.of(NORMAL_PRIORITY);

    /** Handle mail with highest priority */
    Integer HIGH_PRIORITY = 9;
    AttributeValue<Integer> HIGH_PRIORITY_ATTRIBUTE_VALUE = AttributeValue.of(HIGH_PRIORITY);

    /**
     * Attribute name for support if priority. If the attribute is set and
     * priority handling is enabled it will take care of move the Mails with
     * higher priority to the head of the queue (so the mails are faster
     * handled).
     */
    String MAIL_PRIORITY = "MAIL_PRIORITY";
    AttributeName MAIL_PRIORITY_ATTRIBUTE_NAME = AttributeName.of(MAIL_PRIORITY);
}
