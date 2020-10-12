package com.example.assistant.Model;

import com.google.gson.annotations.SerializedName;

public class Assisstant {
    @SerializedName("result")
    public final String result;

    public Assisstant(String result) {
        this.result = result;
    }
}
