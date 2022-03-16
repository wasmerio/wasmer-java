use std::ffi::{CStr, CString};
use std::mem;
use std::os::raw::{c_char, c_void};

extern  {
    fn get_greet_msg_len_from_java(msg_key_ptr: *mut c_char) -> i32;
    fn greet_from_java(msg_key_ptr: *mut c_char, msg_ptr: *mut c_char) -> *mut c_char;
}

#[no_mangle]
pub extern fn greet(msg_key_ptr: *mut c_char) -> *mut c_char {
    let msg_len;
    unsafe { 
        msg_len = get_greet_msg_len_from_java(msg_key_ptr) as usize;
    }
    let msg_ptr = allocate(msg_len) as *mut c_char;
    unsafe {
        greet_from_java(msg_key_ptr, msg_ptr);
    }
    let msg;
    unsafe { 
        msg = CStr::from_ptr(msg_ptr).to_str().unwrap();
    }
    let string = CString::new(format!("Hello, {}!", msg)).unwrap();
    deallocate(msg_ptr as *mut c_void, msg_len);
    string.into_raw()
}

#[no_mangle]
pub extern fn drop_string(ptr: *mut c_char) {
    unsafe {
        let _ = CString::from_raw(ptr);
    }
}

#[no_mangle]
pub extern  fn allocate(size: usize) -> *mut c_void {
    let mut buffer = Vec::with_capacity(size);
    let ptr = buffer.as_mut_ptr();
    mem::forget(buffer);

    ptr as *mut c_void
}

#[no_mangle]
pub extern  fn deallocate(ptr: *mut c_void, capacity: usize) {
    unsafe {
        let _ = Vec::from_raw_parts(ptr, 0, capacity);
    }
}