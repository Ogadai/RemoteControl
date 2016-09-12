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
    this.max = config.max;
    this.limit = 50;

    this.timer = null;
    this.startTime = null;
    this.direction = null;
  }

  setState(state) {
    this.stop();

    let command = this.getCommand(parseInt(state));
    if (command.direction && command.timeOn > 0) {
      this.moveTo(command);
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

      if (this.gpio) {
	this.setGpio((this.position === -this.limit) ? -1 : ((this.position === this.limit) ? 1 : 0));
      }

      this.direction = null;
    }
  }

  setGpio(offset) {
console.log('steering: ' + offset);
    this.gpio.left.writeSync(offset < 0 ? 1 : 0);
    this.gpio.right.writeSync(offset > 0 ? 1 : 0);
  }

  getCommand(state) {
    // Return the time +ve/-ve required for the motor
    let direction = state < 0 ? 'left' : 'right';

    return {
      direction: state < this.position ? 'left' : 'right',
      timeOn: Math.abs((this.position - state) * this.max / this.limit)
    };
  }

  updatePosition(direction, timeOn) {
    let dist = this.limit * timeOn / this.max,
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