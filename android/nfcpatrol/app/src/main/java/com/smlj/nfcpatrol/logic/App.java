package com.smlj.nfcpatrol.logic;

import android.app.Application;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        /*SocketIoManager.getInstance().connect("https://example.com", "user_token");*/
    }
}
