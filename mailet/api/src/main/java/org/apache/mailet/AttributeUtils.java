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

import java.util.Optional;

/** 
 * Attribute
 * 
 * @since Mailet API v3.2
 */
public class AttributeUtils {
    /**
     * Returns the raw attribute value corresponding to an attribute name.
     *
     * A cast will be attempted to match the type expected by the user.
     *
     * Returns an empty optional upon missing attribute or type mismatch.
     */
    public static <T> Optional<T> getValueAndCastFromMail(Mail mail, AttributeName name) {
        return getAttributeValueFromMail(mail, name)
                .flatMap(AttributeUtils::tryToCast);
    }


    /**
     * Returns the raw attribute value corresponding to an attribute name.
     *
     * Returns an empty optional upon missing attribute
     */
    public static Optional<?> getAttributeValueFromMail(Mail mail, AttributeName name) {
        return mail
                .getAttribute(name)
                .map(AttributeUtils::getAttributeValue);
    }

    /**
     * Returns the raw value of an Attribute
     */
    public static Object getAttributeValue(Attribute attribute) {
        return attribute.getValue().getValue();
    }

    private static <T> Optional<T> tryToCast(Object value) {
        try {
            return Optional.of((T)value);
        } catch (ClassCastException e) {
            return Optional.empty();
        }
    }
}
