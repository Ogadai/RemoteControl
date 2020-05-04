"use strict"

const EventEmitter = require('events'),
      deviceType = {
        onoff: require('./devices/onoff'),
        steering: require('./devices/steering'),
        camera: require('./devices/camera'),
        drive: require('./devices/drive')
      };

class DeviceList extends EventEmitter {
  constructor(configList, nodeName) {
    super();
    this.devices = {};

    configList.forEach(config => {
        let device = new deviceType[config.type](config, nodeName);
        device.on('changed', (state, options) => { this.emit('changed', config.name, state, options); })
        device.on('video', data => { this.emit('video', data); })

        this.devices[config.name] = device;
        console.log('Configured device "' + config.name + '"');
    });
  }

  getDevice(name) {
    return this.devices[name];
  }

  reportInitialStates() {
    for(var name in this.devices) {
      if (this.devices[name].reportInitialState) {
        this.devices[name].reportInitialState();
      }
    }
  }

  reset() {
    for(var name in this.devices) {
      if (this.devices[name].reset) {
        this.devices[name].reset();
      }
    }
  }

  disconnectAll() {
    for(var name in this.devices) {
      if (this.devices[name].disconnect) {
        this.devices[name].disconnect();
      }
    }
  }
};
module.exports = DeviceList;
