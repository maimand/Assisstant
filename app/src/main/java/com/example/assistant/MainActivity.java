package com.example.assistant;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.example.assistant.Model.Assisstant;
import com.example.assistant.Model.Assisstant1;
import com.example.assistant.Model.Message;
import com.example.assistant.ViewModel.MessageAdapter;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {
    ArrayList<Message> messages;
    MessageAdapter adapter;
    TextToSpeech t1;
    ListView listView;
    ImageButton button;
    private final int REQUEST_IMAGE_CAPTURE = 1;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    String BASE_URL = "http://f9404a230da4.ngrok.io";
    String name;
    boolean isFace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = findViewById(R.id.messages_view);
        button = findViewById(R.id.btnSpeak);
        messages = new ArrayList<>();
        adapter = new MessageAdapter(messages, this);
        t1 = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                }
            }
        });
        listView.setAdapter(adapter);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        AISpeak("Hello. How are you?");
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        t1.speak("Say something", TextToSpeech.QUEUE_ADD, null);
                        Log.d("debug", "Start Recording.");
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {

                                promptSpeechInput();
                            }
                        }, 1000);
//                        promptSpeechInput();
                        return true;
                    case MotionEvent.ACTION_UP:
                        Log.d("debug", "Stop Recording.");

                        break;
                }
                return false;
            }
        });

    }

    public void AISpeak(String response){
        messages.add(new Message(response, false, null));
        t1.speak(response, TextToSpeech.QUEUE_FLUSH, null);
        adapter.notifyDataSetChanged();
        listView.smoothScrollToPosition(adapter.getCount());
    }

    public void ISpeak(String response){
        messages.add(new Message(response, true, null));
        adapter.notifyDataSetChanged();
        listView.smoothScrollToPosition(adapter.getCount());
    }

    public void IShowPhoto(Bitmap image){
        messages.add(new Message("", true, image));
        adapter.notifyDataSetChanged();
        listView.smoothScrollToPosition(adapter.getCount());
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Say something…");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    "Don't support your language.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String response = result.get(0);
                    ISpeak(response);
                    Log.e("face1", isFace?"true":"false");
                    if(isFace){
                        name = response;
                        saveData(name);
                        dispatchTakePictureIntent();
                    }else{
                        if(response.contains("face")){
                            saveData(true);
                            loadData();
                            Log.e("face2",isFace?"true":"false" );
                            AISpeak("Face Register");
                            AISpeak("What is his or her name?");
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {

                                    promptSpeechInput();
                                }
                            }, 2000);

                        }
                        else if (response.contains("photo")) {
                            dispatchTakePictureIntent();
                        }
                        else {
                            new sendQuestion(response).execute();
                        }
                    }
                }
                break;
            }

            case REQUEST_IMAGE_CAPTURE:
                loadData();
                if (resultCode == RESULT_OK && null != data) {
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    if (imageBitmap == null) {
                        Log.e("Camera:", "not found");
                    } else {
                        if(isFace){
                            saveData(false);
                            loadData();
                            Log.e(TAG, isFace?"true":"false" );
                            IShowPhoto(imageBitmap);
                            new RegisterFace(imageBitmap, name).execute();

                        }else {
                            IShowPhoto(imageBitmap);
                            new SendImage(imageBitmap).execute();
                        }
                    }
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + requestCode);
        }
    }
    public void saveData(String newname) {
        SharedPreferences sharedPreferences = getSharedPreferences("namesharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("name", newname);
        editor.apply();
    }
    public void saveData(boolean isFace) {
        SharedPreferences sharedPreferences = getSharedPreferences("facesharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isFace", isFace);
        editor.apply();
    }
    public void loadData() {
        SharedPreferences namesharedPrefs = getSharedPreferences("namesharedPrefs", MODE_PRIVATE);
        SharedPreferences facesharedPrefs = getSharedPreferences("facesharedPrefs", MODE_PRIVATE);
        name = namesharedPrefs.getString("name", "");
        isFace = facesharedPrefs.getBoolean("isFace", false);
    }

    public void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } catch (ActivityNotFoundException e) {
        }
    }

    class sendQuestion extends AsyncTask<Void, Void, String>{
        String question;

        public sendQuestion(String question) {
            this.question = question;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            AISpeak(s);
        }

        @Override
        protected String doInBackground(Void... voids) {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            MediaType mediaType = MediaType.parse("text/plain");
            RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("question", question)
                    .build();
            Request request = new Request.Builder()
                    .url(BASE_URL+"/ask")
                    .method("POST", body)
                    .build();
            try {
                Gson gson = new Gson();
                ResponseBody response = client.newCall(request).execute().body();
                Assisstant assisstant = gson.fromJson(response.string(), Assisstant.class);
                return assisstant.result;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;

        }
    }

    class RegisterFace extends AsyncTask<Void, Void, Integer>{
        Bitmap bitmap;
        String name;

        public RegisterFace(Bitmap bitmap, String name) {
            this.bitmap = bitmap;
            this.name = name;
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            File file ;
            try {

                file = new File(Environment.getExternalStorageDirectory() + File.separator + System.currentTimeMillis()+".png");
                Log.e(TAG, file.getPath());
                file.createNewFile();

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
                byte[] bitmapdata = bos.toByteArray();

                FileOutputStream fos = new FileOutputStream(file);
                fos.write(bitmapdata);
                fos.flush();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
                return -1; // it will return null
            }
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            MediaType mediaType = MediaType.parse("text/plain");
            RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file",file.getName(),
                            RequestBody.create(MediaType.parse("application/octet-stream"),
                                    file))
                    .addFormDataPart("name", name)
                    .build();
            Request request = new Request.Builder()
                    .url(BASE_URL+"/register-face")
                    .method("POST", body)
                    .build();
            try {
                Gson gson = new Gson();
                ResponseBody response = client.newCall(request).execute().body();
                Assisstant1 assisstant = gson.fromJson(response.string(), Assisstant1.class);
                return assisstant.result;
            } catch (IOException e) {
                e.printStackTrace();
            }
            file.delete();
            return null;
        }

        @Override
        protected void onPostExecute(Integer i) {
            super.onPostExecute(i);
            if(i!=-1){
                AISpeak("Sucessfully register.");
            }else {
                AISpeak("Cannot register face.");
            }
        }
    }

    class SendImage extends AsyncTask<Void, Void, String> {
        Bitmap bitmap;

        public SendImage(Bitmap bitmap) {
            this.bitmap = bitmap;
        }


        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
           AISpeak(s);

        }

        @Override
        protected String doInBackground(Void... voids) {
            File file ;
            try {

                file = new File(Environment.getExternalStorageDirectory() + File.separator + System.currentTimeMillis()+".png");
                file.createNewFile();

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
                byte[] bitmapdata = bos.toByteArray();

                FileOutputStream fos = new FileOutputStream(file);
                fos.write(bitmapdata);
                fos.flush();
                fos.close();
            } catch (Exception e) {
                Log.e(TAG, e.getLocalizedMessage());
                e.printStackTrace();
                return "Cant save file"; // it will return null
            }
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            MediaType mediaType = MediaType.parse("text/plain");
            RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getName(),
                            RequestBody.create(MediaType.parse("application/octet-stream"),
                                    file))
                    .build();
            Request request = new Request.Builder()
                    .url(BASE_URL+"/generate-caption")
                    .method("POST", body)
                    .build();
            try {
                Gson gson = new Gson();
                ResponseBody response = client.newCall(request).execute().body();
                Assisstant assisstant = gson.fromJson(response.string(), Assisstant.class);
                return assisstant.result;
            } catch (IOException e) {
                e.printStackTrace();
            }
            file.delete();
            return null;
        }
    }

}
