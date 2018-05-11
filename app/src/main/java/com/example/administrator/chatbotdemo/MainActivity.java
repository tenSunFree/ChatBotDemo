package com.example.administrator.chatbotdemo;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private final int REQ_CODE_SPEECH_INPUT = 100;
    private ImageButton btnSpeakImageButton;
    private TextView txtSpeechInputTextView, outputTexTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtSpeechInputTextView = findViewById(R.id.txtSpeechInputTextView);
        outputTexTextView = findViewById(R.id.outputTexTextView);

        btnSpeakImageButton = findViewById(R.id.btnSpeakImageButton);
        btnSpeakImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptSpeechInput();
            }
        });
    }

    /**
     * Showing google speech input dialog
     */
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);                    // 通过Intent传递语音识别的模式, 开启语音
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,                                 // 语言模式和自由模式的语音识别
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say Something");                   // 提示语音开始
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);                                  // 开始语音识别
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    "orry! Your device doesn\\'t support speech input",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Receiving speech input
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String userQuery = result.get(0);
                    txtSpeechInputTextView.setText(userQuery);
                    RetrieveFeedTask task = new RetrieveFeedTask();
                    task.execute(userQuery);
                }
                break;
            }
        }
    }

    /**
     * AsyncTask<Params, Progress, Result>
     * Params : 輸入參數的傳入值, 並且在doInBackground內被傳入可使用
     * Progress : 用來更新處理進度, 用pulishProgress傳入onProgressUpdate內
     * Result : 在執行完doInBackground後, return的值, 會傳到onPostExecute內
     */
    class RetrieveFeedTask extends AsyncTask<String, Void, String> {

        /**
         * 在Worker Thread執行, 用來處理繁重的工作
         */
        @Override
        protected String doInBackground(String... voids) {
            String s = null;
            try {
                s = GetText(voids[0]);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                Log.d("more", "Exception occurred " + e);
            }
            return s;
        }

        /**
         * 在UI Thread執行, 在任務執行前會先進入, 通常用來放執行前的Progressbar
         */
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            outputTexTextView.setText(s);
        }
    }

    /**
     * Create GetText Metod
     */
    public String GetText(String query) throws UnsupportedEncodingException {
        String text = "";
        BufferedReader reader = null;

        /** Send data */
        try {
            /** Defined URL  where to send data */
            URL url = new URL("https://api.api.ai/v1/query?v=20150910");

            /** Send POST data request */
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Authorization", "Bearer " + "填上Client access token");
            conn.setRequestProperty("Content-Type", "application/json");

            /** Create JSONObject here */
            JSONObject jsonParam = new JSONObject();
            JSONArray queryArray = new JSONArray();
            queryArray.put(query);
            jsonParam.put("query", queryArray);
            jsonParam.put("lang", "en");
            jsonParam.put("sessionId", "1234567890");
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(jsonParam.toString());
            wr.flush();

            /** Get the server response */
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line = null;

            /** Read Server Response */
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");                                                             // Append server response in string
            }
            text = sb.toString();
            JSONObject object1 = new JSONObject(text);
            JSONObject object = object1.getJSONObject("result");
            JSONObject fulfillment = null;
            String speech = null;
            fulfillment = object.getJSONObject("fulfillment");
            speech = fulfillment.optString("speech");
            return speech;
        } catch (Exception ex) {
            Log.d("more", "exception at last " + ex);
        } finally {
            try {
                reader.close();
            } catch (Exception ex) {
            }
        }
        return null;
    }
}
