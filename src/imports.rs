use crate::{
    exception::{joption_or_throw},
    types::{jptr, Pointer},
    module::Module,
};
use jni::{
    sys::{jlong},
    objects::{JClass, JObject, ReleaseMode, AutoArray, TypeArray},
    JNIEnv,
};
use std::{collections::HashMap, panic};
use wasmer::{ImportObject, NamedResolver, ChainableNamedResolver, Exports, Function, FunctionType, Type, Value};
use wasmer_wasi::WasiState;

pub struct Imports {
    pub(crate) import_object: Box<dyn NamedResolver>,
}

fn array2vec<'a, T: TypeArray>(array: &'a AutoArray<T>) -> Vec<&'a T> {
    let len = array.size().unwrap();
    let mut ret = Vec::with_capacity(len as usize);
    for i in 0..len {
        ret.push(unsafe { &*(array.as_ptr().offset(i as isize) as *const T) });
    }
    ret
}


#[no_mangle]
pub extern "system" fn Java_org_wasmer_Imports_nativeImportsInstantiate(
    env: JNIEnv,
    _class: JClass,
    spec: JObject,
    module: jptr,
) -> jptr {
    let output = panic::catch_unwind(|| {
        let mut namespaces = HashMap::<String, _>::new();
        let mut import_object = ImportObject::new();
        let module: &Module = Into::<Pointer<Module>>::into(module).borrow();
        let store = module.module.store();
        let spec = env.get_list(spec).unwrap();
        for import in spec.iter().unwrap() {
            let namespace = env.get_field(import, "namespace", "Ljava/lang/String;").unwrap().l().unwrap();
            let namespace = env.get_string(namespace.into()).unwrap().to_str().unwrap().to_string();
            let name = env.get_field(import, "name", "Ljava/lang/String;").unwrap().l().unwrap();
            let name = env.get_string(name.into()).unwrap().to_str().unwrap().to_string();
            let function = env.get_field(import, "function", "Ljava/util/function/Function;").unwrap().l().unwrap();
            let params = env.get_field(import, "argTypesInt", "[I").unwrap().l().unwrap();
            let returns = env.get_field(import, "retTypesInt", "[I").unwrap().l().unwrap();
            let params = env.get_int_array_elements(*params, ReleaseMode::NoCopyBack).unwrap();
            let returns = env.get_int_array_elements(*returns, ReleaseMode::NoCopyBack).unwrap();
            let i2t = |i: &i32| match i { 1 => Type::I32, 2 => Type::I64, 3 => Type::F32, 4 => Type::F64, _ => unreachable!("Unknown {}", i)};
            let params = array2vec(&params).into_iter().map(i2t).collect::<Vec<_>>();
            let returns = array2vec(&returns).into_iter().map(i2t).collect::<Vec<_>>();
            let sig = FunctionType::new(params.clone(), returns.clone());
            let function = env.new_global_ref(function).unwrap();
            let jvm = env.get_java_vm().unwrap();
            namespaces.entry(namespace).or_insert_with(|| Exports::new()).insert(name, Function::new(store, sig, move |argv| {
                // There is many ways of transferring the args from wasm to java, JList being the cleanes,
                // but probably also slowest by far (two JNI calls per argument). Benchmark?
                let env = jvm.get_env().unwrap();
                env.ensure_local_capacity(argv.len() as i32 + 2).ok();
                let jargv = env.new_long_array(argv.len() as i32).unwrap();
                let argv = argv.into_iter().enumerate().map(|(i, arg)| match arg {
                    Value::I32(arg) => { assert!(params[i] == Type::I32); *arg as i64 },
                    Value::I64(arg) => { assert!(params[i] == Type::I64); *arg as i64 },
                    Value::F32(arg) => { assert!(params[i] == Type::F32); arg.to_bits() as i64 },
                    Value::F64(arg) => { assert!(params[i] == Type::F64); arg.to_bits() as i64 },
                    _ => panic!("Argument of unsupported type {:?}", arg)
                }).collect::<Vec<jlong>>();
                env.set_long_array_region(jargv, 0, &argv).unwrap();
                let jret = env.call_method(function.as_obj(), "apply", "(Ljava/lang/Object;)Ljava/lang/Object;", &[jargv.into()]).unwrap().l().unwrap();
                let ret = match returns.len() {
                    0 => vec![],
                    len => {
                        let mut ret = vec![0; len];
                        env.get_long_array_region(*jret, 0, &mut ret).unwrap();
                        ret.into_iter().enumerate().map(|(i, ret)| match returns[i] {
                            Type::I32 => Value::I32(ret as i32),
                            Type::I64 => Value::I64(ret as i64),
                            Type::F32 => Value::F32(f32::from_bits(ret as u32)),
                            Type::F64 => Value::F64(f64::from_bits(ret as u64)),
                            t => panic!("Return of unsupported type {:?}", t)
                        }).collect()
                    }
                };
                // TODO: Error handling
                Ok(ret)
            }));
        }
        for (namespace, exports) in namespaces.into_iter() {
            import_object.register(namespace, exports);
        }
        let import_object = Box::new(import_object);

        Ok(Pointer::new(Imports { import_object }).into())
    });

    joption_or_throw(&env, output).unwrap_or(0)
}

#[no_mangle]
pub extern "system" fn Java_org_wasmer_Imports_nativeImportsWasi(
    env: JNIEnv,
    _class: JClass,
    module: jptr,
) -> jptr {
    let output = panic::catch_unwind(|| {
        // TODO: When getting serious about this, one might have to expose the wasi builder... :/
        let module: &Module = Into::<Pointer<Module>>::into(module).borrow();
        let mut wasi = WasiState::new("").finalize().unwrap();
        let import_object = wasi.import_object(&module.module).unwrap();
        let import_object = Box::new(import_object);

        Ok(Pointer::new(Imports { import_object }).into())
    });

    joption_or_throw(&env, output).unwrap_or(0)
}

#[no_mangle]
pub extern "system" fn Java_org_wasmer_Imports_nativeImportsChain(
    env: JNIEnv,
    _class: JClass,
    back: jptr,
    front: jptr,
) -> jptr {
    let output = panic::catch_unwind(|| {

        let back: &Imports = Into::<Pointer<Imports>>::into(back).borrow();
        let front: &Imports = Into::<Pointer<Imports>>::into(front).borrow();
        let import_object = Box::new((&back.import_object).chain_front(&front.import_object));

        Ok(Pointer::new(Imports { import_object }).into())
    });

    joption_or_throw(&env, output).unwrap_or(0)

}

#[no_mangle]
pub extern "system" fn Java_org_wasmer_Imports_nativeDrop(
    _env: JNIEnv,
    _class: JClass,
    imports_pointer: jptr,
) {
    let _: Pointer<Imports> = imports_pointer.into();
}
