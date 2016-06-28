package com.vunun.xmpp;

import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.ReactApplicationContext;

import android.util.Log;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.android.AndroidSmackInitializer;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.sasl.core.SCRAMSHA1Mechanism;
import org.jivesoftware.smack.sasl.provided.SASLDigestMD5Mechanism;
import org.jivesoftware.smack.sasl.provided.SASLPlainMechanism;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.offline.OfflineMessageManager;
import org.jivesoftware.smack.packet.IQ;

import java.io.IOException;
import java.security.cert.CertPathValidatorException;
import java.util.List;

import javax.annotation.Nullable;
import javax.net.ssl.SSLHandshakeException;

public class RNXMPPModule extends ReactContextBaseJavaModule {

    private static final String TAG = "RNXMPPModule";
    private final String EVENT_MESSAGE = "RNXMPPMessage";
    private final String EVENT_IQ = "RNXMPPIQ";
    private final String EVENT_PRESENCE = "RNXMPPPresence";
    private final String EVENT_CONNECT = "RNXMPPConnect";
    private final String EVENT_DISCONNECT = "RNXMPPDisconnect";
    private final String EVENT_ERROR = "RNXMPPError";
    private final String EVENT_LOGINERROR = "RNXMPPLoginError";
    private final String EVENT_LOGIN = "RNXMPPLogin";
    private final String EVENT_ROSTER = "RNXMPPRoster";
    private final ReactApplicationContext mReactContext;
    private final AndroidSmackInitializer mAndroidSmackInitializer = new AndroidSmackInitializer();
    private SimpleConnectionListener mConnectionListener = new SimpleConnectionListener();
    private XMPPTCPConnection mConnection = null;
    private ChatManager mChatManager = null;
    private OfflineMessageManager mOfflineMessageManager = null;
    private StanzaListener mStanzaListener = null;
    private String  mUserName = "";
    private String  mPassword = "";
    private String  mServerName = "";
    private String  mResource = "";


    public RNXMPPModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mReactContext = reactContext;
        mAndroidSmackInitializer.initialize();
    }

    @Override
    public String getName() {
        return "RNXMPP";
    }

    @ReactMethod
    public void connect(String jid, String password, String hostname, int auth) throws CertPathValidatorException {
        Log.e(TAG, "Checking connection " + jid + "...");

        try {
            disconnect();

            String[] array = jid.split("@");
            if(array.length < 2) {
                return;
            }
            mUserName = array[0];
            mPassword = password;

            array = array[1].split("/");
            if (array.length > 1) {
                mServerName = array[0];
                mResource = array[1];
            }
            else {
                mServerName = array[0];
                mResource = "smack";
            }

            Log.e(TAG, mUserName + " " + mPassword + " " + mServerName + " " + mResource);

            XMPPTCPConnectionConfiguration.Builder config = XMPPTCPConnectionConfiguration.builder();
            config.setHost(hostname);
            config.setPort(5222);
            config.setServiceName(mServerName);
            config.setResource(mResource);
            config.setSendPresence(false);
            config.setDebuggerEnabled(true);
            config.setCompressionEnabled(false);
            config.setConnectTimeout(2500);
            config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);

            if (mConnection == null)
                mConnection = new XMPPTCPConnection(config.build());
            mConnection.connect();
            mConnection.addConnectionListener(mConnectionListener);

            sendEvent(mReactContext, EVENT_CONNECT, "Success");

            ReconnectionManager manager = ReconnectionManager.getInstanceFor(mConnection);
            manager.enableAutomaticReconnection();

            StanzaFilter filter = new StanzaFilter() {

                @Override
                public boolean accept(Stanza arg0) {
                    return true;
                }
            };
            mStanzaListener = new StanzaListener() {
                @Override
                public void processPacket(Stanza packet) throws SmackException.NotConnectedException {
                    if (packet instanceof Presence) {
                        Presence presence = (Presence) packet;
                        WritableMap params = Arguments.createMap();
                        params.putString("from", presence.getFrom());
                        params.putString("to", presence.getTo());
                        params.putString("status", presence.getStatus());
                        params.putString("type", presence.getType().toString());
                        params.putString("id", presence.getStanzaId());
                        params.putString("show", presence.getMode().toString());
                        params.putInt("priority", presence.getPriority());
                        sendEvent(mReactContext, EVENT_PRESENCE, params);

                        Log.e(TAG, "presence>>>> " + presence.toString());
                    }

                    if (packet instanceof IQ && ((IQ) packet).getType() == IQ.Type.result) {
                        IQ iq = (IQ) packet;
                        WritableMap params = Arguments.createMap();
                        params.putString("from", iq.getFrom());
                        params.putString("to", iq.getTo());
                        params.putString("type", iq.getType().toString());
                        params.putString("id", iq.getStanzaId());
                        sendEvent(mReactContext, EVENT_IQ, params);
                        Log.e(TAG, "iq>>>> " +iq.toString());
                    }
                }
            };
            mConnection.addPacketInterceptor(mStanzaListener, filter);

        } catch (XMPPException e) {
            mConnection = null;
            sendEvent(mReactContext, EVENT_ERROR, e.getMessage());
        } catch (SmackException e) {
            sendEvent(mReactContext, EVENT_ERROR, e.getMessage());
            mConnection = null;
        } catch (IOException e) {
            sendEvent(mReactContext, EVENT_ERROR, e.getMessage());
            mConnection = null;
        }

        try {
            if(auth == 0) {
                SASLAuthentication.registerSASLMechanism(new SASLPlainMechanism());
                SASLAuthentication.unBlacklistSASLMechanism("PLAIN");
            }
            else if(auth == 1) {
                SASLAuthentication.registerSASLMechanism(new SCRAMSHA1Mechanism());
                SASLAuthentication.blacklistSASLMechanism("SCRAM-SHA-1");
            }
            else if(auth == 2) {
                SASLAuthentication.registerSASLMechanism(new SASLDigestMD5Mechanism());
                SASLAuthentication.unBlacklistSASLMechanism("DIGEST-MD5");
            }

            mConnection.login(mUserName, mPassword);
            if (mOfflineMessageManager == null)
                mOfflineMessageManager = new OfflineMessageManager(mConnection);


            processOfflineMessage();

            Presence presence = new Presence(Presence.Type.available);
            presence.setMode(Presence.Mode.chat);
            presence.setStatus("online");
            presence.setPriority(0);
            mConnection.sendStanza(presence);

            mChatManager = ChatManager.getInstanceFor(mConnection);
            mChatManager.addChatListener(new SimpleChatManagerListener());

        } catch (XMPPException e) {
            sendEvent(mReactContext, EVENT_LOGINERROR, e.getMessage());
        } catch (IOException e) {
            sendEvent(mReactContext, EVENT_LOGINERROR, e.getMessage());
        } catch (SmackException e) {
            sendEvent(mReactContext, EVENT_LOGINERROR, e.getMessage());
        }
    }

    @ReactMethod
    public void disconnect() {

        if (mConnection != null) {
            mConnection.disconnect();
            sendEvent(mReactContext, EVENT_DISCONNECT, "Success");
        }
    }

    @ReactMethod
    public void message(String message, String toJid) {

        Chat chat = ChatManager.getInstanceFor(mConnection).createChat(toJid);
        try {
            chat.sendMessage(message);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
            sendEvent(mReactContext, EVENT_ERROR, e.getMessage());
        }
    }

    @ReactMethod
    public void presence(String to, String type) {
        try {
            Presence presence = new Presence(Presence.Type.available);
            presence.setTo(to);
            presence.setType(Presence.Type.fromString(type));
            mConnection.sendStanza(presence);
        } catch (SmackException.NotConnectedException e) {
            sendEvent(mReactContext, EVENT_ERROR, e.getMessage());
        }
    }

    @ReactMethod
    public void removeRoster(String to) {
        Roster roster = Roster.getInstanceFor(mConnection);
        try {
            for (RosterEntry entry : roster.getEntries()) {
                if(entry.getUser().equals(to)){
                    roster.removeEntry(entry);
                    break;
                }
            }
        }catch (SmackException.NotLoggedInException e){
            sendEvent(mReactContext, EVENT_ERROR, e.getMessage());

        }catch (SmackException.NoResponseException e) {
            sendEvent(mReactContext, EVENT_ERROR, e.getMessage());

        }catch (XMPPException.XMPPErrorException e) {
            sendEvent(mReactContext, EVENT_ERROR, e.getMessage());

        }catch (SmackException.NotConnectedException e) {
            sendEvent(mReactContext, EVENT_ERROR, e.getMessage());
        }
    }

    @ReactMethod
    public void fetchRoster() {

        Roster roster = Roster.getInstanceFor(mConnection);
        for (RosterEntry entry : roster.getEntries()) {
            if(roster.getPresence(entry.getUser()).isAvailable()){
                WritableMap params = Arguments.createMap();
                params.putString("user", entry.getUser());
                params.putString("status", "Online");
                sendEvent(mReactContext, EVENT_ROSTER, params);

            } else {
                WritableMap params = Arguments.createMap();
                params.putString("user", entry.getUser());
                params.putString("status", "Offline");
                sendEvent(mReactContext, EVENT_ROSTER, params);
            }
        }
    }

    @ReactMethod
    public void sendStanza(String message) {

//        try {
//
//            mConnection.sendStanza(stanza);
//        } catch (SmackException.NotConnectedException e) {
//            e.printStackTrace();
//            sendEvent(mReactContext, EVENT_ERROR, e.getMessage());
//        }
    }

    private void processOfflineMessage() {
        try {
            List<Message> list = mOfflineMessageManager.getMessages();
            for (int i = 0; i < list.size(); i++) {
                Message message = list.get(i);
                WritableMap params = Arguments.createMap();
                params.putString("to", message.getTo());
                params.putString("from", message.getFrom());
                params.putString("id", message.getStanzaId());
                params.putString("thread", message.getThread());
                ExtensionElement pe = message.getExtension("inactive","http://jabber.org/protocol/chatstates");
                if(pe != null) {
                    params.putString("inactive", "");
                }
                pe = message.getExtension("active","http://jabber.org/protocol/chatstates");
                if(pe != null) {
                    params.putString("active", "");
                }
                pe = message.getExtension("composing","http://jabber.org/protocol/chatstates");
                if(pe != null) {
                    params.putString("composing", "");
                }
                pe = message.getExtension("paused","http://jabber.org/protocol/chatstates");
                if(pe != null) {
                    params.putString("paused","");
                }
                pe = message.getExtension("paused","http://jabber.org/protocol/chatstates");
                if(pe != null) {
                    params.putString("gone", "");
                }
                if (message.getBody() != null) {
                    params.putString("body", message.getBody());
                }
                sendEvent(mReactContext, EVENT_MESSAGE, params);
            }
            mOfflineMessageManager.deleteMessages();
        } catch (SmackException.NoResponseException e) {
            sendEvent(mReactContext, EVENT_ERROR, e.getMessage());
        } catch (XMPPException.XMPPErrorException e) {
            sendEvent(mReactContext, EVENT_ERROR, e.getMessage());
        } catch (SmackException.NotConnectedException e) {
            sendEvent(mReactContext, EVENT_ERROR, e.getMessage());
        }
    }

    private void sendEvent(ReactContext reactContext,
                           String eventName,
                           @Nullable String params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    private void sendEvent(ReactContext reactContext,
                           String eventName,
                           @Nullable WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    private class SimpleConnectionListener implements ConnectionListener {

        @Override
        public void connected(XMPPConnection connection) {
            sendEvent(mReactContext, EVENT_CONNECT, "Success");
        }

        @Override
        public void authenticated(XMPPConnection connection, boolean resumed) {
            sendEvent(mReactContext, EVENT_LOGIN, "Success");
        }

        @Override
        public void connectionClosed() {
            sendEvent(mReactContext, EVENT_DISCONNECT, "Success");
        }

        @Override
        public void connectionClosedOnError(Exception e) {
            sendEvent(mReactContext, EVENT_ERROR, e.getMessage());
        }

        @Override
        public void reconnectionSuccessful() {
            sendEvent(mReactContext, EVENT_CONNECT, "Success");
        }

        @Override
        public void reconnectingIn(int seconds) {
            // sendEvent(mReactContext, EVENT_CONNECT, "Success");
        }

        @Override
        public void reconnectionFailed(Exception e) {
            sendEvent(mReactContext, EVENT_ERROR, e.getMessage());
        }
    }


    class SimpleChatManagerListener implements ChatManagerListener {
        @Override
        public void chatCreated(Chat chat, boolean createdLocally) {
            if (!createdLocally) {
                chat.addMessageListener(new SimpleChatMessageListener());
            }
        }
    }

    class SimpleChatMessageListener implements ChatMessageListener {
        @Override
        public void processMessage(Chat chat, Message message) {

            Log.e(TAG, "message>>>> " +message.toString());

            WritableMap params = Arguments.createMap();

            params.putString("to", message.getTo());
            params.putString("from", message.getFrom());
            params.putString("id", message.getStanzaId());
            params.putString("thread", message.getThread());
            ExtensionElement pe = message.getExtension("inactive","http://jabber.org/protocol/chatstates");
            if(pe != null) {
                params.putString("inactive", "");
            }
            pe = message.getExtension("active","http://jabber.org/protocol/chatstates");
            if(pe != null) {
                params.putString("active", "");
            }
            pe = message.getExtension("composing","http://jabber.org/protocol/chatstates");
            if(pe != null) {
                params.putString("composing", "");
            }
            pe = message.getExtension("paused","http://jabber.org/protocol/chatstates");
            if(pe != null) {
                params.putString("paused","");
            }
            pe = message.getExtension("paused","http://jabber.org/protocol/chatstates");
            if(pe != null) {
                params.putString("gone", "");
            }
            if (message.getBody() != null) {
                params.putString("body", message.getBody());
            }
            sendEvent(mReactContext, EVENT_MESSAGE, params);
        }
    }
}