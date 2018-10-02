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


package org.apache.mailet.base.test;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.james.core.MailAddress;
import org.apache.james.core.MaybeSender;
import org.apache.james.core.builder.MimeMessageBuilder;
import org.apache.james.util.MimeMessageUtil;
import org.apache.mailet.Attribute;
import org.apache.mailet.AttributeName;
import org.apache.mailet.AttributeValue;
import org.apache.mailet.Mail;
import org.apache.mailet.PerRecipientHeaders;
import org.apache.mailet.PerRecipientHeaders.Header;

import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class FakeMail implements Mail, Serializable {

    private static final String DEFAULT_REMOTE_HOST = "111.222.333.444";
    public static final String DEFAULT_REMOTE_ADDRESS = "127.0.0.1";

    public static FakeMail fromMessage(MimeMessageBuilder message) throws MessagingException {
        return FakeMail.builder()
            .mimeMessage(message)
            .build();
    }

    public static FakeMail fromMime(String text, String javaEncodingCharset, String javamailDefaultEncodingCharset) throws MessagingException, UnsupportedEncodingException {
        Properties javamailProperties = new Properties();
        javamailProperties.setProperty("mail.mime.charset", javamailDefaultEncodingCharset);
        return FakeMail.builder()
                .mimeMessage(MimeMessageUtil.mimeMessageFromBytes((text.getBytes(javaEncodingCharset))))
                .build();
    }

    public static FakeMail from(MimeMessage message) throws MessagingException {
        return builder()
                .mimeMessage(message)
                .build();
    }

    public static FakeMail from(MimeMessageBuilder message) throws MessagingException {
        return from(message.build());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Optional<String> fileName;
        private Optional<MimeMessage> mimeMessage;
        private List<MailAddress> recipients;
        private Optional<String> name;
        private Optional<MailAddress> sender;
        private Optional<String> state;
        private Optional<String> errorMessage;
        private Optional<Date> lastUpdated;
        private Map<AttributeName, AttributeValue<?>> attributes;
        private Optional<Long> size;
        private Optional<String> remoteAddr;
        private Optional<String> remoteHost;
        private PerRecipientHeaders perRecipientHeaders;

        private Builder() {
            fileName = Optional.empty();
            mimeMessage = Optional.empty();
            recipients = Lists.newArrayList();
            name = Optional.empty();
            sender = Optional.empty();
            state = Optional.empty();
            errorMessage = Optional.empty();
            lastUpdated = Optional.empty();
            attributes = Maps.newHashMap();
            size = Optional.empty();
            remoteAddr = Optional.empty();
            remoteHost = Optional.empty();
            perRecipientHeaders = new PerRecipientHeaders();
        }

        public Builder attribute(Attribute attribute) {
            this.attributes.put(attribute.getName(), attribute.getValue());
            return this;
        }

        public Builder size(long size) {
            this.size = Optional.of(size);
            return this;
        }

        public Builder fileName(String fileName) {
            Preconditions.checkNotNull(fileName,"'fileName' can not be null");
            this.fileName = Optional.of(fileName);
            return this;
        }

        public Builder mimeMessage(MimeMessage mimeMessage) {
            Preconditions.checkNotNull(mimeMessage, "'mimeMessage' can not be null");
            this.mimeMessage = Optional.of(mimeMessage);
            return this;
        }

        public Builder mimeMessage(MimeMessageBuilder mimeMessage) throws MessagingException {
            Preconditions.checkNotNull(mimeMessage, "'mimeMessage' can not be null");
            this.mimeMessage = Optional.of(mimeMessage.build());
            return this;
        }

        public Builder recipients() {
            return this;
        }

        public Builder recipients(List<MailAddress> recipients) {
            this.recipients.addAll(recipients);
            return this;
        }

        public Builder recipients(MailAddress... recipients) {
            return recipients(ImmutableList.copyOf(recipients));
        }

        public Builder recipients(String... recipients) {
            Preconditions.checkNotNull(recipients, "'recipients' can not be null");
            return recipients(Arrays.stream(recipients)
                .map(Throwing.function(MailAddress::new))
                .collect(Guavate.toImmutableList()));
        }

        public Builder recipient(MailAddress recipient) {
            return recipients(recipient);
        }

        public Builder recipient(String recipient) throws AddressException {
            return recipients(recipient);
        }

        public Builder name(String name) {
            Preconditions.checkNotNull(name, "'name' can not be null");
            name(Optional.of(name));
            return this;
        }

        public Builder name(Optional<String> name) {
            this.name = name;
            return this;
        }

        public Builder sender(MailAddress sender) {
            Preconditions.checkNotNull(sender, "'sender' can not be null");
            sender(MaybeSender.of(sender));
            return this;
        }

        public Builder sender(MaybeSender sender) {
            this.sender = sender.asOptional();
            return this;
        }

        public Builder sender(String sender) throws AddressException {
            return sender(new MailAddress(sender));
        }

        public Builder state(String state) {
            Preconditions.checkNotNull(state, "'state' can not be null");
            state(Optional.of(state));
            return this;
        }

        public Builder state(Optional<String> state) {
            this.state = state;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            Preconditions.checkNotNull(errorMessage, "'errorMessage' can not be null");
            errorMessage(Optional.of(errorMessage));
            return this;
        }

        public Builder errorMessage(Optional<String> errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder lastUpdated(Date lastUpdated) {
            Preconditions.checkNotNull(lastUpdated, "'lastUpdated' can not be null");
            lastUpdated(Optional.of(lastUpdated));
            return this;
        }

        public Builder lastUpdated(Optional<Date> lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public Builder remoteAddr(String remoteAddr) {
            this.remoteAddr = Optional.of(remoteAddr);
            return this;
        }

        public Builder remoteHost(String remoteHost) {
            this.remoteHost = Optional.of(remoteHost);
            return this;
        }

        public Builder addHeaderForRecipient(Header header, MailAddress recipient) {
            this.perRecipientHeaders.addHeaderForRecipient(header, recipient);
            return this;
        }

        public FakeMail build() throws MessagingException {
            return new FakeMail(getMimeMessage(), recipients, name.orElse(null), sender.orElse(null), state.orElse(null), errorMessage.orElse(null), lastUpdated.orElse(null),
                attributes, size.orElse(0L), remoteAddr.orElse(DEFAULT_REMOTE_ADDRESS), remoteHost.orElse(DEFAULT_REMOTE_HOST), perRecipientHeaders);
        }

        private MimeMessage getMimeMessage() throws MessagingException {
            Preconditions.checkState(!(fileName.isPresent() && mimeMessage.isPresent()), "You can not specify a MimeMessage object when you alredy set Content from a file");
            if (fileName.isPresent()) {
                return MimeMessageUtil.mimeMessageFromStream(ClassLoader.getSystemResourceAsStream(fileName.get()));
            }
            return mimeMessage.orElse(null);
        }
    }

    public static FakeMail defaultFakeMail() throws MessagingException {
        return FakeMail.builder().build();
    }

    private static Map<AttributeName, AttributeValue<?>> attributes(Mail mail) {
        return mail.attributes()
            .map(Attribute::duplicate)
            .collect(Collectors.toMap(
                Attribute::getName,
                Attribute::getValue));
    }

    private transient MimeMessage msg;
    private Collection<MailAddress> recipients;
    private String name;
    private MailAddress sender;
    private String state;
    private String errorMessage;
    private Date lastUpdated;
    private Map<AttributeName, AttributeValue<?>> attributes;
    private long size;
    private String remoteAddr;
    private String remoteHost;
    private PerRecipientHeaders perRecipientHeaders;
    
    public FakeMail(MimeMessage msg, List<MailAddress> recipients, String name, MailAddress sender, String state, String errorMessage, Date lastUpdated,
                    Map<AttributeName, AttributeValue<?>> attributes, long size, String remoteAddr, String remoteHost, PerRecipientHeaders perRecipientHeaders) {
        this.msg = msg;
        this.recipients = recipients;
        this.name = name;
        this.sender = sender;
        this.state = state;
        this.errorMessage = errorMessage;
        this.lastUpdated = lastUpdated;
        this.attributes = attributes;
        this.size = size;
        this.remoteAddr = remoteAddr;
        this.perRecipientHeaders = perRecipientHeaders;
        this.remoteHost = remoteHost;
    }

    @Override
    public Mail duplicate() throws MessagingException {
        return builder()
            .mimeMessage(msg)
            .recipients(ImmutableList.copyOf(recipients))
            .name(Optional.ofNullable(name))
            .sender(MaybeSender.of(sender))
            .state(Optional.ofNullable(state))
            .errorMessage(Optional.ofNullable(errorMessage))
            .lastUpdated(Optional.ofNullable(lastUpdated))
            .attributes(attributes)
            .size(size)
            .remoteAddr(remoteAddr)
            .remoteHost(remoteHost)
            .build();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String newName) {
        this.name = newName;
    }

    @Override
    public MimeMessage getMessage() throws MessagingException {
        return msg;
    }

    @Override
    public Collection<MailAddress> getRecipients() {
        return recipients;
    }

    @Override
    public void setRecipients(Collection<MailAddress> recipients) {
        this.recipients = recipients;
    }

    @Override
    public MailAddress getSender() {
        return sender;
    }

    @Override
    public String getState() {
        return state;
    }

    @Override
    public String getRemoteHost() {
        return remoteHost;
    }

    @Override
    public String getRemoteAddr() {
        return remoteAddr;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public void setErrorMessage(String msg) {
        this.errorMessage = msg;
    }

    @Override
    public void setMessage(MimeMessage message) {
        this.msg = message;
        try {
            if (message != null && message.getSender() != null) {
                this.sender = new MailAddress((InternetAddress) message.getSender());
            }
        } catch (MessagingException e) {
            throw new RuntimeException("Exception caught", e);
        }
    }

    @Override
    public void setState(String state) {
        this.state = state;
    }

    @Override
    public Stream<Attribute> attributes() {
        return attributes.entrySet().stream().map(entry -> new Attribute(entry.getKey(), entry.getValue()));
    }

    @Override
    public Serializable getAttribute(String name) {
        return (Serializable) attributes.get(AttributeName.of(name)).getValue();
    }

    @Override
    public Optional<Attribute> getAttribute(AttributeName name) {
        throw new RuntimeException("to be implemented");
    }

    @Override
    public Iterator<String> getAttributeNames() {
        return attributes.keySet().stream().map(AttributeName::asString).iterator();
    }

    @Override
    public Iterator<AttributeName> attributeNames() {
        throw new RuntimeException("to be implemented");
    }

    @Override
    public boolean hasAttributes() {
        return !attributes.isEmpty();
    }

    @Override
    public Serializable removeAttribute(String name) {
        AttributeValue<?> previous = attributes.remove(AttributeName.of(name));
        return (Serializable) Optional.ofNullable(previous).map(AttributeValue::value).orElse(null);
    }

    @Override
    public Attribute removeAttribute(AttributeName attributeName) {
        throw new RuntimeException("to be implemented");
    }

    @Override
    public void removeAllAttributes() {
        attributes.clear();
    }

    @Override
    public Serializable setAttribute(String name, Serializable object) {
        AttributeValue<?> previous = attributes.put(AttributeName.of(name), AttributeValue.of(object));
        return (Serializable) Optional.ofNullable(previous).map(AttributeValue::value).orElse(null);
    }

    @Override
    public Attribute setAttribute(Attribute attribute) {
        throw new RuntimeException("to be implemented");
    }

    @Override
    public long getMessageSize() throws MessagingException {
        return size;
    }

    @Override
    public Date getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setMessageSize(long size) {
        this.size = size;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof FakeMail) {
            FakeMail that = (FakeMail) o;

            return Objects.equal(this.size, that.size)
                && Objects.equal(this.recipients, that.recipients)
                && Objects.equal(this.name, that.name)
                && Objects.equal(this.sender, that.sender)
                && Objects.equal(this.state, that.state)
                && Objects.equal(this.errorMessage, that.errorMessage)
                && Objects.equal(this.lastUpdated, that.lastUpdated)
                && Objects.equal(this.attributes, that.attributes)
                && Objects.equal(this.remoteHost, that.remoteHost)
                && Objects.equal(this.perRecipientHeaders, that.perRecipientHeaders)
                && Objects.equal(this.remoteAddr, that.remoteAddr);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(name, sender, recipients, state, errorMessage, lastUpdated, attributes, size, remoteAddr, remoteHost, perRecipientHeaders);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("msg", msg)
            .add("recipients", recipients)
            .add("name", name)
            .add("sender", sender)
            .add("state", state)
            .add("errorMessage", errorMessage)
            .add("lastUpdated", lastUpdated)
            .add("attributes", attributes)
            .add("size", size)
            .add("remoteAddr", remoteAddr)
            .toString();
    }

    @Override
    public PerRecipientHeaders getPerRecipientSpecificHeaders() {
        return perRecipientHeaders;
    }

    @Override
    public void addSpecificHeaderForRecipient(Header header, MailAddress recipient) {
        perRecipientHeaders.addHeaderForRecipient(header, recipient);
    }
}
