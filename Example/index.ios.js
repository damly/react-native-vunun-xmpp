/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 * @flow
 */

import React, { Component } from 'react';
import {
  AppRegistry,
  StyleSheet,
  Text,
  View
} from 'react-native';

import XMPP from 'react-native-vunun-xmpp'

class Example extends Component {

  constructor(props) {
    super(props);
    XMPP.on('message', (message)=>console.log("MESSAGE:"+JSON.stringify(message)));
    XMPP.on('iq', (message)=>console.log("IQ:"+JSON.stringify(message)));
    XMPP.on('presence', (message)=>console.log("PRESENCE:"+JSON.stringify(message)));
    XMPP.on('error', (message)=>console.log("ERROR:"+message));
    XMPP.on('loginError', (message)=>console.log("LOGIN ERROR:"+message));
    XMPP.on('login', (message)=>console.log("LOGGED!"));
    XMPP.on('connect', (message)=>console.log("CONNECTED!"));
    XMPP.on('disconnect', (message)=>console.log("DISCONNECTED!"));
  }

  componentDidMount() {

    XMPP.connect('1000@vunun-tarot.top', '1', 'vunun-tarot.top');

   // XMPP.message('Hello world!', '1001@vunun-tarot.top');
  }
  
  render() {
    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>
          Welcome to React Native!
        </Text>
        <Text style={styles.instructions}>
          To get started, edit index.ios.js
        </Text>
        <Text style={styles.instructions}>
          Press Cmd+R to reload,{'\n'}
          Cmd+D or shake for dev menu
        </Text>
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
  },
  instructions: {
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5,
  },
});

AppRegistry.registerComponent('Example', () => Example);
