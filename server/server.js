"use strict"

const WebSocketServer = require('websocket').server,
    http = require('http'),
    moment = require('moment'),
    DeviceList = require('./device-list'),
    getIP = require('./get-ip'),
    settings = require('./settings'),
    notify = require('./notify');

let server = http.createServer((request, response) => {
  console.log((new Date()) + ' Received request for ' + request.url);
  response.writeHead(404);
  response.write('Remote Control - 404');
  response.end();
});
server.listen(settings.port, () => {
  let addr = 'ws://' + getIP() + ':' + settings.port,
      dateNow = moment().format('LLL');
  console.log(dateNow + ' Server is listening at ' + addr);

  if (settings.notifyOptions) {
    notify(settings.notifyOptions, addr, dateNow);
  }
});

let wsServer = new WebSocketServer({
  httpServer: server,
  autoAcceptConnections: false
});

let deviceList = new DeviceList(settings.devices, 'RC');

wsServer.on('request', request => {
  console.log(`Web Socket opened from ${request.origin} for ${request.resource}`);
  let connection = request.accept('echo-protocol', request.origin);

  connection.on('message', message => {
    if (message.type === 'utf8') {
      try {
        let decodedMsg = JSON.parse(message.utf8Data),
            device = deviceList.getDevice(decodedMsg.name),
            state = decodedMsg.state;

        if (device) {
          device.setState(state);
          console.log(decodedMsg.name + ' set: ' + state);
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

    deviceList.reset();
    deviceList.removeListener('changed', changedMessage);
    deviceList.removeListener('video', sendVideo);
    connection = null;
  });

  function changedMessage(name, state) {
    if (connection) {
      connection.sendUTF(name + ' changed: ' + state);
    }
  }
  function sendVideo(data) {
    if (connection) {
      connection.sendBytes(data);
    }
  }

  deviceList.on('changed', changedMessage);
  deviceList.on('video', sendVideo);
});
