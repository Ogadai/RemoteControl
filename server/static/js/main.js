import { sendDrive, m1up, m1down } from './socket-client.js';
import { orientation } from './orientation.js';
import { showInfo } from './error.js';
import { fullScreen } from './full-screen.js';

const query = selector => document.querySelector(selector);

const { hostname, protocol } = window.location;

const imgElement = query('#imgVideo');
imgElement.src = `${protocol}${hostname}:3001/?action=stream`;

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

let timerUpdate;
const triggerTimerUpdate = () => {
    timerUpdate = setTimeout(sendUpdate, 500);
};

const sendUpdate = () => {
    if (timerUpdate) clearTimeout(timerUpdate);

    showInfo(`Speed: ${legoSpeed}, Turn: ${legoTurn}`);
    sendDrive({
        speed: legoSpeed,
        turn: legoTurn
    });

    triggerTimerUpdate();
};

orientation(({ turn }) => {
    legoTurn = turn;
    sendUpdate();

    imgElement.style.transform = `scale(1.7) rotate(${-turn}deg)`;
});
