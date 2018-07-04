package org.apache.james.jmap;

public class JmapMethodNotImplementedException extends JmapNotImplementedException {
    private final String method;

    public JmapMethodNotImplementedException(String method) {
        super();
        this.method = method;
    }

    @Override
    public String getMessage() {
        return "'" + method + "' is not yet implemented";
    }
}
