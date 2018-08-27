package com.acme.reactivestreams;

class Looper {
  boolean complete;
  void loop() {
    do {
      // something
    } while (!complete);
  }

  void complete() {
    complete = true;
  }

  public static void main(String[] args) {
    Looper looper = new Looper();
    new Thread(looper::loop).start();
    new Thread(looper::complete).start();
  }
}
