use jni::{errors::Error as JniError, JNIEnv};
use std::thread;

pub(crate) fn unwrap_or_throw<T>(env: &JNIEnv, result: thread::Result<Result<T, JniError>>) -> T
where
    T: Default,
{
    match result {
        Ok(result) => match result {
            Ok(result) => result,
            Err(jni_error) => {
                if !env.exception_check().unwrap() {
                    env.throw_new("java/lang/RuntimeException", &jni_error.to_string())
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
