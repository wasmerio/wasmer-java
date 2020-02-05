use crate::{
    exception::{joption_or_throw, runtime_error, Error},
    types::{jptr, Pointer},
};
use jni::{
    objects::{GlobalRef, JClass, JObject},
    sys::{jboolean, jbyteArray},
    JNIEnv,
};

use std::panic;

use wasmer_runtime::{self as runtime, validate};

pub struct Module {
    #[allow(unused)]
    java_module_object: GlobalRef,
    #[allow(unused)]
    module: runtime::Module,
}

impl Module {
    fn new(java_module_object: GlobalRef, module_bytes: Vec<u8>) -> Result<Self, Error> {
        let module_bytes = module_bytes.as_slice();
        let module = runtime::compile(module_bytes)
            .map_err(|e| runtime_error(format!("Failed to compile the module: {}", e)))?;

        Ok(Self {
            java_module_object,
            module,
        })
    }
}

#[no_mangle]
pub extern "system" fn Java_org_wasmer_Module_nativeModuleInstantiate(
    env: JNIEnv,
    _class: JClass,
    this: JObject,
    module_bytes: jbyteArray,
) -> jptr {
    let output = panic::catch_unwind(|| {
        let module_bytes = env.convert_byte_array(module_bytes)?;
        let java_module = env.new_global_ref(this)?;

        let module = Module::new(java_module, module_bytes)?;

        Ok(Pointer::new(module).into())
    });

    joption_or_throw(&env, output).unwrap_or(0)
}

#[no_mangle]
pub extern "system" fn Java_org_wasmer_Module_nativeDrop(
    _env: JNIEnv,
    _class: JClass,
    module_pointer: jptr,
) {
    let _: Pointer<Module> = module_pointer.into();
}

#[no_mangle]
pub extern "system" fn Java_org_wasmer_Module_nativeValidate(
    env: JNIEnv,
    _class: JClass,
    module_bytes: jbyteArray,
) -> jboolean {
    let output = panic::catch_unwind(|| {
        let module_bytes = env.convert_byte_array(module_bytes)?;
        match validate(module_bytes.as_slice()) {
            true => Ok(1),
            false => Ok(0),
        }
    });

    joption_or_throw(&env, output).unwrap_or(0)
}
