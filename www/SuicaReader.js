var exec = require('cordova/exec');

exports.getHistory = function(count, success, error) {
    exec(success, error, "SuicaReader", "getHistory", [count]);
};
