use crate::exception::{runtime_error, Error};
use jni::{objects::JObject, sys::jlong, JNIEnv};
use std::ops::Deref;
use wasmer_runtime::Value;

const INT_CLASS: &str = "java/lang/Integer";
const LONG_CLASS: &str = "java/lang/Long";
const FLOAT_CLASS: &str = "java/lang/Float";
const DOUBLE_CLASS: &str = "java/lang/Double";

#[allow(non_camel_case_types)]
pub type jptr = jlong;

#[derive(Debug)]
pub struct Pointer<Kind> {
    value: Box<Kind>,
}

impl<Kind> Pointer<Kind> {
    pub fn new(value: Kind) -> Self {
        Pointer {
            value: Box::new(value),
        }
    }

    pub fn borrow<'a>(self) -> &'a mut Kind {
        Box::leak(self.value)
    }
}

impl<Kind> From<Pointer<Kind>> for jptr {
    fn from(pointer: Pointer<Kind>) -> Self {
        Box::into_raw(pointer.value) as _
    }
}

impl<Kind> From<jptr> for Pointer<Kind> {
    fn from(pointer: jptr) -> Self {
        Self {
            value: unsafe { Box::from_raw(pointer as *mut Kind) },
        }
    }
}

impl<Kind> Deref for Pointer<Kind> {
    type Target = Kind;

    fn deref(&self) -> &Self::Target {
        &self.value
    }
}

pub fn to_value(env: &JNIEnv, jobj: JObject) -> Result<Value, Error> {
    if env.is_instance_of(jobj, INT_CLASS).unwrap_or(false) {
        let jval = env.call_method(jobj, "intValue", "()I", &[])?;
        Ok(Value::I32(
            jval.i().expect("Could not get an integer value."),
        ))
    } else if env.is_instance_of(jobj, LONG_CLASS).unwrap_or(false) {
        let jval = env.call_method(jobj, "longValue", "()J", &[])?;
        Ok(Value::I64(jval.j().expect("Could not get a long value.")))
    } else if env.is_instance_of(jobj, FLOAT_CLASS).unwrap_or(false) {
        let jval = env.call_method(jobj, "floatValue", "()F", &[])?;
        Ok(Value::F32(jval.f().expect("Could not get a float value.")))
    } else if env.is_instance_of(jobj, DOUBLE_CLASS).unwrap_or(false) {
        let jval = env.call_method(jobj, "doubleValue", "()D", &[])?;
        Ok(Value::F64(jval.d().expect("Could not get a double value.")))
    } else {
        Err(runtime_error(format!(
            "Could not convert argument {:?} to a WebAssembly value.",
            jobj
        )))
    }
}
