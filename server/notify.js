"use strict"

const queryString = require('querystring'),
      https = require('https'),
      extend = require('extend');

module.exports = (hostOptions, message1, message2) => {
  var data = JSON.stringify({
    value1: 'Remote Control Server',
    value2: message1,
    value3: message2
  });

  var options = extend({
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(data)
      }
    }, hostOptions);

  var req = https.request(options);
  req.write(data);
  req.end();
}

