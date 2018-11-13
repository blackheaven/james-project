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

import java.io.Serializable;
import java.util.Map;
import javax.mail.MessagingException;

import org.apache.mailet.Attribute;
import org.apache.mailet.AttributeName;
import org.apache.mailet.AttributeUtils;
import org.apache.mailet.AttributeValue;
import org.apache.mailet.BytesArrayDto;
import org.apache.mailet.Mail;
import org.apache.mailet.MailetException;
import org.apache.mailet.base.GenericMailet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

/**
 * This mailet replace the mail attribute map of key to MimePart
 * by a map of key to the MimePart content (as bytes).
 * <br />
 * It takes only one parameter:
 * <ul>
 * <li>attribute (mandatory): mime content to be decoded, expected to be a Map&lt;String, byte[]&gt;
 * </ul>
 *
 * Then all this map attribute values will be replaced by their content.
 */
public class MimeDecodingMailet extends GenericMailet {
    private static final Logger LOGGER = LoggerFactory.getLogger(MimeDecodingMailet.class);

    public static final String ATTRIBUTE_PARAMETER_NAME = "attribute";

    private AttributeName attribute;

    @Override
    public void init() throws MessagingException {
        String attributeRaw = getInitParameter(ATTRIBUTE_PARAMETER_NAME);
        if (Strings.isNullOrEmpty(attributeRaw)) {
            throw new MailetException("No value for " + ATTRIBUTE_PARAMETER_NAME
                    + " parameter was provided.");
        }
        attribute = AttributeName.of(attributeRaw);
    }

    @Override
    public void service(Mail mail) throws MessagingException {
        if (!mail.getAttribute(attribute).isPresent()) {
            return;
        }

        ImmutableMap<String, AttributeValue<?>> extractedMimeContentByName = getAttributeContent(mail)
            .entrySet()
            .stream()
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        mail.setAttribute(new Attribute(attribute, AttributeValue.of(extractedMimeContentByName)));
    }

    private Map<String, AttributeValue<BytesArrayDto>> getAttributeContent(Mail mail) throws MailetException {
        return AttributeUtils
                .getValueAndCastFromMail(mail, attribute, Serializable.class)
                .map(this::castAttributeContent)
                .orElse(ImmutableMap.<String, AttributeValue<BytesArrayDto>>of());
    }

    @SuppressWarnings("unchecked")
    private Map<String, AttributeValue<BytesArrayDto>> castAttributeContent(Serializable attributeContent) {
        if (! (attributeContent instanceof Map)) {
            LOGGER.debug("Invalid attribute found into attribute {} class Map expected but {} found.",
                    attribute, attributeContent.getClass());
            return ImmutableMap.of();
        }
        return (Map<String, AttributeValue<BytesArrayDto>>) attributeContent;
    }

    @Override
    public String getMailetInfo() {
        return "MimeDecodingMailet";
    }

}
