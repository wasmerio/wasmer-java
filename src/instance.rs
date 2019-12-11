use crate::{
    exception::unwrap_or_throw,
    types::{jptr, Pointer},
};
use jni::{
    objects::{GlobalRef, JClass, JObject, JString, JValue},
    sys::{jbyteArray, jint, jintArray, jlong, jstring},
    JNIEnv,
};
use std::{fmt, panic};

pub(crate) struct Instance {
    java_instance: GlobalRef,
}

impl fmt::Debug for Instance {
    fn fmt(&self, formatter: &mut fmt::Formatter) -> fmt::Result {
        write!(formatter, "Instance {{ }}")
    }
}

impl Instance {
    fn new(java_instance: GlobalRef, _module_bytes: Vec<u8>) -> Self {
        Self { java_instance }
    }
}

#[no_mangle]
pub extern "system" fn Java_org_wasmer_Instance_instantiate(
    env: JNIEnv,
    _class: JClass,
    this: JObject,
    module_bytes: jbyteArray,
) -> jptr {
    let output = panic::catch_unwind(|| {
        let module_bytes = env.convert_byte_array(module_bytes)?;
        let java_instance = env.new_global_ref(this)?;

        let instance = Instance::new(java_instance, module_bytes);

        Ok(Pointer::new(instance).into())
    });

    unwrap_or_throw(&env, output)
}

#[no_mangle]
pub extern "system" fn Java_org_wasmer_Instance_drop(
    _env: JNIEnv,
    _class: JClass,
    instance_pointer: jptr,
) {
    let _: Pointer<Instance> = instance_pointer.into();
}

#[no_mangle]
pub extern "system" fn Java_org_wasmer_Instance_dynCall(
    env: JNIEnv,
    _class: JClass,
    instance_pointer: jlong,
    export_name: JString,
    arguments: jintArray,
) -> jint {
    let _instance: &Instance = Into::<Pointer<Instance>>::into(instance_pointer).borrow();
    let _export_name: String = env
        .get_string(export_name)
        .expect("Could not get Java string.")
        .into();

    let arguments_length = env.get_array_length(arguments).unwrap();

    let mut buffer = vec![0; arguments_length as usize];
    env.get_int_array_region(arguments, 0, buffer.as_mut_slice())
        .unwrap();

    buffer[0] + buffer[1]
}
