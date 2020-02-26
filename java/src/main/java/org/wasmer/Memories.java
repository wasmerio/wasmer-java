package org.wasmer;

import java.util.HashMap;
import java.util.Map;

/**
 * `Memories` is a Java class that represents the set of WebAssembly memories.
 *
 * Example:
 * <pre>{@code
 * Instance instance = new Instance(wasmBytes);
 * Memories memories = instance.memories;
 * Memory memory = memories.get("memory-name");
 * }</pre>
 */
class Memories {
    private Map<String, Memory> inner;

    protected Memories() {
        this.inner = new HashMap<String, Memory>();
    }

    /**
     * Returns the memory object to which the specified name is mapped, or null if this contains no mapping for the name.
     *
     * @param name Memory name.
     * @return Memory or null if `name` is not a memory name.
     */
    public Memory get(String name) {
        return this.inner.get(name);
    }
}
