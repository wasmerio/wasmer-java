use crate::exception::{runtime_error, Error};
use jni::{errors::ErrorKind, objects::JObject, JNIEnv};
use std::convert::TryFrom;
use wasmer_runtime::Value as WasmValue;

pub struct Value {
    pub value: WasmValue,
}

const INT_CLASS: &str = "java/lang/Integer";
const LONG_CLASS: &str = "java/lang/Long";
const FLOAT_CLASS: &str = "java/lang/Float";
const DOUBLE_CLASS: &str = "java/lang/Double";

impl TryFrom<(&JNIEnv<'_>, JObject<'_>)> for Value {
    type Error = Error;

    fn try_from((env, jobject): (&JNIEnv, JObject)) -> Result<Self, Self::Error> {
        if jobject.is_null() {
            return Err(ErrorKind::NullPtr("`try_from` receives a null object").into());
        }

        if env.is_instance_of(jobject, INT_CLASS).unwrap_or(false) {
            Ok(Value {
                value: WasmValue::I32(env.call_method(jobject, "intValue", "()I", &[])?.i()?),
            })
        } else if env.is_instance_of(jobject, LONG_CLASS).unwrap_or(false) {
            Ok(Value {
                value: WasmValue::I64(env.call_method(jobject, "longValue", "()J", &[])?.j()?),
            })
        } else if env.is_instance_of(jobject, FLOAT_CLASS).unwrap_or(false) {
            Ok(Value {
                value: WasmValue::F32(env.call_method(jobject, "floatValue", "()F", &[])?.f()?),
            })
        } else if env.is_instance_of(jobject, DOUBLE_CLASS).unwrap_or(false) {
            Ok(Value {
                value: WasmValue::F64(env.call_method(jobject, "doubleValue", "()D", &[])?.d()?),
            })
        } else {
            Err(runtime_error(format!(
                "Could not convert argument {:?} to a WebAssembly value.",
                jobject
            )))
        }
    }
}
