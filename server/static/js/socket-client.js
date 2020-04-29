import { showError } from './error.js';

const { host, protocol } = window.location;

const wsProtocol = (protocol === 'https:') ? 'wss:' : 'ws:';
const webSocket = new WebSocket(`${wsProtocol}${host}`, 'echo-protocol');

webSocket.onopen = () => {
    console.log('socket is connected')
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
