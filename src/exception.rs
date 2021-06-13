use jni::JNIEnv;
use std::thread;
use jni::errors::Error as JNIError;

#[derive(Debug)]
pub enum Error {
    JNIError(JNIError),
    Message(String),
}

impl From<JNIError> for Error {
    fn from(err: JNIError) -> Self { Self::JNIError(err) }
}

impl std::fmt::Display for Error {
    fn fmt(&self, fmt: &mut std::fmt::Formatter<'_>) -> Result<(), std::fmt::Error> {
        match self {
            Error::JNIError(err) => err.fmt(fmt),
            Error::Message(msg) => msg.fmt(fmt),
        }
    }
}

pub fn runtime_error(message: String) -> Error {
    Error::Message(message)
}

#[derive(Debug)]
pub enum JOption<T> {
    Some(T),
    None,
}

impl<T> JOption<T> {
    pub fn unwrap_or(self, default: T) -> T {
        match self {
            JOption::Some(result) => result,
            JOption::None => default,
        }
    }
}

pub fn joption_or_throw<T>(env: &JNIEnv, result: thread::Result<Result<T, Error>>) -> JOption<T> {
    match result {
        Ok(result) => match result {
            Ok(result) => JOption::Some(result),
            Err(error) => {
                if !env.exception_check().unwrap() {
                    env.throw_new("java/lang/RuntimeException", &error.to_string())
                        .expect("Cannot throw an `java/lang/RuntimeException` exception.");
                }

                JOption::None
            }
        },
        Err(ref error) => {
            env.throw_new("java/lang/RuntimeException", format!("{:?}", error))
                .expect("Cannot throw an `java/lang/RuntimeException` exception.");

            JOption::None
        }
    }
}
