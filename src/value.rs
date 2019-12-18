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

macro_rules! non_null {
    ( $obj:expr, $ctx:expr ) => {
        if $obj.is_null() {
            return Err(ErrorKind::NullPtr($ctx).into());
        } else {
            $obj
        }
    };
}

impl TryFrom<(&JNIEnv<'_>, JObject<'_>)> for Value {
    type Error = Error;

    fn try_from((env, obj): (&JNIEnv, JObject)) -> Result<Self, Self::Error> {
        non_null!(obj, "to_value obj argument");
        if env.is_instance_of(obj, INT_CLASS).unwrap_or(false) {
            Ok(Value {
                value: WasmValue::I32(env.call_method(obj, "intValue", "()I", &[])?.i()?),
            })
        } else if env.is_instance_of(obj, LONG_CLASS).unwrap_or(false) {
            Ok(Value {
                value: WasmValue::I64(env.call_method(obj, "longValue", "()J", &[])?.j()?),
            })
        } else if env.is_instance_of(obj, FLOAT_CLASS).unwrap_or(false) {
            Ok(Value {
                value: WasmValue::F32(env.call_method(obj, "floatValue", "()F", &[])?.f()?),
            })
        } else if env.is_instance_of(obj, DOUBLE_CLASS).unwrap_or(false) {
            Ok(Value {
                value: WasmValue::F64(env.call_method(obj, "doubleValue", "()D", &[])?.d()?),
            })
        } else {
            Err(runtime_error(format!(
                "Could not convert argument {:?} to a WebAssembly value.",
                obj
            )))
        }
    }
}
