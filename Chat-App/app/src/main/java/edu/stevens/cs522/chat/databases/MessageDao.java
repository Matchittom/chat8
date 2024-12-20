package edu.stevens.cs522.chat.databases;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import edu.stevens.cs522.chat.entities.Message;

// TODO add annotations for Repository pattern
@Dao
public interface MessageDao {
    @Query("SELECT * FROM Messages WHERE chatroom = :chatroom")
    public abstract LiveData<List<Message>> fetchAllMessages(String chatroom);

    @Query("SELECT * FROM Messages WHERE sender = :peerName")
    public LiveData<List<Message>> fetchMessagesFromPeer(String peerName);

    @Insert
    public void persist(Message message);

}
