package com.example.assistant.Model;

import android.graphics.Bitmap;

public class Message {
    public final String text;
    public final boolean belongsToMe;
    public final Bitmap image;
    public Message(String text, boolean belongsToMe, Bitmap image) {
        this.text = text;
        this.belongsToMe = belongsToMe;
        this.image = image;
    }
}
