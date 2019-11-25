use jni::objects::{JClass, JString, JValue};
use jni::sys::jstring;
use jni::JNIEnv;

#[no_mangle]
pub extern "system" fn Java_org_wasmer_Instance_hello(
    env: JNIEnv,
    class: JClass,
    input: JString,
) -> jstring {
    let input: String = env
        .get_string(input)
        .expect("Couldn't get java string!")
        .into();

    if let Ok(JValue::Object(object)) = env.get_field(class.into(), "exports", "Ljava/lang/String;")
    {
        let a: String = env.get_string(object.into()).unwrap().into();
        println!("==> {}", a);
    }

    env.set_field(
        class.into(),
        "exports",
        "Ljava/lang/String;",
        JValue::Object(env.new_string("rust").expect("hop").into()),
    );

    let output = env
        .new_string(format!("Hello, {}!", input))
        .expect("Couldn't create java string!");

    output.into_inner()
}
