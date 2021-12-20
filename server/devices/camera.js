"use strict"

const EventEmitter = require('events'),
      extend = require('extend'),
      RaspiCam = require('raspicam'),
      RaspiMock = require('./mock/raspicam-mock'),
      imagingInUse = require('../imaging/imaging-in-use');

const BLOCK_MARKER = new Buffer([0, 0, 0, 1]),
      BLOCKS_RECEIVED_PREFIX = 'block-';

class CameraDevice extends EventEmitter {
  constructor(config) {
    super();

    this.running = false;
    this.mock = config.mock;
    this.options = extend({ mode: 'video', output: '-' }, config.options);
    
    this.minLag = config.minLag || 1;
    this.maxLag = config.maxLag || 5;

    this.blocksSent = 0;
    this.skipBlocks = 0;

    this.inUsePromise = null;
    this.inUse = null;
  }

  setState(state, options) {
    if (state.startsWith(BLOCKS_RECEIVED_PREFIX)) {
      let blockCount = parseInt(state.substring(BLOCKS_RECEIVED_PREFIX.length));
      this.updateBlocksReceived(blockCount);
      return;
    }

    const turnOn = (state.startsWith('on'));
    if (this.running !== turnOn) {
      if (turnOn)
        this.start(state, options);
      else
        this.stop();
    } else if (this.running) {
        this.stop();
        setTimeout(() => {
          this.start(state)
        }, 100);
    }
  }

  reset() {
    if (this.running) {
      this.stop();
    }
  }

  disconnect() {
    reset();
  }

  start(state, options) {
    this.blocksSent = 0;
    this.blocks = [];

    let cameraOptions = this.options,
        sizePos = state.indexOf('-('),
        sizeEnd = sizePos !== -1 ? state.indexOf(')', sizePos) : -1;

    if (options) {
      const { width, height, bitrate, framerate, intra } = options;
      cameraOptions = extend({}, this.options, { width, height, bitrate, framerate, intra });
    } else if (sizePos !== -1 && sizeEnd !== -1) {
      let sizeParams = state.substring(sizePos + 2, sizeEnd).split(',');
      if (sizeParams.length === 2) {
        // Get the requested size
        let sizeOptions = { width: parseInt(sizeParams[0]), height: parseInt(sizeParams[1]) };

        // Limit to the width specified in the options
        sizeOptions.height = sizeOptions.height * this.options.width / sizeOptions.width;
        sizeOptions.width = this.options.width;

        cameraOptions = extend({}, this.options, sizeOptions);
      }
    }

    this.raspicam = this.mock ? new RaspiMock(cameraOptions) : new RaspiCam(cameraOptions);
    this.emit('changed', 'on', { width: cameraOptions.width, height: cameraOptions.height });

    this.raspicam.on('start', (e, t, stream) => {
      console.log('video started');

      stream.on('data', this.buffer.bind(this))
    });

    this.inUsePromise = imagingInUse.start();
    this.inUsePromise.then(inUse => {
      this.inUsePromise = null;
      this.inUse = inUse;
      this.raspicam.start();
    });

    this.running = true;
  }

  stop() {
    console.log('video finished');
    this.emit('changed', 'off');

    if (this.inUsePromise) {
      this.inUsePromise.cancel();
      this.inUsePromise = null;
    }
    if (this.inUse) {
      this.inUse.stop();
      this.inUse = null;
    }
    this.raspicam.stop();
    this.running = false;
  }

  updateBlocksReceived(receivedCount) {
    let lag = this.blocksSent - receivedCount;
    if (lag > this.maxLag) {
      this.skipBlocks = lag - this.minLag;
      console.log('Skipping ' + this.skipBlocks + ' video blocks');
    }
  }

  buffer(data) {
    try {
      this.blocks.push({ offset: 0, size: data.length, data });

      while(this.trySendBlock()) {
      }
    } catch (ex) {
      console.error('Error sending stream data');
      this.stop();
    }
  }

  trySendBlock() {
    var videoBlock = this.extractBlock();

    if (videoBlock && videoBlock.length > 0) {

      if (this.skipBlocks > 0) {
        // Skip this block
        this.skipBlocks--;
      } else {
        // Send this block
        this.emit('video', videoBlock);
        this.blocksSent++;
      }
      return true;
    }
    return false;
  } 

  extractBlock() {
    let lastBlock = this.blocks[this.blocks.length - 1];
    let blockMarkerIndex = lastBlock.data.indexOf(BLOCK_MARKER, lastBlock.offset + 1);
    if (blockMarkerIndex === -1) return null; 

    lastBlock.size = blockMarkerIndex - lastBlock.offset;

    // Count of block to create
    let count = this.blocks.reduce((prev, block) => prev + block.size, 0);
    if (count === 0) return null;

    let videoBlock = new Buffer(count),
        blockPos = 0;

    this.blocks.forEach((block, i) => {
      block.data.copy(videoBlock, blockPos, block.offset, block.offset + block.size)
      blockPos += block.size;
    });

    this.blocks = [{
      offset: blockMarkerIndex,
      size: lastBlock.data.length - blockMarkerIndex,
      data: lastBlock.data
    }];

    return videoBlock;
  }
}
module.exports = CameraDevice;