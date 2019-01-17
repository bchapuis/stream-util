package ch.bertil.stream;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ch.bertil.stream.StreamExceptions.*;

public class StreamExceptionsTest {

    @Test
    public void testConsumerWithCheckedExceptions() throws ClassNotFoundException {
        Stream.of("java.lang.Object", "java.lang.Integer", "java.lang.String")
                .forEach(rethrowConsumer(className -> System.out.println(Class.forName(className))));
        Stream.of("java.lang.Object", "java.lang.Integer", "java.lang.String")
                .forEach(rethrowConsumer(System.out::println));
    }

    @Test
    public void testFunctionWithCheckedExceptions() throws ClassNotFoundException {
        List<Class> classes1
                = Stream.of("Object", "Integer", "String")
                .map(rethrowFunction(className -> Class.forName("java.lang." + className)))
                .collect(Collectors.toList());
        List<Class> classes2
                = Stream.of("java.lang.Object", "java.lang.Integer", "java.lang.String")
                .map(rethrowFunction(Class::forName))
                .collect(Collectors.toList());
    }

    @Test
    public void testSupplierWithCheckedExceptions() throws UnsupportedEncodingException {
        Collector.of(
                rethrowSupplier(() -> new StringJoiner(new String(new byte[]{77, 97, 114, 107}, "UTF-8"))),
                StringJoiner::add, StringJoiner::merge, StringJoiner::toString);
    }

    @Test
    public void testUncheckExceptionThrownByMethod() {
        Class clazz1 = uncheck(() -> Class.forName("java.lang.String"));
        Class clazz2 = uncheck(Class::forName, "java.lang.String");
    }

    @Test(expected = ClassNotFoundException.class)
    public void testIfCorrectExceptionIsStillThrownByMethod() {
        Class clazz3 = uncheck(Class::forName, "INVALID");
    }

}