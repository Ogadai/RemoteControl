var keypress = require("keypress"),
    Gpio = require('./devices/gpio');

var gpio = null,
    pin = 1;

keypress(process.stdin);
process.stdin.on('keypress', function (ch, key) {
    if (key) {
        if (key.ctrl) {
            switch (key.name) {
                case 'c':
                    console.log('exiting');
                    disconnect();
                    process.exit(0);
                    break;
            }
        } else if (key.name === 'up') {
            pin++;
	    setPin(pin);
        } else if (key.name === 'down') {
            pin--;
	    setPin(pin);
        }
    }
});

if (process.stdin.isTTY) {
    process.stdin.setRawMode(true);
}
process.stdin.resume();
setPin(1);

function setPin(pin) {
  disconnect();
  console.log('testing pin ' + pin);

  gpio = new Gpio(pin, 'out');
  gpio.writeSync(1);
}

function disconnect() {
  if (gpio) {
    gpio.writeSync(0);
    gpio.unexport();
    gpio = null;
  }
}
