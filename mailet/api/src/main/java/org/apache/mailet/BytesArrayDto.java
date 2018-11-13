package org.apache.mailet;

import java.util.Arrays;

public class BytesArrayDto {
    private final byte[] values;

    public BytesArrayDto(byte[] values) {
        this.values = values;
    }

    public byte[] getValues() {
        return values;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        BytesArrayDto other = (BytesArrayDto) obj;
        if (!Arrays.equals(values, other.values)) {
            return false;
        }

        return true;
    }

}
