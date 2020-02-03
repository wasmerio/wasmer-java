use crate::{
    exception::joption_or_throw,
    instance::Instance,
    types::{jptr, Pointer},
};
use jni::{
    objects::{JByteBuffer, JClass},
    sys::jlong,
    JNIEnv,
};
use std::{panic, rc::Rc};
use wasmer_runtime::{memory::MemoryView, Memory as WasmMemory};

#[derive(Debug)]
pub struct Memory {
    pub memory: Rc<WasmMemory>,
}

impl Memory {
    pub fn new(memory: Rc<WasmMemory>) -> Self {
        Self { memory }
    }
}

#[no_mangle]
pub extern "system" fn Java_org_wasmer_Memory_nativeSetMemoryData(
    env: JNIEnv,
    _class: JClass,
    instance_pointer: jptr,
    java_buffer: JByteBuffer,
) -> jlong {
    let output = panic::catch_unwind(|| {
        let instance: &Instance = Into::<Pointer<Instance>>::into(instance_pointer).borrow();
        let buffer = env.get_direct_buffer_address(java_buffer)?;
        match &instance.memory {
            Some(memory) => {
                let view: MemoryView<u8> = memory.memory.view();
                for (i, byte) in view[0..view.len()].iter().enumerate() {
                    buffer[i] = byte.get();
                }
                Ok(view.len() as jlong)
            }
            _ => Ok(0),
        }
    });

    joption_or_throw(&env, output).unwrap_or(0)
}
