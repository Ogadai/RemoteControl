const waiting = [];
let inUse = null;

const stopCallback = () => {
  inUse = null;
  if (waiting.length > 0) {
    const next = waiting.splice(0, 1)[0];

    inUse = {
      stop: stopCallback
    };
    next(inUse);
  }
}

const imaging = {
  start: () => {
    let usePromise;
    let inQueue = null;
    let stopped = false;
    let stopper = () => {
      if (!stopped) {
        stopped = true;
        stopCallback();
      }
    };

    if (inUse) {
      usePromise = new Promise(resolve => {
        inQueue = () => {
          inQueue = null;
          resolve(inUse);
        };
        waiting.push(inQueue);
      });
    } else {
      inUse = {
        stop: stopper
      };
      usePromise = Promise.resolve(inUse);
    }

    usePromise.cancel = () => {
      if (inQueue) {
        const index = waiting.indexOf(inQueue);
        if (index !== -1) {
          waiting.splice(index, 1)
        }
      } else {
        stopper();
      }
    };
    return usePromise;
  }
};

module.exports = imaging;
