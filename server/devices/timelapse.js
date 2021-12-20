"use strict"
const EventEmitter = require('events'),
  extend = require('extend'),
  fs = require('fs'),
  moment = require('moment'),
  RaspiCam = require('raspicam'),
  StoreFolder = require('../imaging/store-folder'),
  imagingInUse = require('../imaging/imaging-in-use');

const DEFAULT_OPTIONS = {
  intervalS: 600,
  store: '../cam'
};

class TimeLapse extends EventEmitter {
  constructor(options) {
    super();
    this.options = extend({}, DEFAULT_OPTIONS, options)

    this.inUsePromise = null;

    this.storeFolder = new StoreFolder({
      store: this.options.store,
      storeDays: this.options.storeDays
    })

    setInterval(() => {
      if (this.retryTimer) {
        clearTimeout(this.retryTimer);
        this.retryTimer = null;
      }

      this.triggerSnapshot();
    }, this.options.intervalS * 1000);

    setTimeout(() => {
      this.triggerSnapshot();
    }, 10000);
  }

  triggerSnapshot() {
    if (this.inUsePromise) {
      this.inUsePromise.cancel();
    }

    this.inUsePromise = imagingInUse.start();
    this.inUsePromise.then(inUse => {
      this.inUsePromise = null;
      this.snapshot(inUse);
    });
  }

  snapshot(inUse) {
    const now = moment()
    const folderstamp = now.format('YYYY-MM-DD')
    const filestamp = now.format('HH-mm-ss')

    try {
      this.storeFolder.checkFolder(folderstamp).then(() => {
          const writeName = `${folderstamp}/${filestamp}.jpg`;

          const camSettings = extend({
            mode: "photo",
            output: `../${this.options.store}/${writeName}`
          }, this.options.options);

          const camera = new RaspiCam(camSettings);
          camera.on("exit", function(){
            inUse.stop();
          });

          camera.start();
      }).catch(ex => {
        console.error('Error capturing timelapse photo', ex);
      });
    } catch (ex) {
      console.error('Error checking folder for timelapse photo', ex);
    }
  }
};
module.exports = TimeLapse;
