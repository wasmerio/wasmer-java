extern  {
    fn mul_from_java(x:i32, y: i32) -> i32;
}

#[no_mangle]
pub extern fn double_each_arg_then_mul(x: i32, y: i32) -> i32 {
    let x = x * 2;
    let y = y * 2;
    unsafe {
        mul_from_java(x, y)
    }
}
