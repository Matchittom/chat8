package edu.stevens.cs522.chat.services;

import static android.app.Activity.RESULT_OK;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.ResultReceiver;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.time.Instant;

import edu.stevens.cs522.base.Datagram;
import edu.stevens.cs522.base.DatagramConnectionFactory;
import edu.stevens.cs522.base.IDatagramConnection;
import edu.stevens.cs522.chat.R;
import edu.stevens.cs522.chat.databases.ChatDatabase;
import edu.stevens.cs522.chat.entities.Chatroom;
import edu.stevens.cs522.chat.entities.Message;
import edu.stevens.cs522.chat.entities.Peer;
import edu.stevens.cs522.chat.entities.TimestampConverter;
import edu.stevens.cs522.chat.settings.Settings;


public class ChatService extends Service implements IChatService {

    protected static final String TAG = ChatService.class.getCanonicalName();

    protected static final String SEND_TAG = "ChatSendThread";


    public final static String SENDER_NAME = "name";

    public final static String CHATROOM = "room";

    public final static String MESSAGE_TEXT = "text";

    public final static String TIMESTAMP = "timestamp";

    public final static String LATITUDE = "latitude";

    public final static String LONGITUDE = "longitude";


    protected IBinder binder = new ChatBinder();

    protected SendHandler sendHandler;

    protected Thread receiveThread;

    protected IDatagramConnection chatConnection;

    protected boolean socketOK = true;

    protected boolean finished = false;

    protected ChatDatabase chatDatabase;

    protected int chatPort;

    @Override
    public void onCreate() {

        chatPort = this.getResources().getInteger(R.integer.app_port);

        Log.d(TAG, "Getting database instance in ChatService....");
        chatDatabase = ChatDatabase.getInstance(this);

        try {
            DatagramConnectionFactory factory = new DatagramConnectionFactory();
            chatConnection = factory.getUdpConnection(chatPort);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to init client socket.", e);
        }

        // TODO initialize the thread that sends messages
        HandlerThread sendThread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        sendThread.start();

        sendHandler = new SendHandler(sendThread.getLooper());

       // end TODO

        receiveThread = new Thread(new ReceiverThread());
        receiveThread.start();
    }

    @Override
    public void onDestroy() {
        finished = true;
        sendHandler.getLooper().getThread().interrupt();  // No-op?
        sendHandler.getLooper().quit();
        socketOK = false;
        receiveThread.interrupt();
        chatConnection.close();

        chatDatabase = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public final class ChatBinder extends Binder {

        public IChatService getService() {
            return ChatService.this;
        }

    }


    @Override
    public void send(String destAddress, String chatRoom, String messageText,
                     Instant timestamp, double latitude, double longitude, ResultReceiver receiver) {
        android.os.Message message = sendHandler.obtainMessage();
        // TODO send the message to the sending thread (add a bundle with params)

        Bundle data = new Bundle();
        data.putString(SendHandler.HDLR_DEST_ADDRESS, destAddress);
        data.putString(SendHandler.HDLR_CHATROOM, chatRoom);
        data.putString(SendHandler.HDLR_MESSAGE_TEXT, messageText);
        data.putString(SendHandler.HDLR_TIMESTAMP, TimestampConverter.serialize(timestamp));
        data.putDouble(SendHandler.HDLR_LATITUDE, latitude);
        data.putDouble(SendHandler.HDLR_LONGITUDE, longitude);
        data.putParcelable(SendHandler.HDLR_RECEIVER, receiver);
        message.setData(data);
        sendHandler.sendMessage(message);


        Log.d(TAG, "chatroom: " + chatRoom);
        Log.d(TAG, "text: " + messageText);

    }


    private final class SendHandler extends Handler {

        public static final String HDLR_CHATROOM = "edu.stevens.cs522.chat.services.extra.CHATROOM";
        public static final String HDLR_MESSAGE_TEXT = "edu.stevens.cs522.chat.services.extra.MESSAGE_TEXT";
        public static final String HDLR_TIMESTAMP = "edu.stevens.cs522.chat.services.extra.TIMESTAMP";
        public static final String HDLR_LATITUDE = "edu.stevens.cs522.chat.services.extra.LATITUDE";
        public static final String HDLR_LONGITUDE = "edu.stevens.cs522.chat.services.extra.LONGITUDE";

        public static final String HDLR_DEST_ADDRESS = "edu.stevens.cs522.chat.services.extra.DEST_ADDRESS";
        public static final String HDLR_RECEIVER = "edu.stevens.cs522.chat.services.extra.RECEIVER";

        public SendHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(android.os.Message message) {

            try {

                String destinationAddr = null;

                String senderName = null;

                String chatRoom = null;

                String messageText = null;

                Instant timestamp = null;

                Double latitude = null, longitude = null;

                ResultReceiver receiver = null;

                senderName = Settings.getSenderName(ChatService.this);

                Bundle data = message.getData();

                // TODO get data from message (including result receiver)
                if (data != null) {
                    destinationAddr = data.getString(SendHandler.HDLR_DEST_ADDRESS);
                    chatRoom = data.getString(SendHandler.HDLR_CHATROOM);
                    messageText = data.getString(SendHandler.HDLR_MESSAGE_TEXT);
                    timestamp = TimestampConverter.deserialize(data.getString(SendHandler.HDLR_TIMESTAMP));
                    latitude = data.getDouble(SendHandler.HDLR_LATITUDE);
                    longitude = data.getDouble(SendHandler.HDLR_LONGITUDE);
                    receiver = data.getParcelable(SendHandler.HDLR_RECEIVER);
                }
                Log.d("MessageParsed", "Parsed Sender: " + senderName);
                Log.d("MessageParsed", "parsed Chatroom: " + chatRoom);
                // End todo

                /*
                 * Insert into the local database
                 */
                Message mesg = new Message();
                mesg.messageText = messageText;
                mesg.chatroom = chatRoom;
                mesg.timestamp = timestamp;
                mesg.latitude = latitude;
                mesg.longitude = longitude;
                mesg.sender = senderName;

                // Okay to do this synchronously because we are on a background thread.
                chatDatabase.messageDao().persist(mesg);

                StringWriter output = new StringWriter();
                JsonWriter wr = new JsonWriter(output);
                wr.beginObject();
                wr.name(SENDER_NAME).value(senderName);
                wr.name(CHATROOM).value(chatRoom);
                wr.name(MESSAGE_TEXT).value(messageText);
                wr.name(TIMESTAMP).value(TimestampConverter.serialize(timestamp));
                wr.name(LATITUDE).value(latitude);
                wr.name(LONGITUDE).value(longitude);
                wr.endObject();

                String content = output.toString();

                Log.d(TAG, "Sending data: " + content);

                Datagram sendPacket = new Datagram();
                sendPacket.setAddress(destinationAddr);
                sendPacket.setData(content);

                chatConnection.send(getApplicationContext(), sendPacket);

                if (receiver != null) {
                    try {
                        Log.d(TAG, "Sleeping for 5 seconds before sending message....");
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        Log.i(TAG, "....sleep during message send interrupted!", e);
                    }
                    Log.d(TAG, "Sending message....");
                    receiver.send(RESULT_OK, null);
                    Log.d(TAG, "...message sent!");
                }

            } catch (UnknownHostException e) {
                Log.e(TAG, "Unknown host exception", e);
            } catch (IOException e) {
                Log.e(TAG, "IO exception", e);
            }

        }
    }

    private final class ReceiverThread implements Runnable {

        public void run() {

            Datagram receivePacket = new Datagram();

            while (!finished && socketOK) {

                try {

                    String sender = null;

                    String room = null;

                    String text = null;

                    Instant timestamp = null;

                    Double latitude = null;

                    Double longitude = null;

                    /*
                     * THere is an apparent bug in the emulator stack on Windows where
                     * messages can arrive empty, we loop as a workaround.
                     */

                    chatConnection.receive(receivePacket);
                    Log.d(TAG, "Received a packet");

                    if (receivePacket.getData() == null) {
                        Log.d(TAG, "....missing data, skipping....");
                        continue;
                    }

                    Log.d(TAG, "Source Address: " + receivePacket.getAddress());

                    String content = receivePacket.getData();
                    Log.d(TAG, "Message received: " + content);

                    /*
                     * Parse the JSON object
                     */
                    JsonReader rd = new JsonReader(new StringReader(content));

                    rd.beginObject();
                    if (SENDER_NAME.equals(rd.nextName())) {
                        sender = rd.nextString();
                    }
                    if (CHATROOM.equals(rd.nextName())) {
                        room = rd.nextString();
                    }
                    if (MESSAGE_TEXT.equals((rd.nextName()))) {
                        text = rd.nextString();
                    }
                    if (TIMESTAMP.equals(rd.nextName())) {
                        timestamp = TimestampConverter.deserialize(rd.nextString());
                    }
                    if (LATITUDE.equals(rd.nextName())) {
                        latitude = rd.nextDouble();
                    }
                    if (LONGITUDE.equals((rd.nextName()))) {
                        longitude = rd.nextDouble();
                    }
                    rd.endObject();

                    rd.close();

                    /*
                     * Add the sender to our list of senders
                     */
                    Chatroom chatroom = new Chatroom();
                    chatroom.name = room;

                    Peer peer = new Peer();
                    peer.name = sender;
                    peer.timestamp = timestamp;
                    peer.latitude = latitude;
                    peer.longitude = longitude;

                    Message message = new Message();
                    message.messageText = text;
                    message.chatroom = room;
                    message.sender = sender;
                    message.timestamp = timestamp;
                    message.latitude = latitude;
                    message.longitude = longitude;


                    Log.d(TAG, "sender: " + sender);
                    Log.d(TAG, "chatroom: " + room);
                    Log.d(TAG, "text: " + text);
                    Log.d(TAG, "Message: " + message);

                    /*
                     * TODO upsert chatroom and peer, and insert message into the database
                     */
                    Log.d("ChatroomViewModel", "Adding chatroom: " + chatroom.name);
                    chatDatabase.chatroomDao().insert(chatroom);
                    Log.d("ChatroomViewModel", "Chatroom added: " + chatroom.name);
                    chatDatabase.peerDao().upsert(peer);
                    chatDatabase.messageDao().persist(message);

                    Log.d(TAG, "Parsed sender: " + sender);
                    Log.d(TAG, "Parsed chatroom: " + room);
                    Log.d(TAG, "Parsed text: " + text);
                    Log.d(TAG, "Message object: " + message);


                } catch (Exception e) {

                    Log.e(TAG, "Problems receiving packet.", e);
                    socketOK = false;
                }

            }

        }

    }

}
