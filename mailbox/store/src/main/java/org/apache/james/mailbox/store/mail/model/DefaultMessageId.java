package org.apache.james.mailbox.store.mail.model;

import java.util.Optional;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.james.mailbox.model.MessageId;
import org.apache.mailet.QueueSerializable;

public class DefaultMessageId implements MessageId {

    public static class Factory implements MessageId.Factory {
        
        @Override
        public MessageId generate() {
            return new DefaultMessageId();
        }

        @Override
        public Optional<QueueSerializable> deserialize(Serializable serializable) {
            throw new NotImplementedException("MessageId is not supported by this backend");
        }
    }
    
    public DefaultMessageId() {
    }

    @Override
    public String asString() {
        throw new IllegalStateException("Capabilities should prevent calling this method");
    }
    
    @Override
    public final boolean equals(Object obj) {
        throw new IllegalStateException("Capabilities should prevent calling this method");
    }
    
    @Override
    public final int hashCode() {
        throw new IllegalStateException("Capabilities should prevent calling this method");
    }

    @Override
    public String toString() {
        return "DefaultMessageId{}";
    }

    @Override
    public Serializable serialize() {
        throw new IllegalStateException("Capabilities should prevent calling this method");
    }
}
