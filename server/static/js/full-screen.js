export function fullScreen(buttonElement) {
    let fullScreen = false;
    buttonElement.style.backgroundImage = 'url(/img/fullscreen.png)';

    buttonElement.addEventListener('click', () => {
        fullScreen = !fullScreen;
        if (fullScreen) {
            document.querySelector('#root').requestFullscreen();
            buttonElement.style.backgroundImage = 'url(/img/fullscreen-exit.png)';
        } else {
            document.exitFullscreen();
            buttonElement.style.backgroundImage = 'url(/img/fullscreen.png)';
        }
    });
}
