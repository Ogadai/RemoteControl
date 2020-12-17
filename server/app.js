"use strict"

const express = require('express');
const path = require('path');

module.exports = function() {
    const app = express();
    app.use(express.static(path.join(__dirname, 'static')));
    app.use('/node_modules', express.static(path.join(__dirname, 'node_modules')));

    return app;
}
