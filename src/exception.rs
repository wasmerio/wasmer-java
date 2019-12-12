pub use jni::errors::Error;
use jni::{errors::ErrorKind, JNIEnv};
use std::thread;

pub fn runtime_error(message: String) -> Error {
    Error::from_kind(ErrorKind::Msg(message))
}

pub fn unwrap_or_throw<T>(env: &JNIEnv, result: thread::Result<Result<T, Error>>) -> T
where
    T: Default,
{
    match result {
        Ok(result) => match result {
            Ok(result) => result,
            Err(error) => {
                if !env.exception_check().unwrap() {
                    env.throw_new("java/lang/RuntimeException", &error.to_string())
                        .expect("Cannot throw an `java/lang/RuntimeException` exception.");
                }

                T::default()
            }
        },
        Err(ref error) => {
            env.throw_new("java/lang/RuntimeException", format!("{:?}", error))
                .expect("Cannot throw an `java/lang/RuntimeException` exception.");

            T::default()
        }
    }
}
