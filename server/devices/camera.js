"use strict"

const EventEmitter = require('events'),
      extend = require('extend'),
      RaspiCam = require('raspicam'),
      RaspiMock = require('./mock/raspicam-mock');

class CameraDevice extends EventEmitter {
  constructor(config) {
    super();

    this.running = false;
    this.mock = config.mock;
    this.options = extend({ mode: 'video', output: '-' }, config.options);
  }

  setState(state) {
    const turnOn = (state === 'on');
    if (this.running !== turnOn) {
      if (turnOn)
        this.start();
      else
        this.stop();
    } else if (this.running) {
        this.stop();
        setTimeout(() => {
          this.start()
        }, 100);
    }
  }

  reset() {
    if (this.running) {
      this.stop();
    }
  }

  disconnect() {
    reset();
  }

  start() {
    this.raspicam = this.mock ? new RaspiMock(this.options) : new RaspiCam(this.options);

    this.raspicam.on('start', (e, t, stream) => {
      console.log('video started');

      stream.on('data', data => {
        try {
          this.emit('video', data);
        } catch (ex) {
          console.error('Error sending stream data - ' + ex);
          this.stop();
        }
      })
    });

    this.raspicam.start();
    this.running = true;
  }

  stop() {
    console.log('video finished');
    this.raspicam.stop();
    this.running = false;
  }
}
module.exports = CameraDevice;