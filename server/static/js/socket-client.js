import { showError } from './error.js';

const { host, protocol } = window.location;

const wsProtocol = (protocol === 'https:') ? 'wss:' : 'ws:';
let webSocket;
let canvasElement;
let socketOpen = false;
let wsavc;
let devicesCallback;

export const videoSize = {
    width: 640,
    height: 480,
    framerate: 15,
    intra: 15
};

connectSocket();

function connectSocket() {
    if (webSocket) {
        disconnectSocket();
    }

    webSocket = new WebSocket(`${wsProtocol}${host}`, 'echo-protocol');
    webSocket.binaryType = "arraybuffer";

    webSocket.onopen = () => {
        console.log('socket is connected')
        socketOpen = true;
    }

    webSocket.onmessage = (evt) => {
        if(typeof evt.data == "string") {
            return handleMessage(JSON.parse(evt.data));
        }

        var frame = new Uint8Array(evt.data);
        wsavc.addFrame(frame);
    };

    if (canvasElement) {
        wsavc = new WSAvcPlayer(canvasElement, "webgl", 1, 35);
    }
}

function disconnectSocket() {
    if (webSocket) {
        webSocket.close();
        webSocket = null;
    }
    socketOpen = false;
    wsavc = null;
}

function handleMessage(msg) {
    if (msg.name === 'camera' && msg.state === 'on') {
        console.log(`Camera on, options: `, msg.options);
    } else if (msg.name === 'devices') {
        console.log(`Devices: `, msg.options);
        const camOptions = msg.options.find(d => d.name === 'camera');
        if (camOptions) {
            videoSize.width = camOptions.width;
            videoSize.height = camOptions.height;
            videoSize.framerate = camOptions.framerate;
            videoSize.intra = camOptions.intra;
        }

        if (canvasElement) {
            wsavc.initCanvas(videoSize.width, videoSize.height);
            canvasElement.style.left = `calc(50vw - (50vh * ${videoSize.width / videoSize.height}))`;
            canvasElement.style.width = `calc(100vh * ${videoSize.width / videoSize.height})`;
            video(true);
        }
        if (devicesCallback) {
            devicesCallback(msg.options);
        }
    }
}

function send(msg) {
    try {
        webSocket.send(JSON.stringify(msg));
    } catch (error) {
        showError(error, 'Sending websocket message');
    }
}

export function sendDrive(state) {
    send({
        name: 'drive',
        state
    });
}

export function m1up(onoff) {
    send({
        name: 'm1up',
        state: onoff ? 'on' : 'off'
    });
}

export function m1down(onoff) {
    send({
        name: 'm1down',
        state: onoff ? 'on' : 'off'
    });
}

export function m2up(onoff) {
    send({
        name: 'm2up',
        state: onoff ? 'on' : 'off'
    });
}

export function m2down(onoff) {
    send({
        name: 'm2down',
        state: onoff ? 'on' : 'off'
    });
}

export function setCanvas(canvas, callback) {
    canvasElement = canvas;
    devicesCallback = callback;

    wsavc = new WSAvcPlayer(canvasElement, "webgl", 1, 35);

    if (socketOpen) {
        video(true);
    }
}

export function video(onoff) {
    send({
        name: 'camera',
        state: onoff ? 'on' : 'off',
        options: onoff ? videoSize : undefined
    });
}


// Set the name of the hidden property and the change event for visibility
var hidden, visibilityChange;
if (typeof document.hidden !== "undefined") { // Opera 12.10 and Firefox 18 and later support
  hidden = "hidden";
  visibilityChange = "visibilitychange";
} else if (typeof document.msHidden !== "undefined") {
  hidden = "msHidden";
  visibilityChange = "msvisibilitychange";
} else if (typeof document.webkitHidden !== "undefined") {
  hidden = "webkitHidden";
  visibilityChange = "webkitvisibilitychange";
}
document.addEventListener(visibilityChange, () => {
    if (!document[hidden]) {
        connectSocket();
    } else {
        disconnectSocket();
    }
}, false);

