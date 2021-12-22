"use strict"

const WebSocketServer = require('websocket').server,
    DeviceList = require('./device-list'),
    settings = require('./settings');

const deviceList = new DeviceList(settings.devices, 'RC');
const redLight = deviceList.getDevice('red-light');
if (redLight) {
    redLight.setState('on');
}
let socketIndex = 0;

module.exports = function(server) {
  const wsServer = new WebSocketServer({
    httpServer: server,
    autoAcceptConnections: false
  });
  
  wsServer.on('request', request => {
    console.log(`Web Socket opened from ${request.origin} for ${request.resource}`);
    let connection = request.accept('echo-protocol', request.origin);
    const thisIndex = ++socketIndex;
  
    let redLightInterval;
    let redOn = false;
    if (redLight) {
      redLightInterval = setInterval(() => {
        redOn = !redOn;
        redLight.setState(redOn ? 'on' : 'off');
      }, 500);
    }
  
    connection.on('message', message => {
      if (message.type === 'utf8') {
        try {
          const decodedMsg = JSON.parse(message.utf8Data),
              device = deviceList.getDevice(decodedMsg.name),
              state = decodedMsg.state,
              options = { ...decodedMsg.options, socketIndex: thisIndex };

          if (device) {
            device.setState(state, options);
            if (settings.debug) console.log(decodedMsg.name + ' set: ' + JSON.stringify(state));
          } else {
            console.log('unknown device: ', decodedMsg);
          }
        } catch(e) {
          console.error('error processing message: ', message.utf8Data);
          console.error(e);
        }
      }
      else if (message.type === 'binary') {
        console.log('Received Binary Message of ' + message.binaryData.length + ' bytes');
      }
    });
    connection.on('close', (reasonCode, description) => {
      console.log((new Date()) + ' Peer ' + connection.remoteAddress + ' disconnected.');
  
      deviceList.reset({ socketIndex: thisIndex });
      deviceList.removeListener('changed', changedMessage);
      deviceList.removeListener('video', sendVideo);
      connection = null;
  
      if (redLight) {
        clearInterval(redLightInterval);
        redLight.setState('on');
      }
    });
  
    function changedMessage(name, state, options) {
      if (connection) {
        const message = {
          name,
          state,
          options
        };
        connection.sendUTF(JSON.stringify(message));
      }
    }
    function setUpdate(name, state, options) {

    }
    function sendVideo(data) {
      if (connection) {
        connection.sendBytes(data);
      }
    }

    function sendDeviceOptions() {
      const message = {
        name: 'devices',
        options: settings.devices.map(deviceOptions)
      };

      if (connection) {
        connection.sendUTF(JSON.stringify(message));
      }
    }

    function deviceOptions(d) {
      if (d.type === 'camera') {
        return {
          name: d.name,
          width: d.options.width,
          height: d.options.height,
          framerate: d.options.framerate
        };
      } else {
        return { name: d.name };
      }
    }
  
    deviceList.on('changed', changedMessage);
    deviceList.on('video', sendVideo);
    sendDeviceOptions();
  });
}

module.exports.cleanup = function() {
    if (redLight) {
        redLight.setState('off');
    }
}
