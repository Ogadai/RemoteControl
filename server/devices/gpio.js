"use strict"

try
{
    module.exports = require('onoff').Gpio;
}
catch(e)
{
    console.error('Couldn\'t load Gpio - ' + e);
    module.exports = null;
}

