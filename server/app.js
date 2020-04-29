"use strict"

const express = require('express');

module.exports = function() {
    const app = express();
    app.use(express.static('static'));
    app.use('/node_modules', express.static('node_modules'));

    return app;
}
