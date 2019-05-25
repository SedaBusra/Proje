package com.gokhan.connecttoarduino;

import android.os.Handler;
import android.os.Message;

public class CustomHandler extends Handler {
    private AppHandlerReceiver appHandlerReceiver;

    public CustomHandler(AppHandlerReceiver receiver) {
        appHandlerReceiver = receiver;
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        appHandlerReceiver.onMessageResult(msg);
    }

    public interface AppHandlerReceiver {
        void onMessageResult(Message message);
    }
}
