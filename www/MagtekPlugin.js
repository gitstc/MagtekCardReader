var MagtekPlugin = {
    openDevice: function(successCB, errorCB) {
        cordova.exec(successCB, errorCB, "CDVMagtekPlugin", "openDevice", []);
    },
    closeDevice: function(successCB, errorCB) {
        cordova.exec(successCB, errorCB, "CDVMagtekPlugin", "closeDevice", []);
    },
    readCard: function(successCB, errorCB) {
        cordova.exec(successCB, errorCB, "CDVMagtekPlugin", "readCard", []);
    }
}

module.exports = MagtekPlugin;