import { showError } from './error.js';

const { host, protocol } = window.location;

const wsProtocol = (protocol === 'https:') ? 'wss:' : 'ws:';
const webSocket = new WebSocket(`${wsProtocol}${host}`, 'echo-protocol');
webSocket.binaryType = "arraybuffer";
let canvasElement;
let socketOpen = false;
let wsavc;

const videoSize = {
    width: 640,
    height: 480,
    framerate: 15,
    intra: 15
};

webSocket.onopen = () => {
    console.log('socket is connected')
    socketOpen = true;

    if (canvasElement) {
        video(true);
    }
}

webSocket.onmessage = (evt) => {
    if(typeof evt.data == "string") {
        return handleMessage(JSON.parse(evt.data));
    }

    var frame = new Uint8Array(evt.data);
    wsavc.addFrame(frame);
};

function handleMessage(msg) {
    if (msg.name === 'camera' && msg.state === 'on') {
        console.log(`Camera on, options: `, msg.options);
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

export function setCanvas(canvas) {
    canvasElement = canvas;

    wsavc = new WSAvcPlayer(canvasElement, "webgl", 1, 35);
    wsavc.initCanvas(videoSize.width, videoSize.height);

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