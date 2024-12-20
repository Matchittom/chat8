package edu.stevens.cs522.chat.services;

import android.os.ResultReceiver;

import java.time.Instant;

/**
 * Created by dduggan.
 */

public interface IChatService {

    public void send(String destAddress, String chatRoom, String message,
                     Instant timestamp, double latitude, double longitude,
                     ResultReceiver receiver);

}
