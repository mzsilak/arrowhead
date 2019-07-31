package eu.arrowhead.common.opcua.datatypes;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

public class ServiceRegistryEntry {

    private final String foo;
    private final UInteger bar;
    private final boolean baz;

    public ServiceRegistryEntry() {
        this(null, uint(0), false);
    }

    public ServiceRegistryEntry(String foo, UInteger bar, boolean baz) {
        this.foo = foo;
        this.bar = bar;
        this.baz = baz;
    }

    public String getFoo() {
        return foo;
    }

    public UInteger getBar() {
        return bar;
    }

    public boolean isBaz() {
        return baz;
    }

}