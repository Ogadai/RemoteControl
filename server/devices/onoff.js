"use strict"

const EventEmitter = require('events'),
      Gpio = require('./gpio');

class OnOffDevice extends EventEmitter {
  constructor(config) {
    super();

    if (Gpio) {
      this.pin = config.gpio.pin;
      this.gpio = new Gpio(config.gpio.pin, 'out');
    }
  }

  setState(state) {
    if (this.gpio) {
      this.gpio.writeSync(state == 'on' ? 1 : 0);
    }
  }

  reset() {
    setState('off');
  }

  disconnect() {
    if (this.gpio) {
      this.gpio.unexport();
      this.gpio = null;
    }
  }
}
module.exports = OnOffDevice;