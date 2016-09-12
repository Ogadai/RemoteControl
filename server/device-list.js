"use strict"

const EventEmitter = require('events'),
      deviceType = {
        onoff: require('./devices/onoff'),
        steering: require('./devices/steering'),
        camera: require('./devices/camera')
      };

class DeviceList extends EventEmitter {
  constructor(configList, nodeName) {
    super();
    this.devices = {};

    configList.forEach(config => {
        let device = new deviceType[config.type](config, nodeName);
        device.on('changed', state => { this.emit('changed', config.name, state); })
        device.on('video', data => { this.emit('video', data); })

        this.devices[config.name] = device;
        console.log('Configured device "' + config.name + '"');
    });
  }

  getDevice(name) {
    return this.devices[name];
  }

  reportInitialStates() {
    for(var name in devices) {
      if (devices[name].reportInitialState) {
        devices[name].reportInitialState();
      }
    }
  }

  disconnectAll() {
    for(var name in devices) {
      if (devices[name].disconnect) {
        devices[name].disconnect();
      }
    }
  }
};
module.exports = DeviceList;
