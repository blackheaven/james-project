package org.apache.mailet;

import java.util.Optional;

public interface QueueSerializable {
    public class Serializable {
        private final AttributeValue<?> value;
        private final Class<? extends Factory> factory;
        public Serializable(AttributeValue<?> value, Class<? extends Factory> factory) {
            this.value = value;
            this.factory = factory;
        }

        public Class<? extends Factory> getFactory() {
            return factory;
        }

        public AttributeValue<?> getValue() {
            return value;
        }
    }

    public interface Factory {
        Optional<QueueSerializable> deserialize(Serializable serializable);
    }
    
    Serializable serialize();
}
