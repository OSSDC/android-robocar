package com.zugaldia.robocar.app;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

import com.zugaldia.robocar.hardware.adafruit2348.AdafruitDCMotor;
import com.zugaldia.robocar.hardware.adafruit2348.AdafruitMotorHat;
import com.zugaldia.robocar.software.controller.nes30.NES30Connection;
import com.zugaldia.robocar.software.controller.nes30.NES30Listener;
import com.zugaldia.robocar.software.controller.nes30.NES30Manager;
import com.zugaldia.robocar.software.webserver.LocalWebServer;
import com.zugaldia.robocar.software.webserver.RequestListener;
import com.zugaldia.robocar.software.webserver.models.RobocarMove;
import com.zugaldia.robocar.software.webserver.models.RobocarResponse;
import com.zugaldia.robocar.software.webserver.models.RobocarStatus;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity
    implements NES30Listener, RequestListener {

  // Set the speed, from 0 (off) to 255 (max speed)
  private static final int MOTOR_SPEED = 255;

  private NES30Manager nes30Manager;
  private NES30Connection nes30Connection;
  private boolean isMoving = false;

  private AdafruitMotorHat motorHat;
  private AdafruitDCMotor motorFrontLeft;
  private AdafruitDCMotor motorFrontRight;
  private AdafruitDCMotor motorBackLeft;
  private AdafruitDCMotor motorBackRight;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Controller
    nes30Manager = new NES30Manager(this);

    // Motors
    motorHat = new AdafruitMotorHat();
    motorFrontLeft = motorHat.getMotor(1);
    motorFrontLeft.setSpeed(MOTOR_SPEED);
    motorBackLeft = motorHat.getMotor(2);
    motorBackLeft.setSpeed(MOTOR_SPEED);
    motorFrontRight = motorHat.getMotor(3);
    motorFrontRight.setSpeed(MOTOR_SPEED);
    motorBackRight = motorHat.getMotor(4);
    motorBackRight.setSpeed(MOTOR_SPEED);

    // Local web server
    setupWebServer();

    // NES30 BT connection
    setupBluetooth();
  }

  private void setupWebServer() {
    LocalWebServer localWebServer = new LocalWebServer(this);
    try {
      localWebServer.start();
    } catch (IOException e) {
      Timber.e(e, "Failed to start local web server.");
    }
  }

  private void setupBluetooth() {
    nes30Connection = new NES30Connection(
        this, RobocarConstants.NES30_NAME, RobocarConstants.NES30_MAC_ADDRESS);

    Timber.d("BT status: %b", nes30Connection.isEnabled());
    Timber.d("Paired devices: %d", nes30Connection.getPairedDevices().size());
    BluetoothDevice nes30device = nes30Connection.isPaired();
    if (nes30device == null) {
      Timber.d("Starting discovery: %b", nes30Connection.startDiscovery());
    } else {
      Timber.d("Creating bond: %b", nes30Connection.createBond(nes30device));
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    release();
    motorHat.close();
    nes30Connection.cancelDiscovery();
  }

  /*
   * Handle keyboard (controller) events
   */

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    return nes30Manager.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onKeyLongPress(int keyCode, KeyEvent event) {
    return nes30Manager.onKeyLongPress(keyCode, event);
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    return nes30Manager.onKeyUp(keyCode, event);
  }

  @Override
  public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
    return nes30Manager.onKeyMultiple(keyCode, count, event);
  }

  @Override
  public void onKeyPress(@NES30Manager.ButtonCode int keyCode, boolean isDown) {
    if (isDown && isMoving) {
      // We're already moving, no need send further instructions to the engine.
      // One way to improve this is to tie this event with instructions to the engine
      // to increase its speed (acceleration).
      return;
    } else {
      // We start moving the moment the key is pressed
      isMoving = isDown;
      if (!isMoving) {
        // And we stop when the key is released
        release();
        return;
      }
    }

    switch (keyCode) {
      case NES30Manager.BUTTON_UP_CODE:
        moveForward();
        break;
      case NES30Manager.BUTTON_DOWN_CODE:
        moveBackward();
        break;
      case NES30Manager.BUTTON_LEFT_CODE:
        turnLeft();
        break;
      case NES30Manager.BUTTON_RIGHT_CODE:
        turnRight();
        break;
      case NES30Manager.BUTTON_KONAMI:
        // Do your magic here ;-)
        break;
    }
  }

  private void moveForward() {
    Timber.d("Moving forward.");
    motorFrontLeft.run(AdafruitMotorHat.FORWARD);
    motorFrontRight.run(AdafruitMotorHat.BACKWARD);
    motorBackLeft.run(AdafruitMotorHat.BACKWARD);
    motorBackRight.run(AdafruitMotorHat.FORWARD);
  }

  private void moveBackward() {
    Timber.d("Moving backward.");
    motorFrontLeft.run(AdafruitMotorHat.BACKWARD);
    motorFrontRight.run(AdafruitMotorHat.FORWARD);
    motorBackLeft.run(AdafruitMotorHat.FORWARD);
    motorBackRight.run(AdafruitMotorHat.BACKWARD);
  }

  private void turnLeft() {
    Timber.d("Turning left.");
    motorFrontLeft.run(AdafruitMotorHat.BACKWARD);
    motorFrontRight.run(AdafruitMotorHat.BACKWARD);
    motorBackLeft.run(AdafruitMotorHat.FORWARD);
    motorBackRight.run(AdafruitMotorHat.FORWARD);
  }

  private void turnRight() {
    Timber.d("Turning right.");
    motorFrontLeft.run(AdafruitMotorHat.FORWARD);
    motorFrontRight.run(AdafruitMotorHat.FORWARD);
    motorBackLeft.run(AdafruitMotorHat.BACKWARD);
    motorBackRight.run(AdafruitMotorHat.BACKWARD);
  }

  private void release() {
    Timber.d("Release.");
    motorFrontLeft.run(AdafruitMotorHat.RELEASE);
    motorFrontRight.run(AdafruitMotorHat.RELEASE);
    motorBackLeft.run(AdafruitMotorHat.RELEASE);
    motorBackRight.run(AdafruitMotorHat.RELEASE);
  }

  @Override
  public void onRequest(NanoHTTPD.IHTTPSession session) {
    LocalWebServer.logSession(session);
  }

  @Override
  public RobocarStatus onStatus() {
    return new RobocarStatus(200, "OK");
  }

  @Override
  public RobocarResponse onMove(RobocarMove move) {
    return new RobocarResponse(200, "TODO");
  }

}
