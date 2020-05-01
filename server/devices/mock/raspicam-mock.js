var events = require('events'),
    fs = require('fs');

function RaspiCamMock(opts) {
    var self = this,
        testVideo = './devices/mock/out.h264';

    this.start = function () {
        if (opts.mode == 'video') {
            videoStart();
        }
    }
    this.stop = function () {
        if (opts.mode == 'video') {
            videoStop();
        }
        self.emit('exit')
    }

    var fileData,
        videoEmitter,
        running,
        bytesPerChunk = 4096;

    function videoStart() {
        running = true;

        // var movieStream = fs.createReadStream(testVideo, { highWaterMark: 4096 })
        // self.emit('start', 'message', 0, movieStream);

        // movieStream.on('end', self.stop);

       videoEmitter = new events.EventEmitter();
       self.emit('start', 'message', 0, videoEmitter);
       fileData = fs.openSync(testVideo, 'r');
       setTimeout(sendVideoChunk, 36);
    }
    function videoStop() {
        running = false;
    }

    function sendVideoChunk() {
        var buffer = new Buffer(bytesPerChunk);
        fs.read(fileData, buffer, 0, bytesPerChunk, null, function (err, bytesRead, buffer) {
            var useBuffer = buffer,
                endOfFile = false;
            if (bytesRead < bytesPerChunk) {
                useBuffer = buffer.slice(0, bytesRead);
                endOfFile = true;
            }

            videoEmitter.emit('data', useBuffer);

            if (endOfFile || !running) {
                fs.closeSync(fileData);
                fileData = null;

                videoEmitter.emit('end');
                self.stop();
            } else {
                setTimeout(sendVideoChunk, 4);
            }
        });
    }
}
RaspiCamMock.prototype.__proto__ = events.EventEmitter.prototype;
module.exports = RaspiCamMock;
