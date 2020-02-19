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
    use wasmer_runtime::memory::MemoryView;

    pub fn initialize_memories(env: &JNIEnv, instance: &Instance) -> Result<(), Error> {
        let memories_object = env
            .get_field(
                instance.java_instance_object.as_obj(),
                "memories",
                "Lorg/wasmer/Memories;",
            )?
            .l()?;
        let inner_object = env
            .get_field(memories_object, "inner", "Ljava/util/Map;")?
            .l()?;
        let jmap = env.get_map(inner_object)?;

        let memory_class = env.find_class("org/wasmer/Memory")?;

        for (export_name, memory) in &instance.memories {
            let view: MemoryView<u8> = memory.memory.view();
            let data = unsafe {
                std::slice::from_raw_parts_mut(
                    view[..].as_ptr() as *mut Cell<u8> as *mut u8,
                    view.len(),
                )
            };

            let memory_object = env.new_object(memory_class, "()V", &[])?;
            let java_buffer = env.new_direct_byte_buffer(data)?;
            env.call_method(
                memory_object,
                "setInner",
                "(Ljava/nio/ByteBuffer;)V",
                &[JObject::from(java_buffer).into()],
            )?;

            let name = env.new_string(export_name)?;
            jmap.put(*name, memory_object)?;
        }
        Ok(())
    }
}
