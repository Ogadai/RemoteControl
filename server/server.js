const WebSocketServer = require('websocket').server,
    http = require('http'),
    DeviceList = require('./device-list'),
    getIP = require('./get-ip'),
    settings = require('./settings');

let server = http.createServer((request, response) => {
  console.log((new Date()) + ' Received request for ' + request.url);
  response.writeHead(404);
  response.write('Remote Control - 404');
  response.end();
});
server.listen(settings.port, () => {
  console.log((new Date()) + ' Server is listening at '
        + getIP() + ':' + settings.port);
});

let wsServer = new WebSocketServer({
  httpServer: server,
  autoAcceptConnections: false
});

let deviceList = new DeviceList(settings.devices, 'RC');

wsServer.on('request', request => {
  console.log('Web Socket opened from ', request.origin);
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
          console.log('unknown device: ' + decodedMsg.name);
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
    connection = null;
  });

  deviceList.on('changed', (name, state) => {
    connection.sendUTF(name + ' changed: ' + state);
  });
});
