package com.intiku.hid;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppQosSettings;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import java.util.concurrent.Executors;

@SuppressLint("MissingPermission")
public class BluetoothHidManager {

    private static final String TAG = "HID_MANAGER";
    private Context context;
    private BluetoothHidDevice hidDevice;
    private BluetoothDevice connectedHost;

    // Report ID untuk membedakan sinyal Keyboard dan Mouse
    private static final byte ID_KEYBOARD = 1;
    private static final byte ID_MOUSE = 2;

    // Descriptor yang memberitahu PC: "Saya adalah Keyboard dan Mouse"
// Descriptor yang memberitahu PC: "Saya adalah Keyboard dan Mouse"
    private static final byte[] HID_DESCRIPTOR = {
        // KEYBOARD
        0x05, 0x01, 0x09, 0x06, (byte) 0xa1, 0x01, (byte) 0x85, ID_KEYBOARD,
        0x05, 0x07, 0x19, (byte) 0xe0, 0x29, (byte) 0xe7, 0x15, 0x00, 0x25, 0x01,
        0x75, 0x01, (byte) 0x95, 0x08, (byte) 0x81, 0x02, (byte) 0x95, 0x01, 0x75, 0x08,
        (byte) 0x81, 0x01, (byte) 0x95, 0x05, 0x75, 0x01, 0x05, 0x08, 0x19, 0x01,
        0x29, 0x05, (byte) 0x91, 0x02, (byte) 0x95, 0x01, 0x75, 0x03, (byte) 0x91, 0x01,
        (byte) 0x95, 0x06, 0x75, 0x08, 0x15, 0x00, 0x25, 0x65, 0x05, 0x07, 0x19, 0x00,
        0x29, 0x65, (byte) 0x81, 0x00, (byte) 0xc0,

        // MOUSE
        0x05, 0x01, 0x09, 0x02, (byte) 0xa1, 0x01, (byte) 0x85, ID_MOUSE,
        0x09, 0x01, (byte) 0xa1, 0x00, 0x05, 0x09, 0x19, 0x01, 0x29, 0x03,
        0x15, 0x00, 0x25, 0x01, (byte) 0x95, 0x03, 0x75, 0x01, (byte) 0x81, 0x02,
        (byte) 0x95, 0x01, 0x75, 0x05, (byte) 0x81, 0x03, 0x05, 0x01, 0x09, 0x30,
        0x09, 0x31, 0x09, 0x38, 0x15, (byte) 0x81, 0x25, 0x7f, 0x75, 0x08,
        (byte) 0x95, 0x03, (byte) 0x81, 0x06, (byte) 0xc0, (byte) 0xc0
    };

    public BluetoothHidManager(Context context) {
        this.context = context;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            BluetoothAdapter.getDefaultAdapter().getProfileProxy(context, new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    if (profile == BluetoothProfile.HID_DEVICE) {
                        hidDevice = (BluetoothHidDevice) proxy;
                        registerApp();
                    }
                }
                @Override
                public void onServiceDisconnected(int profile) {
                    if (profile == BluetoothProfile.HID_DEVICE) hidDevice = null;
                }
            }, BluetoothProfile.HID_DEVICE);
        }
    }

    private void registerApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && hidDevice != null) {
            BluetoothHidDeviceAppSdpSettings sdp = new BluetoothHidDeviceAppSdpSettings(
                "Intiku HID", "Intiku App", "Intiku", (byte) 0, HID_DESCRIPTOR
            );
            hidDevice.registerApp(sdp, null, null, Executors.newSingleThreadExecutor(), new BluetoothHidDevice.Callback() {
                @Override
                public void onConnectionStateChanged(BluetoothDevice device, int state) {
                    if (state == BluetoothProfile.STATE_CONNECTED) {
                        connectedHost = device;
                    } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                        connectedHost = null;
                    }
                }
            });
        }
    }

    public void sendKeyboardReport(byte modifier, byte scanCode) {
        if (hidDevice == null || connectedHost == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return;
        byte[] report = {modifier, 0, scanCode, 0, 0, 0, 0, 0};
        hidDevice.sendReport(connectedHost, ID_KEYBOARD, report);
        
        // Kirim report kosong (release tombol) agar tidak nyangkut
        byte[] release = {0, 0, 0, 0, 0, 0, 0, 0};
        hidDevice.sendReport(connectedHost, ID_KEYBOARD, release);
    }

    public void sendMouseReport(byte buttons, byte dx, byte dy, byte dWheel) {
        if (hidDevice == null || connectedHost == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return;
        byte[] report = {buttons, dx, dy, dWheel};
        hidDevice.sendReport(connectedHost, ID_MOUSE, report);
    }
}