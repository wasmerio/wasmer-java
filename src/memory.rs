use crate::{
    exception::{joption_or_throw, runtime_error, Error},
    types::{jptr, Pointer},
};
use jni::{objects::JClass, sys::jint, JNIEnv};
use std::{panic, rc::Rc};
use wasmer_runtime::Memory as WasmMemory;
use wasmer_runtime_core::units::Pages;

#[derive(Debug, Clone)]
pub struct Memory {
    pub memory: Rc<WasmMemory>,
}

impl Memory {
    pub fn new(memory: Rc<WasmMemory>) -> Self {
        Self { memory }
    }

    pub fn grow(&self, number_of_pages: u32) -> Result<u32, Error> {
        self.memory
            .grow(Pages(number_of_pages))
            .map(|previous_pages| previous_pages.0)
            .map_err(|e| runtime_error(format!("Failed to grow the memory: {}", e)))
    }
}

#[no_mangle]
pub extern "system" fn Java_org_wasmer_Memory_nativeMemoryGrow(
    env: JNIEnv,
    _class: JClass,
    memory_pointer: jptr,
    number_of_pages: jint,
) -> jint {
    let output = panic::catch_unwind(|| {
        let memory: &Memory = Into::<Pointer<Memory>>::into(memory_pointer).borrow();
        Ok(memory.grow(number_of_pages as u32)? as i32)
    });

    joption_or_throw(&env, output).unwrap_or(0)
}

pub mod java {
    use crate::{
        exception::Error,
        instance::Instance,
        types::{jptr, Pointer},
    };
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

            // Try to set the memory pointer to the field `org.wasmer.Memory.memoryPointer`.
            let memory_pointer: jptr = Pointer::new(memory.clone()).into();
            env.set_field(memory_object, "memoryPointer", "J", memory_pointer.into())?;

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
