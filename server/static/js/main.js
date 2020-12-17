import { sendDrive, m1up, m1down, m2up, m2down, video, setCanvas } from './socket-client.js';
import { orientation } from './orientation.js';
import { showInfo } from './error.js';
import { fullScreen } from './full-screen.js';

const query = selector => document.querySelector(selector);

const { hostname, protocol } = window.location;

const canvasElement = query('#canvasVideo');
setCanvas(canvasElement);

fullScreen(query('#btnFullScreen'));

let legoSpeed = 0;
let legoTurn = 0;

const button = (selector, start, end) => {
    const btn = query(selector);
    btn.addEventListener('mousedown', start);
    btn.addEventListener('mouseup', end);
    btn.addEventListener('touchstart', start);
    btn.addEventListener('touchend', end);
};

button('#btnForward',
    () => {
        legoSpeed = 1;
        sendUpdate();
    },
    () => {
        legoSpeed = 0;
        sendUpdate();
    });

button('#btnBackward',
    () => {
        legoSpeed = -1;
        sendUpdate();
    },
    () => {
        legoSpeed = 0;
        sendUpdate();
    });

button('#btnM1Up',
    () => m1up(true),
    () => m1up(false));
button('#btnM1Down',
    () => m1down(true),
    () => m1down(false));

    
button('#btnM2Up',
    () => m2up(true),
    () => m2up(false));
button('#btnM2Down',
    () => m2down(true),
    () => m2down(false));

let timerUpdate;
let windowInFocus = true;
const triggerTimerUpdate = () => {
    timerUpdate = setTimeout(sendUpdate, 500);
};

const sendUpdate = () => {
    if (timerUpdate) clearTimeout(timerUpdate);

    showInfo(`Speed: ${legoSpeed}, Turn: ${legoTurn}`);
    if (windowInFocus) {
        sendDrive({
            speed: legoSpeed,
            turn: legoTurn
        });
    }

    triggerTimerUpdate();
};

orientation(({ turn }) => {
    legoTurn = turn;
    sendUpdate();

    canvasElement.style.transform = `rotate(${-turn}deg)`;
});

window.addEventListener('focus', () => {
    windowInFocus = true;
});
window.addEventListener('blur', () => {
    windowInFocus = false;
});
