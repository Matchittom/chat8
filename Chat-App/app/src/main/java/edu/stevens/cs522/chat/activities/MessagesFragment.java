/*********************************************************************

    Chat server: accept chat messages from clients.
    
    Sender chatName and GPS coordinates are encoded
    in the messages, and stripped off upon receipt.

    Copyright (c) 2017 Stevens Institute of Technology

**********************************************************************/
package edu.stevens.cs522.chat.activities;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import edu.stevens.cs522.chat.R;
import edu.stevens.cs522.chat.entities.Chatroom;
import edu.stevens.cs522.chat.entities.Message;
import edu.stevens.cs522.chat.settings.Settings;
import edu.stevens.cs522.chat.ui.MessageAdapter;
import edu.stevens.cs522.chat.ui.MessageSenderAdapter;
import edu.stevens.cs522.chat.ui.TextAdapter;
import edu.stevens.cs522.chat.viewmodels.ChatViewModel;
import edu.stevens.cs522.chat.viewmodels.SharedViewModel;

public class MessagesFragment extends Fragment implements OnClickListener {

    private final static String TAG = MessagesFragment.class.getCanonicalName();

    public interface IChatListener {
        void sendMessageDialog(Chatroom chatroom);
    }

    // Used to launch a dialog for sending a message
    private IChatListener listener;

    // Header for messages fragment
    private TextView messagesHeader;

    // Query the messages database
    private ChatViewModel chatViewModel;

    // Need to remove observer when we leave chatroom
    private LiveData<List<Message>> messages;

    // Current chatroom selection, shared between activity and messages fragment
    private SharedViewModel sharedViewModel;

    // Display list of messages in a chatroom (with senders identified in message headings)
    private MessageAdapter messagesAdapter;


    public MessagesFragment() {
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof IChatListener) {
            listener = (IChatListener) context;
        } else {
            throw new IllegalStateException("Activity must implement IChatListener!");
        }
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.view_messages, container, false);

        /*
         * Use the floating action button to produce the dialog for sending a message
         */
        FloatingActionButton sendButton = rootView.findViewById(R.id.next_button);
        sendButton.setOnClickListener(this);

        /*
         * Header for list of messages
         */
        messagesHeader = rootView.findViewById(R.id.messages_heading);

        /*
         * Widget for list of messages
         */
        RecyclerView messageList = rootView.findViewById(R.id.message_list);
        messageList.setLayoutManager(new LinearLayoutManager(requireActivity()));

        // TODO Initialize the recyclerview and adapter for messages
        messagesAdapter = new MessageAdapter() {
            @Override
            public String getHeading(Message message) {
                return message.getSender();
            }
        };
        messageList.setAdapter(messagesAdapter);
        return rootView;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // TODO get the view models
        chatViewModel = new ViewModelProvider(requireActivity()).get(ChatViewModel.class);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // Rely on live data to requery the messages if the chatroom selection changes
        queryMessages(sharedViewModel.getSelected());
        sharedViewModel.observe(getViewLifecycleOwner(), this::queryMessages);
    }

    private void queryMessages(Chatroom chatroom) {
        // Set the name of the chatroom in the messages view.
        Chatroom currentChatroom = sharedViewModel.getSelected();
        String chatroomName = (currentChatroom != null) ? currentChatroom.name : "";
        String header = getString(R.string.messages_heading, Settings.getSenderName(requireActivity()), chatroomName);
        messagesHeader.setText(header);

        if (chatroom == null) {
            // messagesAdapter.notifyItemRangeChanged(0, 1);
            messagesAdapter.notifyItemRangeRemoved(0, messagesAdapter.getItemCount());
            messagesAdapter.setMessage(new ArrayList<>(0));
            // TODO remove any observers of messages as we leave the chatroom
            if (messages != null) {
                messages.removeObservers(getViewLifecycleOwner());
                messages = null;
            }
            return;
        }

        // TODO query the database asynchronously, and use messagesAdapter to display the result
        // The messages live data will need an observer for when new messages are inserted.
        messages = chatViewModel.fetchAllMessages(chatroom);
        messages.observe(getViewLifecycleOwner(), messages -> {
            for (Message msg : messages) {
                Log.d("MessagesFragment", "Message: " + msg.messageText);
            }
            messagesAdapter.setMessage(messages);
            messagesAdapter.notifyDataSetChanged();
        });
    }

    public void onResume() {
        super.onResume();
    }

    public void onPause() {
        super.onPause();
    }

    public void onDestroy() {
        super.onDestroy();
    }

    /*
     * Callback for the NEXT button.
     */
    public void onClick(View v) {
        Chatroom currentChatroom = sharedViewModel.getSelected();
        if (currentChatroom != null) {
            listener.sendMessageDialog(currentChatroom);
        }
    }
}
