package org.apache.james.jmap;

public class JmapFieldNotSupportedException extends JmapNotImplementedException {
    private final String method;
    private final String field;

    public JmapFieldNotSupportedException(String method, String field) {
        super();
        this.method = method;
        this.field = field;
    }

    @Override
    public String getMessage() {
        return "'" + field + "' is not yet implemented for '" + method + "'";
    }
}
