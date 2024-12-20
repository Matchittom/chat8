package edu.stevens.cs522.chat.services;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;

import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import javax.annotation.Nonnull;

public class PostMessageResultReceiver extends ResultReceiver implements DefaultLifecycleObserver {

    public final static String TAG = PostMessageResultReceiver.class.getCanonicalName();

    public interface IReceive {
        public void onReceiveResult(int resultCode, Bundle data);
    }

    protected LifecycleOwner lifecycleOwner;

    protected IReceive receiver;

    public PostMessageResultReceiver(Handler handler, LifecycleOwner lifecycleOwner, IReceive receiver) {
        super(handler);
        /*
         * Save the observer of the result.
         */
        this.receiver = receiver;
        /*
         * The observer's lifecycle, remove the observer when its state becomes DESTROYED
         */
        this.lifecycleOwner = lifecycleOwner;
        this.lifecycleOwner.getLifecycle().addObserver(this);
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle data) {
        if (lifecycleOwner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            Log.d(TAG, "Result observer is active, delivering result="+resultCode);
            receiver.onReceiveResult(resultCode, data);
        } else {
            Log.d(TAG, "Result observer is not active, so missing the result!");
        }
    }

    @Override
    public void onDestroy(@Nonnull LifecycleOwner owner) {
        this.receiver = null;
        owner.getLifecycle().removeObserver(this);
    }
}
