package com.intiku.hid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private BluetoothHidManager hidManager;
    private EditText hiddenInput;
    private View trackpadArea;
    private View scrollZone;
    
    private float lastX, lastY;
    private GestureDetector gestureDetector;
    private boolean isScrolling = false;

    // Modifier Byte untuk tombol CTRL, ALT, dll.
    private byte currentModifier = 0x00;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inisialisasi Bluetooth Core
        hidManager = new BluetoothHidManager(this);

        hiddenInput = findViewById(R.id.hiddenInput);
        trackpadArea = findViewById(R.id.trackpadArea);
        scrollZone = findViewById(R.id.scrollZone);

        setupKeyboard();
        setupMouse();
        setupExtraKeys();
    }

    private void setupKeyboard() {
        Button btnShowKeyboard = findViewById(R.id.btnShowKeyboard);
        btnShowKeyboard.setOnClickListener(v -> {
            hiddenInput.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(hiddenInput, InputMethodManager.SHOW_IMPLICIT);
        });

        hiddenInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    char c = s.charAt(s.length() - 1);
                    sendCharAsHid(c);
                    
                    hiddenInput.removeTextChangedListener(this);
                    hiddenInput.setText("");
                    hiddenInput.addTextChangedListener(this);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupMouse() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (!isScrolling) {
                    // Klik Kiri: Byte 1 (0x01)
                    hidManager.sendMouseReport((byte) 0x01, (byte) 0, (byte) 0, (byte) 0);
                    // Release (Lepas Klik)
                    hidManager.sendMouseReport((byte) 0x00, (byte) 0, (byte) 0, (byte) 0);
                }
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                // Klik Kanan: Byte 2 (0x02)
                hidManager.sendMouseReport((byte) 0x02, (byte) 0, (byte) 0, (byte) 0);
                hidManager.sendMouseReport((byte) 0x00, (byte) 0, (byte) 0, (byte) 0);
            }
        });

        trackpadArea.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = x;
                    lastY = y;
                    isScrolling = (x >= trackpadArea.getWidth() - scrollZone.getWidth());
                    break;
                    
                case MotionEvent.ACTION_MOVE:
                    float deltaX = x - lastX;
                    float deltaY = y - lastY;
                    
                    if (isScrolling) {
                        // Scroll Wheel, sumbu Y
                        byte wheel = (byte) (deltaY * -0.1f);
                        hidManager.sendMouseReport((byte) 0x00, (byte) 0, (byte) 0, wheel);
                    } else {
                        // Pergerakan Kursor
                        hidManager.sendMouseReport((byte) 0x00, (byte) deltaX, (byte) deltaY, (byte) 0);
                    }
                    
                    lastX = x;
                    lastY = y;
                    break;
                    
                case MotionEvent.ACTION_UP:
                    isScrolling = false;
                    break;
            }
            return true;
        });
    }

    private void setupExtraKeys() {
        // Mapping Tombol Arah & Spesial berdasarkan standar HID Usage Tables
        findViewById(R.id.btnLeft).setOnClickListener(v -> hidManager.sendKeyboardReport(currentModifier, (byte) 0x50)); // Left Arrow
        findViewById(R.id.btnRight).setOnClickListener(v -> hidManager.sendKeyboardReport(currentModifier, (byte) 0x4F)); // Right Arrow
        findViewById(R.id.btnUp).setOnClickListener(v -> hidManager.sendKeyboardReport(currentModifier, (byte) 0x52)); // Up Arrow
        findViewById(R.id.btnDown).setOnClickListener(v -> hidManager.sendKeyboardReport(currentModifier, (byte) 0x51)); // Down Arrow
        
        findViewById(R.id.btnHome).setOnClickListener(v -> hidManager.sendKeyboardReport(currentModifier, (byte) 0x4A)); // Home
        findViewById(R.id.btnEnd).setOnClickListener(v -> hidManager.sendKeyboardReport(currentModifier, (byte) 0x4D)); // End

        // Toggle CTRL
        Button btnCtrl = findViewById(R.id.btnCtrl);
        btnCtrl.setOnClickListener(v -> {
            if (currentModifier == 0x01) { // 0x01 adalah Left CTRL
                currentModifier = 0x00; // Matikan
                btnCtrl.setText("Ctrl");
            } else {
                currentModifier = 0x01; // Hidupkan
                btnCtrl.setText("Ctrl (ON)");
            }
        });
    }

    private void sendCharAsHid(char c) {
        c = Character.toLowerCase(c);
        byte scanCode = 0;
        // Peta sederhana huruf a-z
        if (c >= 'a' && c <= 'z') {
            scanCode = (byte) (c - 'a' + 0x04);
        } else if (c == ' ') {
            scanCode = 0x2C;
        } else if (c == '\n') {
            scanCode = 0x28; // Enter
        }
        
        if (scanCode != 0) {
            hidManager.sendKeyboardReport(currentModifier, scanCode);
        }
    }
}