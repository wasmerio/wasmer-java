use crate::{
    exception::{joption_or_throw, runtime_error, Error},
    types::{jptr, Pointer},
    value::{Value, DOUBLE_CLASS, FLOAT_CLASS, INT_CLASS, LONG_CLASS},
};
use jni::{
    objects::{GlobalRef, JClass, JObject, JString, JValue},
    sys::{jbyteArray, jlong, jobjectArray},
    JNIEnv,
};
use std::{convert::TryFrom, panic, rc::Rc};
use wasmer_runtime::{imports, instantiate, Export, Value as WasmValue};
use wasmer_runtime_core as core;

pub struct Instance {
    #[allow(unused)]
    java_instance_object: GlobalRef,
    instance: Rc<core::Instance>,
}

impl Instance {
    fn new(java_instance_object: GlobalRef, module_bytes: Vec<u8>) -> Result<Self, Error> {
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

        Ok(Self {
            java_instance_object,
            instance,
        })
    }

    fn call_exported_function(
        &self,
        export_name: String,
        arguments: Vec<WasmValue>,
    ) -> Result<Vec<WasmValue>, Error> {
        let function = self.instance.dyn_func(&export_name).map_err(|_| {
            runtime_error(format!(
                "Exported function `{}` does not exist.",
                export_name
            ))
        })?;

        function
            .call(arguments.as_slice())
            .map_err(|e| runtime_error(format!("{}", e)))
    }

    fn set_exported_functions(&self, env: &JNIEnv) {
        for (export_name, export) in self.instance.exports() {
            if let Export::Function { .. } = export {
                let name = env
                    .new_string(export_name)
                    .expect("Failed to create a new string object.");
                env.call_method(
                    self.java_instance_object.as_obj(),
                    "addExportFunction",
                    "(Ljava/lang/String;)V",
                    &[JObject::from(name).into()],
                )
                .expect("Failed to add an exported function.");
            }
        }
        self.unmodifiable_map(env);
    }

    fn unmodifiable_map(&self, env: &JNIEnv) {
        env.call_method(
            self.java_instance_object.as_obj(),
            "unmodifiableMap",
            "()V",
            &[],
        )
        .expect("Failed to make a hashmap unmodifiable.");
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

        let instance = Instance::new(java_instance, module_bytes)?;
        instance.set_exported_functions(&env);

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
pub extern "system" fn Java_org_wasmer_Instance_nativeCall<'a>(
    env: JNIEnv<'a>,
    _class: JClass,
    instance_pointer: jlong,
    export_name: JString,
    arguments_pointer: jobjectArray,
) -> jobjectArray {
    let output = panic::catch_unwind(|| {
        let instance: &Instance = Into::<Pointer<Instance>>::into(instance_pointer).borrow();
        let export_name: String = env
            .get_string(export_name)
            .expect("Could not get Java string.")
            .into();

        let arguments_length = env.get_array_length(arguments_pointer).unwrap();

        let arguments: Vec<JObject> = (0..arguments_length)
            .map(|i| {
                env.get_object_array_element(arguments_pointer, i)
                    .expect("Could not get Java object")
            })
            .collect();

        let results = instance.call_exported_function(
            export_name,
            arguments
                .iter()
                .map(|argument| {
                    Value::try_from((&env, *argument))
                        .expect("Could not convert an argument to a WebAssembly value")
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
            .expect("Could not create a Java object array");
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
                    obj.expect("Could not create a Java object"),
                )
                .expect("Could not set a Java object element");
            }
            Ok(obj_array)
        } else {
            Ok(JObject::null().into_inner())
        }
    });

    joption_or_throw(&env, output).unwrap_or(JObject::null().into_inner())
}
