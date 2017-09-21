"use strict"

const EventEmitter = require('events'),
      Gpio = require('./gpio');

class SteeringDevice extends EventEmitter {
  constructor(config) {
    super();

    if (Gpio) {
      this.gpio = {
        left: new Gpio(config.gpio.left, 'out'),
        right: new Gpio(config.gpio.right, 'out')
      };
    }

    this.position = 0;
    this.max = config.max || 100;
    this.limit = config.limit || 100;
    this.duration = 0;

    this.timer = null;
    this.startTime = null;
    this.direction = null;
  }

  setState(state) {
    this.stop();

    if (state.startsWith('duration:')) {
      this.duration = parseInt(state.substring(9));
    } else if (state === "right") {
      if (this.gpio) this.setGpio(1);
    } else if (state === "left") {
      if (this.gpio) this.setGpio(-1);
    } else if (state === "off") {
      if (this.gpio) this.setGpio(0);
    } else {
      let command = this.getCommand(parseInt(state));
      if (command.direction && command.timeOn > 0) {
        this.moveTo(command);
      } else if (this.gpio) {
        this.setGpio(0);
      }
    }
  }

  disconnect() {
    if (this.gpio) {
      this.gpio.left.unexport();
      this.gpio.right.unexport();
      this.gpio = null;
    }
  }

  moveTo(command) {
    this.startTime = new Date().getTime();
    this.direction = command.direction;

    if (this.gpio) {
      this.setGpio(command.direction === 'left' ? -1 : 1);
    }

    this.timer = setTimeout(() => {
      this.timer = null;
      this.stop();

      if (this.gpio) {
  	    this.setGpio(0);
      }
    }, command.timeOn);
  }

  stop() {
    // Stop any current movement
    if (this.timer) {
      clearTimeout(this.timer);
      this.timer = null;
    }

    if (this.direction) {
      let endTime = new Date().getTime();
      this.updatePosition(this.direction, endTime - this.startTime);

      this.direction = null;
    }
  }

  setGpio(offset) {
    this.gpio.left.writeSync(offset < 0 ? 1 : 0);
    this.gpio.right.writeSync(offset > 0 ? 1 : 0);
  }

  getCommand(state) {
    // Return the time +ve/-ve required for the motor
    let direction = state < 0 ? 'left' : 'right';
    let max = this.duration > 0 ? this.duration : this.max;

    return {
      direction: state < this.position ? 'left' : 'right',
      timeOn: Math.abs((this.position - state) * max / this.limit)
    };
  }

  updatePosition(direction, timeOn) {
    let max = this.duration > 0 ? this.duration : this.max;
    let dist = this.limit * timeOn / max,
        sign = direction === 'left' ? -1 : 1,
        newPosition = this.position + sign * dist;

    if (newPosition > this.limit)
      newPosition = this.limit
    else if (newPosition < -this.limit)
      newPosition = -this.limit

    this.position = newPosition;
  }
}
module.exports = SteeringDevice;