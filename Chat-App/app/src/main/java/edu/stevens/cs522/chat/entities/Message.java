package edu.stevens.cs522.chat.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.Instant;

/**
 * Created by dduggan.
 */

// TODO annotate (including FK constraints)
// You must also declare indices on the FK columns, otherwise integrity checking
// may trigger a linear search of this table.
@Entity(tableName="messages",
        foreignKeys = @ForeignKey(
                entity = Peer.class, onDelete=ForeignKey.CASCADE, parentColumns = "name", childColumns = "sender"),
        indices = {@Index(value = "sender")})
public class Message implements Parcelable {

    // TODO annotate
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String chatroom;

    public String messageText;

    public Instant timestamp;

    public Double latitude;

    public Double longitude;

    public String sender;

    public Message() {
    }

    public Message(Parcel in) {
        id = in.readLong();
        chatroom = in.readString();
        messageText = in.readString();
        timestamp = TimestampConverter.deserialize(in.readString());
        latitude = in.readDouble();
        longitude = in.readDouble();
        sender = in.readString();
    }

    @Override
    public String toString() {
        return messageText;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(id);
        out.writeString(chatroom);
        out.writeString(messageText);
        out.writeString(TimestampConverter.serialize(timestamp));
        out.writeDouble(latitude);
        out.writeDouble(longitude);
        out.writeString(sender);
    }

    public static final Creator<Message> CREATOR = new Creator<Message>() {

        @Override
        public Message createFromParcel(Parcel source) {
            return new Message(source);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }

    };

    public String getSender() {
        return sender;
    }

}

