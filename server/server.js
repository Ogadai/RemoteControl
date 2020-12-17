"use strict"

const fs = require('fs'),
    path = require('path'),
    http = require('http'),
    https = require('https'),
    moment = require('moment'),
    getIP = require('./get-ip'),
    settings = require('./settings'),
    notify = require('./notify'),
    app = require('./app'),
    webSocket = require('./web-socket');

const ipAddress = getIP();
const onListen = (protocol, port, doNotify) => () => {
  let addr = `{${protocol}}://${ipAddress}:${port}`,
      dateNow = moment().format('LLL');
  console.log(dateNow + ' Server is listening at ' + addr);

  if (doNotify && settings.notifyOptions) {
    notify(settings.notifyOptions, addr, dateNow);
  }
};
  
const robotApp = app();

const server = http.createServer(robotApp);
server.listen(settings.port, onListen('http|ws', settings.port, true));
webSocket(server);

if (settings.securePort) {
  const httpsServer = https.createServer({
    key: fs.readFileSync(path.join(__dirname, 'server.key'), 'utf8'),
    cert: fs.readFileSync(path.join(__dirname, 'server.cert'), 'utf8')
  }, robotApp);
  httpsServer.listen(settings.securePort, onListen('https|wss', settings.securePort, false));
  webSocket(httpsServer);
}

process.stdin.resume();//so the program will not close instantly

function exitHandler(options, err) {
    if (options.cleanup) {
      webSocket.cleanup();
    }
    if (err) console.log(err.stack);
    if (options.exit) process.exit();
}

//do something when app is closing
process.on('exit', exitHandler.bind(null,{cleanup:true}));

//catches ctrl+c event
process.on('SIGINT', exitHandler.bind(null, {exit:true}));

// catches "kill pid" (for example: nodemon restart)
process.on('SIGUSR1', exitHandler.bind(null, {exit:true}));
process.on('SIGUSR2', exitHandler.bind(null, {exit:true}));

//catches uncaught exceptions
process.on('uncaughtException', exitHandler.bind(null, {exit:true}));
