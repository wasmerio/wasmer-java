package org.wasmer;

import java.util.HashMap;
import java.util.Map;

class Memories {
    private Map<String, Memory> inner;

    protected Memories() {
        this.inner = new HashMap<String, Memory>();
    }

    public Memory get(String name) {
        return this.inner.get(name);
    }

    public Map<String, Memory> inner() {
        return this.inner;
    }
}
