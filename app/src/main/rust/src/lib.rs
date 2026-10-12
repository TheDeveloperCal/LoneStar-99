// Copyright (C) 2026 DeveloperCal
// SPDX-License-Identifier: GPL-3.0-only
// This JNI adapter is original project code. The linked libre99-core and the
// embedded firmware remain separately licensed; see THIRD_PARTY_NOTICES.md.

use jni::objects::{JByteArray, JClass, JFloatArray, JIntArray, JString};
use jni::sys::{jboolean, jint, jlong};
use jni::JNIEnv;
use libre99_core::cartridge::Cartridge;
use libre99_core::keyboard::TiKey;
use libre99_core::machine::Machine;
use std::sync::Mutex;

const ROM: &[u8] = include_bytes!("../assets/console-rom.bin");
const GROM: &[u8] = include_bytes!("../assets/console-grom.bin");
const DISK_DSR: &[u8] = include_bytes!("../assets/disk-dsr.bin");
const FRAME_PIXELS: usize = 256 * 192;

struct Engine {
    machine: Machine,
    pixels: Vec<u32>,
}

fn engine<'a>(handle: jlong) -> Option<&'a Mutex<Engine>> {
    if handle == 0 { None } else { Some(unsafe { &*(handle as *const Mutex<Engine>) }) }
}

#[no_mangle]
pub extern "system" fn Java_com_developercal_ti99pocket_NativeCore_nativeCreate(
    mut _env: JNIEnv,
    _class: JClass,
) -> jlong {
    let mut machine = Machine::new(ROM, GROM);
    machine.load_disk_controller(DISK_DSR);
    machine.set_audio_sample_rate(48_000);
    Box::into_raw(Box::new(Mutex::new(Engine { machine, pixels: vec![0; FRAME_PIXELS] }))) as jlong
}

#[no_mangle]
pub extern "system" fn Java_com_developercal_ti99pocket_NativeCore_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle != 0 { unsafe { drop(Box::from_raw(handle as *mut Mutex<Engine>)); } }
}

#[no_mangle]
pub extern "system" fn Java_com_developercal_ti99pocket_NativeCore_nativeRunFrame(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    output: JIntArray,
) -> jboolean {
    let Some(state) = engine(handle) else { return 0 };
    let Ok(mut state) = state.lock() else { return 0 };
    if env.get_array_length(&output).unwrap_or(0) as usize != FRAME_PIXELS { return 0; }
    state.machine.run_frame();
    let Engine { machine, pixels } = &mut *state;
    machine.render(pixels);
    let frame: Vec<jint> = state.pixels.iter().map(|pixel| (*pixel | 0xFF00_0000) as jint).collect();
    if env.set_int_array_region(&output, 0, &frame).is_err() { 0 } else { 1 }
}

#[no_mangle]
pub extern "system" fn Java_com_developercal_ti99pocket_NativeCore_nativeReset(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if let Some(state) = engine(handle) { if let Ok(mut state) = state.lock() { state.machine.reset(); } }
}

#[no_mangle]
pub extern "system" fn Java_com_developercal_ti99pocket_NativeCore_nativeSetKey(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    key: jint,
    down: jboolean,
) {
    let Some(key) = ti_key(key) else { return };
    if let Some(state) = engine(handle) { if let Ok(mut state) = state.lock() { state.machine.set_key(key, down != 0); } }
}

fn ti_key(code: jint) -> Option<TiKey> {
    use TiKey::*;
    Some(match code {
        0 => A, 1 => B, 2 => C, 3 => D, 4 => E, 5 => F, 6 => G, 7 => H, 8 => I,
        9 => J, 10 => K, 11 => L, 12 => M, 13 => N, 14 => O, 15 => P, 16 => Q,
        17 => R, 18 => S, 19 => T, 20 => U, 21 => V, 22 => W, 23 => X, 24 => Y, 25 => Z,
        26 => Num0, 27 => Num1, 28 => Num2, 29 => Num3, 30 => Num4, 31 => Num5,
        32 => Num6, 33 => Num7, 34 => Num8, 35 => Num9, 36 => Equals, 37 => Period,
        38 => Comma, 39 => Semicolon, 40 => Slash, 41 => Space, 42 => Enter,
        43 => Fctn, 44 => Shift, 45 => Ctrl, 46 => Joy1Fire, 47 => Joy1Up,
        48 => Joy1Down, 49 => Joy1Left, 50 => Joy1Right, _ => return None,
    })
}

#[no_mangle]
pub extern "system" fn Java_com_developercal_ti99pocket_NativeCore_nativeLoadCartridge(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    bytes: JByteArray,
) -> jboolean {
    let Some(state) = engine(handle) else { return 0 };
    let Ok(bytes) = env.convert_byte_array(bytes) else { return 0 };
    match Cartridge::parse(&bytes) {
        Ok(cartridge) => { let Ok(mut state) = state.lock() else { return 0 }; state.machine.mount_cartridge(&cartridge); state.machine.reset(); 1 }
        Err(_) => 0,
    }
}

#[no_mangle]
pub extern "system" fn Java_com_developercal_ti99pocket_NativeCore_nativeLoadDisk(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    drive: jint,
    bytes: JByteArray,
    key: JString,
) -> jboolean {
    let Some(state) = engine(handle) else { return 0 };
    let Ok(bytes) = env.convert_byte_array(bytes) else { return 0 };
    let Ok(key) = env.get_string(&key) else { return 0 };
    let key = key.to_string_lossy();
    if !(0..3).contains(&drive) || bytes.len() < 40 * 9 * 256 || bytes.len() > 4 * 1024 * 1024 { return 0; }
    let Ok(mut state) = state.lock() else { return 0 };
    state.machine.mount_disk_keyed(drive as usize, &key, bytes);
    1
}

#[no_mangle]
pub extern "system" fn Java_com_developercal_ti99pocket_NativeCore_nativeEjectDisk(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    drive: jint,
) -> jboolean {
    let Some(state) = engine(handle) else { return 0 };
    if !(0..3).contains(&drive) { return 0 }
    let Ok(mut state) = state.lock() else { return 0 };
    state.machine.eject_disk(drive as usize);
    1
}

#[no_mangle]
pub extern "system" fn Java_com_developercal_ti99pocket_NativeCore_nativeEjectCartridge(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jboolean {
    let Some(state) = engine(handle) else { return 0 };
    let Ok(mut state) = state.lock() else { return 0 };
    state.machine.eject_cartridge();
    state.machine.reset();
    1
}

#[no_mangle]
pub extern "system" fn Java_com_developercal_ti99pocket_NativeCore_nativeGetDiskImage(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    drive: jint,
) -> jni::sys::jbyteArray {
    let Some(state) = engine(handle) else { return std::ptr::null_mut() };
    if !(0..3).contains(&drive) { return std::ptr::null_mut() }
    let Ok(state) = state.lock() else { return std::ptr::null_mut() };
    let Some(image) = state.machine.bus().disk.drive_image(drive as usize) else { return std::ptr::null_mut() };
    match env.byte_array_from_slice(image) {
        Ok(bytes) => bytes.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

#[no_mangle]
pub extern "system" fn Java_com_developercal_ti99pocket_NativeCore_nativeSaveState(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jni::sys::jbyteArray {
    let Some(state) = engine(handle) else { return std::ptr::null_mut() };
    let Ok(state) = state.lock() else { return std::ptr::null_mut() };
    match env.byte_array_from_slice(&state.machine.save_state()) {
        Ok(bytes) => bytes.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

#[no_mangle]
pub extern "system" fn Java_com_developercal_ti99pocket_NativeCore_nativeLoadState(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    bytes: JByteArray,
) -> jboolean {
    let Some(state) = engine(handle) else { return 0 };
    let Ok(bytes) = env.convert_byte_array(bytes) else { return 0 };
    if bytes.len() < 12 || bytes.len() > 16 * 1024 * 1024 { return 0; }
    let Ok(mut state) = state.lock() else { return 0 };
    state.machine.load_state(&bytes).map_or(0, |_| 1)
}

#[no_mangle]
pub extern "system" fn Java_com_developercal_ti99pocket_NativeCore_nativeFillAudio(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    output: JFloatArray,
) -> jboolean {
    let Some(state) = engine(handle) else { return 0 };
    let Ok(mut state) = state.lock() else { return 0 };
    let Ok(length) = env.get_array_length(&output) else { return 0 };
    let mut samples = vec![0.0f32; length as usize];
    state.machine.fill_audio(&mut samples);
    if env.set_float_array_region(&output, 0, &samples).is_err() { 0 } else { 1 }
}
