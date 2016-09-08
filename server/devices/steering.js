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

console.log(command.direction + ' for ' + command.timeOn);

    if (this.gpio) {
      this.gpio[command.direction].writeSync(1);
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
        this.gpio.left.writeSync(this.position === -100 ? 1 : 0);
        this.gpio.right.writeSync(this.position === 100 ? 1 : 0);
      }

      this.direction = null;
    }
  }

  getCommand(state) {
    // Return the time +ve/-ve required for the motor
    let direction = state < 0 ? 'left' : 'right';

    return {
      direction: state < this.position ? 'left' : 'right',
      timeOn: Math.abs((this.position - state) * this.max / 100)
    };
  }

  updatePosition(direction, timeOn) {
    let dist = 100 * timeOn / this.max,
        sign = direction === 'left' ? -1 : 1,
        newPosition = this.position + sign * dist;
    
    if (newPosition > 100)
      newPosition = 100
    else if (newPosition < -100)
      newPosition = -100

    this.position = newPosition;
console.log('position: ' + this.position);
  }
}
module.exports = SteeringDevice;