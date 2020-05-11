import org.wasmer.Instance;
import org.wasmer.Memory;

import java.io.IOException;
import java.lang.StringBuilder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

class GreetExample {
    public static void main(String[] args) throws IOException {
        // Instantiates the module.
        byte[] bytes = Files.readAllBytes(Paths.get("greet.wasm"));
        Instance instance = new Instance(bytes);
        Memory memory = instance.exports.getMemory("memory");

        // Set the subject to greet.
        byte[] subject = "Wasmer".getBytes(StandardCharsets.UTF_8);

        // Allocate memory for the subject, and get a pointer to it.
        Integer input_pointer = (Integer) instance.exports.getFunction("allocate").apply(subject.length)[0];

        // Write the subject into the memory.
        {
            ByteBuffer memoryBuffer = memory.buffer();
            memoryBuffer.position(input_pointer);
            memoryBuffer.put(subject);
        }

        // Run the `greet` function. Give the pointer to the subject.
        Integer output_pointer = (Integer) instance.exports.getFunction("greet").apply(input_pointer)[0];

        // Read the result of the `greet` function.
        StringBuilder output = new StringBuilder();

        {
            ByteBuffer memoryBuffer = memory.buffer();

            for (Integer i = output_pointer, max = memoryBuffer.limit(); i < max; ++i) {
                byte[] b = new byte[1];
                memoryBuffer.position(i);
                memoryBuffer.get(b);

                if (b[0] == 0) {
                    break;
                }

                output.appendCodePoint(b[0]);
            }
        }

        System.out.println(output);

        instance.close();
    }
}
