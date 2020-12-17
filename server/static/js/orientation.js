import { showError } from './error.js';

export function orientation(callbackFn) {
    const handleOrientation = (event) => {
        callbackFn({turn: Math.round(event.beta || 0)});
    };

    try {
        if (window.DeviceOrientationEvent && typeof window.DeviceOrientationEvent.requestPermission === 'function') {
            window.DeviceOrientationEvent.requestPermission().then(permissionState => {
                if (permissionState === 'granted') {
                    console.log('hooking deviceorientation');
                    window.addEventListener('deviceorientation', handleOrientation);
                }
            })
            .catch(error => {
                console.error(error || 'Permission not given');
                showError(error, 'Permission not given');
            });
        } else {
            console.log('hooking deviceorientation');
            window.addEventListener('deviceorientation', handleOrientation);
        }
    } catch (error) {
        showError(error, 'Requesting orientation listener');
    }
}
