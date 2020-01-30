use crate::{
    exception::{joption_or_throw, runtime_error, Error},
    types::{jptr, Pointer},
    value::{Value, DOUBLE_CLASS, FLOAT_CLASS, INT_CLASS, LONG_CLASS},
};
use jni::{
    errors,
    objects::{GlobalRef, JClass, JObject, JString, JValue},
    sys::{jbyteArray, jobjectArray},
    JNIEnv,
};
use std::{convert::TryFrom, panic, rc::Rc};
use wasmer_runtime::{imports, instantiate, Export, Memory as WasmMemory, Value as WasmValue};
use wasmer_runtime_core as core;

use std::cell::Cell;
use wasmer_runtime::memory::MemoryView;

pub struct Instance {
    #[allow(unused)]
    java_instance_object: GlobalRef,
    instance: Rc<core::Instance>,
    memory: Option<Rc<WasmMemory>>,
}

impl Instance {
    fn new(
        java_instance_object: GlobalRef,
        module_bytes: Vec<u8>,
        env: &JNIEnv,
    ) -> Result<Self, Error> {
        let module_bytes = module_bytes.as_slice();
        let imports = imports! {};
        let instance = match instantiate(module_bytes, &imports) {
            Ok(instance) => Rc::new(instance),
            Err(e) => {
                return Err(runtime_error(format!(
                    "Failed to instantiate the module: {}",
                    e
                )))
            }
        };

        let memory = instance.exports().find_map(|(_, export)| match export {
            Export::Memory(memory) => Some(Rc::new(memory)),
            _ => None,
        });

        let instance_wrapper = Self {
            java_instance_object,
            instance,
            memory,
        };

        instance_wrapper
            .set_exported_functions(&env)
            .map_err(|e| runtime_error(format!("Failed to set the exported functions: {}", e)))?;

        Ok(instance_wrapper)
    }

    fn call_exported_function(
        &self,
        export_name: String,
        arguments: Vec<WasmValue>,
    ) -> Result<Vec<WasmValue>, Error> {
        let function = self.instance.dyn_func(&export_name).map_err(|_| {
            runtime_error(format!(
                "Exported function `{}` does not exist",
                export_name
            ))
        })?;

        function
            .call(arguments.as_slice())
            .map_err(|e| runtime_error(format!("{}", e)))
    }

    fn set_exported_functions(&self, env: &JNIEnv) -> errors::Result<()> {
        let exports_object: JObject = env
            .get_field(
                self.java_instance_object.as_obj(),
                "exports",
                "Lorg/wasmer/Export;",
            )?
            .l()?;

        for (export_name, export) in self.instance.exports() {
            if let Export::Function { .. } = export {
                let name = env.new_string(export_name)?;

                env.call_method(
                    exports_object,
                    "addExportedFunction",
                    "(Ljava/lang/String;)V",
                    &[JObject::from(name).into()],
                )?;
            }
        }

        Ok(())
    }
}

#[no_mangle]
pub extern "system" fn Java_org_wasmer_Instance_nativeInstantiate(
    env: JNIEnv,
    _class: JClass,
    this: JObject,
    module_bytes: jbyteArray,
) -> jptr {
    let output = panic::catch_unwind(|| {
        let module_bytes = env.convert_byte_array(module_bytes)?;
        let java_instance = env.new_global_ref(this)?;

        let instance = Instance::new(java_instance, module_bytes, &env)?;

        Ok(Pointer::new(instance).into())
    });

    joption_or_throw(&env, output).unwrap_or(0)
}

#[no_mangle]
pub extern "system" fn Java_org_wasmer_Instance_nativeDrop(
    _env: JNIEnv,
    _class: JClass,
    instance_pointer: jptr,
) {
    let _: Pointer<Instance> = instance_pointer.into();
}

#[no_mangle]
pub extern "system" fn Java_org_wasmer_Instance_nativeGetMemoryData(
    env: JNIEnv,
    _class: JClass,
    instance_pointer: jptr,
) -> jbyteArray {
    let output = panic::catch_unwind(|| {
        let instance: &Instance = Into::<Pointer<Instance>>::into(instance_pointer).borrow();
        match &instance.memory {
            Some(memory) => {
                let view: MemoryView<u8> = memory.view();
                let data: Vec<u8> = view[0..view.len()].iter().map(Cell::get).collect();
                let output = env
                    .byte_array_from_slice(&data)
                    .expect("Failed to convert Rust byte slice to Java byte array.");
                Ok(output)
            }
            _ => Ok(JObject::null().into_inner()),
        }
    });

    joption_or_throw(&env, output).unwrap_or(JObject::null().into_inner())
}

#[no_mangle]
pub extern "system" fn Java_org_wasmer_Instance_nativeCall<'a>(
    env: JNIEnv<'a>,
    _class: JClass,
    instance_pointer: jptr,
    export_name: JString,
    arguments_pointer: jobjectArray,
) -> jobjectArray {
    let output = panic::catch_unwind(|| {
        let instance: &Instance = Into::<Pointer<Instance>>::into(instance_pointer).borrow();
        let export_name: String = env
            .get_string(export_name)
            .expect("Failed to get Java string")
            .into();

        let arguments_length = env.get_array_length(arguments_pointer).unwrap();

        let arguments: Vec<JObject> = (0..arguments_length)
            .map(|i| {
                env.get_object_array_element(arguments_pointer, i)
                    .expect("Failed to get Java object")
            })
            .collect();

        let results = instance.call_exported_function(
            export_name,
            arguments
                .iter()
                .map(|argument| {
                    Value::try_from((&env, *argument))
                        .expect("Failed to convert an argument to a WebAssembly value")
                        .inner()
                })
                .collect(),
        )?;

        let obj_array = env
            .new_object_array(
                i32::try_from(results.len()).unwrap(),
                "java/lang/Object",
                JObject::null(),
            )
            .expect("Failed to create a Java object array");
        if results.len() > 0 {
            for (i, result) in results.iter().enumerate() {
                let obj = match result {
                    WasmValue::I32(val) => env.new_object(INT_CLASS, "(I)V", &[JValue::from(*val)]),
                    WasmValue::I64(val) => {
                        env.new_object(LONG_CLASS, "(J)V", &[JValue::from(*val)])
                    }
                    WasmValue::F32(val) => {
                        env.new_object(FLOAT_CLASS, "(F)V", &[JValue::from(*val)])
                    }
                    WasmValue::F64(val) => {
                        env.new_object(DOUBLE_CLASS, "(D)V", &[JValue::from(*val)])
                    }
                    _ => unreachable!(),
                };
                env.set_object_array_element(
                    obj_array,
                    i as i32,
                    obj.expect("Failed to create a Java object"),
                )
                .expect("Failed to set a Java object element");
            }
            Ok(obj_array)
        } else {
            Ok(JObject::null().into_inner())
        }
    });

    joption_or_throw(&env, output).unwrap_or(JObject::null().into_inner())
}
