package org.wasmer;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Imports {

    static {
        if (!Native.LOADED_EMBEDDED_LIBRARY) {
            System.loadLibrary("wasmer_jni");
        }
    }
    private static native long nativeImportsInstantiate(List<Spec> imports, long modulePointer) throws RuntimeException;
    private static native long nativeImportsChain(long back, long froont) throws RuntimeException;
    private static native long nativeImportsWasi() throws RuntimeException;
    private static native void nativeDrop(long nativePointer);

    final long importsPointer;

    public static class Spec {
        public final String namespace;
        public final String name;
        final Function<long[], long[]> function;
        public final List<Type> argTypes;
        public final List<Type> retTypes;
        final int[] argTypesInt;
        final int[] retTypesInt;

        public Spec(String namespace, String name, Function<List<Number>, List<Number>> function, List<Type> argTypes, List<Type> retTypes) {
            this.namespace = namespace;
            this.name = name;
            this.function = (long[] argv) -> {
                List<Number> lret = function.apply(IntStream.range(0, argTypes.size()).mapToObj((int i) -> {
                    switch (argTypes.get(i)) {
                        case I32:
                            return (int) argv[i];
                        case I64:
                            return argv[i];
                        case F32:
                            return Float.intBitsToFloat((int) argv[i]);
                        case F64:
                            return Double.longBitsToDouble(argv[i]);
                        default:
                            throw new RuntimeException("Unreachable (argument type)");
                    }
                }).collect(Collectors.toList()));
                long[] ret = argv.length >= retTypes.size() ? argv : new long[retTypes.size()];
                for (int i = 0; i < retTypes.size(); i++)
                    switch (retTypes.get(i)) {
                        case I32:
                            ret[i] = lret.get(i).longValue();
                            break;
                        case I64:
                            ret[i] = lret.get(i).longValue();
                            break;
                        case F32:
                            ret[i] = Float.floatToRawIntBits(lret.get(i).floatValue());
                            break;
                        case F64:
                            ret[i] = Double.doubleToRawLongBits(lret.get(i).doubleValue());
                            break;
                        default:
                            throw new RuntimeException("Unreachable (return type)");
                    }
                return ret;
            };
            this.argTypesInt = argTypes.stream().mapToInt(t -> t.i).toArray();
            this.retTypesInt = retTypes.stream().mapToInt(t -> t.i).toArray();
            this.argTypes = Collections.unmodifiableList(argTypes);
            this.retTypes = Collections.unmodifiableList(retTypes);
        }
    }

    private Imports(long importsPointer) {
        this.importsPointer = importsPointer;
    }

    public static Imports from(List<Spec> imports, Module module) throws RuntimeException {
        return new Imports(nativeImportsInstantiate(imports, module.modulePointer));
    }

    public static Imports chain(Imports back, Imports front) {
       return new Imports(nativeImportsChain(back.importsPointer, front.importsPointer));
    }

    public static Imports wasi() {
        return new Imports(nativeImportsWasi());
    }

    protected void finalize() {
        nativeDrop(importsPointer);
        // TODO allow memory-safe user invocation
    }
}
