"use strict"

const EventEmitter = require('events'),
      Gpio = require('./gpio');

const INTERVAL = 10;
const INTERVAL_STEPS = 10;

const MAX_TURN = 30;
const SPIN_THRESHOLD = 0.5;

class DriveDevice extends EventEmitter {
  constructor(config) {
    super();

    this.state = {
      left: 0,
      right: 0
    }
    this.interval = null;
    this.intervalStep = 0;
    this.timeout = null;

    if (Gpio) {
      const left = config.gpio.left;
      const right = config.gpio.right;

      this.gpio = {
        left: {
          forward: new Gpio(left.forward, 'out'),
          back: new Gpio(left.back, 'out')
        },
        right: {
          forward: new Gpio(right.forward, 'out'),
          back: new Gpio(right.back, 'out')
        }
      };
    }
  }

  disconnect() {
    if (this.gpio) {
      this.gpio.left.forward.unexport();
      this.gpio.left.back.unexport();
      this.gpio.right.forward.unexport();
      this.gpio.right.back.unexport();
      this.gpio = null;
    }
  }

  setState({ speed, turn }) {
    const state = {
      left: 0,
      right: 0
    };

    const turnSign = turn < 0 ? -1 : 1;
    const turnRate = Math.min(Math.abs(turn), MAX_TURN) / MAX_TURN;

    if (speed === 0) {
      // turning
      if (turnRate > SPIN_THRESHOLD) {
        const spin = (turnRate - SPIN_THRESHOLD) / (1 - SPIN_THRESHOLD)
        state.left = turnSign * spin;
        state.right = turnSign * spin * -1;
      }
    } else {
      // driving
      const speedSign = speed < 0 ? -1 : 1;
      const speedRate = Math.abs(speed);
      state.left = speed + speedSign * speedRate * turnSign * turnRate;
      state.right = speed - speedSign * speedRate * turnSign * turnRate;
    }
    
    const limit = value => Math.max(Math.min(Math.round(value * 100) / 100, 1), -1);
    this.state = {
      left: limit(state.left),
      right: limit(state.right)
    };

    this.checkInterval();

    // Cancel movement if no class for 1 second
    if (this.timeout) clearTimeout(this.timeout);
    this.timeout = setTimeout(() => {
      this.setState({ speed: 0, turn: 0 });
    }, 1000);
  }

  checkInterval() {
    if (this.state.left !== 0 || this.state.left !== 0) {
      if (!this.interval) {
        this.interval = setInterval(() => this.onInterval(), INTERVAL);
      }
    } else if (this.interval) {
      clearInterval(this.interval);
      this.interval = null;
    }
  }

  onInterval() {
    this.intervalStep++;
    if (this.intervalStep >= INTERVAL_STEPS) {
      this.intervalStep = INTERVAL_STEPS;
    }

    if (this.gpio) {
      this.setMotor(this.state.left, this.gpio.left);
      this.setMotor(this.state.right, this.gpio.right);
    } else {
      console.log(`Left: ${this.state.left}, Right: ${this.state.right}`);
    }
  }

  setMotor(speed, { forward, back }) {
    const pin = speed < 0 ? back : forward;
    const altPin = speed < 0 ? forward : back;

    pin.writeSync(this.intervalStep <= Math.abs(speed) * INTERVAL_STEPS ? 1 : 0);
    altPin.writeSync(0);
  }
}
module.exports = DriveDevice;
