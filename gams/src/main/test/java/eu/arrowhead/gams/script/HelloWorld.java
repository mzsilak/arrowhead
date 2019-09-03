package eu.arrowhead.gams.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Callable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

class HelloWorld {

    private final Logger logger = LogManager.getLogger();

    @Test
    void helloPolyglot() {
        logger.info("Hello Java!");
        try (Context context = Context.create()) {
            context.eval("js", "print('Hello JavaScript!');");
        }
    }

    @Test
    void evaluateWithReturn() {
        try (Context context = Context.create()) {
            Value function = context.eval("js", "x => x+1");
            assertTrue(function.canExecute());
            int x = function.execute(41).asInt();
            assertEquals(42, x);
        }
    }

    @Test
    void accessMembers() {
        try (Context context = Context.create()) {
            Value result = context.eval("js", "({ id: 42, text: '42', arr: [1,42,3] })");
            assertTrue(result.hasMembers());

            int id = result.getMember("id").asInt();
            assertEquals(42, id);

            String text = result.getMember("text").asString();
            assertEquals("42", text);

            Value array = result.getMember("arr");
            assertTrue(array.hasArrayElements());
            assertEquals(3, array.getArraySize());
            assertEquals(42, array.getArrayElement(1).asInt());
        }
    }

    @Test
    void accessJavaObjects() {
        try (Context context = Context.newBuilder().allowAllAccess(true).build()) {
            context.getBindings("js").putMember("javaObj", new MyClass());
            boolean valid = context.eval("js", ""
                + "javaObj.id == 42"
                + " && javaObj.text == '42'"
                + " && javaObj.arr[1] == 42"
                + " && javaObj.ret42() == 42").asBoolean();
            assertTrue(valid);
        }
    }

    @Test
    void accessJavaMethods()
    {
        try (Context context = Context.create()) {
            Services services = new Services();
            context.getBindings("js").putMember("services", services);
            String name = context.eval("js",
                                       "let emp = services.createEmployee('John Doe');" +
                                           "emp.getName()").asString();
            assertEquals("John Doe", name);

            try {
                context.eval("js", "services.exitVM()");
                assert false;
            } catch (PolyglotException e) {
                assert e.getMessage().endsWith(
                    "Unknown identifier: exitVM");
            }
        }
    }

    public static class MyClass {
        public int id = 42;
        public String text = "42";
        public int[] arr = new int[]{1, 42, 3};
        public Callable<Integer> ret42 = () -> 42;
    }

    public static class Employee {
        private final String name;
        Employee(String name) {this.name = name;}

        @HostAccess.Export
        public String getName() {
            return name;
        }
    }

    public static class Services {
        @HostAccess.Export
        public Employee createEmployee(String name) {
            return new Employee(name);
        }

        public void exitVM() {
            System.exit(1);
        }
    }
}
