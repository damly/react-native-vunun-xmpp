'use strict';
var React = require('react-native');
var {NativeAppEventEmitter, NativeModules} = React;
var RNXMPP = NativeModules.RNXMPP;

var map = {
    'message' : 'RNXMPPMessage',
    'iq': 'RNXMPPIQ',
    'presence': 'RNXMPPPresence',
    'connect': 'RNXMPPConnect',
    'disconnect': 'RNXMPPDisconnect',
    'error': 'RNXMPPError',
    'loginError': 'RNXMPPLoginError',
    'login': 'RNXMPPLogin',
    'roster': 'RNXMPPRoster'
}

class XMPP {

    constructor(){
        this.PLAIN = 0;
        this.SCRAMSHA1 = 1;
        this.DigestMD5 = 2;

        this.isConnected = false;
        this.isLogged = false;
        // NativeAppEventEmitter.addListener(map.connect, this.onConnected.bind(this));
        // NativeAppEventEmitter.addListener(map.disconnect, this.onDisconnected.bind(this));
        // NativeAppEventEmitter.addListener(map.error, this.onError.bind(this));
        // NativeAppEventEmitter.addListener(map.loginError, this.onLoginError.bind(this));
        // NativeAppEventEmitter.addListener(map.login, this.onLogin.bind(this));
    }

    onConnected(){
        console.log("Connected");
        this.isConnected = true;
    }

    onLogin(){
        console.log("Logged");
        this.isLogged = true;
    }

    onDisconnected(error){
        console.log("Disconnected, error"+error);
        this.isConnected = false;
        this.isLogged = false;
    }

    onError(text){
        console.log("Error: "+text);
    }

    onLoginError(text){
        this.isLogged = false;
        console.log("LoginError: "+text);
    }

    on(type, callback){
        if (map[type]){
            NativeAppEventEmitter.addListener(
                map[type],callback);
        } else {
            console.error("No registered type: "+type);
        }

    }

    connect(username, password, host, auth = this.SCRAMSHA1){
        React.NativeModules.RNXMPP.connect(username, password, host, auth);
    }

    message(text, user){
        React.NativeModules.RNXMPP.message(text, user);
    }

    fetchRoster(){
        RNXMPP.fetchRoster();
    }

    presence(to, type){
        React.NativeModules.RNXMPP.presence(to, type);
    }

    removeFromRoster(to){
        React.NativeModules.RNXMPP.removeRoster(to);
    }

    disconnect(){
        if (this.isConnected){
            React.NativeModules.RNXMPP.disconnect();
        }
    }
}

module.exports = new XMPP();
