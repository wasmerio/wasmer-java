use std::rc::Rc;
use wasmer_runtime::Memory as WasmMemory;

#[derive(Debug)]
pub struct Memory {
    pub memory: Rc<WasmMemory>,
}

impl Memory {
    pub fn new(memory: Rc<WasmMemory>) -> Self {
        Self { memory }
    }
}

pub mod java {
    use crate::{exception::Error, instance::Instance};
    use jni::{objects::JObject, JNIEnv};
    use std::cell::Cell;

    pub fn initialize_memories(env: &JNIEnv, instance: &Instance) -> Result<(), Error> {
        // Try to read `org.wasmer.Memories` from the
        // `org.wasmer.Instance.memores` attribute.
        let memories = env
            .get_field(
                instance.java_instance_object.as_obj(),
                "memories",
                "Lorg/wasmer/Memories;",
            )?
            .l()?;

        // Try to read `java.util.Map` from the
        // `org.wasmer.Memories.inner` attribute.
        let memories_map = env.get_field(memories, "inner", "Ljava/util/Map;")?.l()?;

        // Try to cast the `JObject` to `JMap`.
        let memories_map = env.get_map(memories_map)?;

        // Get the `org.wasmer.Memory` class.
        let memory_class = env.find_class("org/wasmer/Memory")?;

        for (memory_name, memory) in &instance.memories {
            // Read the memory data as a pointer to `u8`.
            let view = memory.memory.view::<u8>();
            let data = unsafe {
                std::slice::from_raw_parts_mut(
                    view[..].as_ptr() as *mut Cell<u8> as *mut u8,
                    view.len(),
                )
            };

            // Create a new `JByteBuffer`, aka `java.nio.ByteBuffer`,
            // borrowing the data from the WebAssembly memory.
            let byte_buffer = env.new_direct_byte_buffer(data)?;

            // Instantiate the `Memory` class.
            let memory_object = env.new_object(memory_class, "()V", &[])?;

            // Try to write the `org.wasmer.Memory.inner` attribute by
            // calling the `org.wasmer.Memory.setInner` method.
            env.call_method(
                memory_object,
                "setInner",
                "(Ljava/nio/ByteBuffer;)V",
                &[JObject::from(byte_buffer).into()],
            )?;

            // Add the newly created `org.wasmer.Memory` in the
            // `org.wasmer.Memories` collection.
            memories_map.put(*env.new_string(memory_name)?, memory_object)?;
        }

        Ok(())
    }
}
