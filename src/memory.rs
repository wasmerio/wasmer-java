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
