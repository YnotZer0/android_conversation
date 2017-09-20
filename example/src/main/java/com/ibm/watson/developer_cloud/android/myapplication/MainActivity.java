/*
 * Copyright 2017 IBM Corp. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.ibm.watson.developer_cloud.android.myapplication;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.HashMap;
//import java.util.List;
import java.util.Map;

//import com.google.gson.Gson;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneHelper;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneInputStream;
import com.ibm.watson.developer_cloud.android.library.audio.StreamPlayer;
import com.ibm.watson.developer_cloud.android.library.audio.utils.ContentType;
import com.ibm.watson.developer_cloud.language_translator.v2.LanguageTranslator;
import com.ibm.watson.developer_cloud.language_translator.v2.model.Language;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechResults;
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.BaseRecognizeCallback;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.Voice;
import com.ibm.watson.developer_cloud.conversation.v1.ConversationService;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageRequest;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageResponse;
import com.ibm.watson.developer_cloud.http.ServiceCallback;

//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {
  private final String TAG = "MainActivity";

  private RadioGroup targetLanguage;
  private EditText input;
  private ImageButton mic;
  private Button translate;
  private ImageButton play;
  private TextView translatedText;
  private Button conv;
  private Button clear;

  private SpeechToText speechService;
  private TextToSpeech textService;
  private LanguageTranslator translationService;
  private Language selectedTargetLanguage = Language.SPANISH;
  private static ConversationService conversationService;

  private StreamPlayer player = new StreamPlayer();
  private MicrophoneHelper microphoneHelper;

  private MicrophoneInputStream capture;
  private boolean listening = false;

  private Handler handler = new Handler();
  public ListView msgView;
  public ArrayAdapter<String> msgList;
  Map context = new HashMap();



  /**
   * On create.
   *
   * @param savedInstanceState the saved instance state
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    microphoneHelper = new MicrophoneHelper(this);
    speechService = initSpeechToTextService();
    textService = initTextToSpeechService();
    translationService = initLanguageTranslatorService();
    conversationService = initConversationService();
    // set the workspace id for WCS
    final String inputWorkspaceId = getString(R.string.conversation_workspaceId);

    msgView = (ListView) findViewById(R.id.listView);
    msgList = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
    msgView.setAdapter(msgList);

    targetLanguage = (RadioGroup) findViewById(R.id.target_language);
    input = (EditText) findViewById(R.id.input);
    mic = (ImageButton) findViewById(R.id.mic);
    translate = (Button) findViewById(R.id.translate);
    play = (ImageButton) findViewById(R.id.play);
    translatedText = (TextView) findViewById(R.id.translated_text);
    conv = (Button) findViewById(R.id.conv_button);
    clear = (Button) findViewById(R.id.clear_button);

    MessageResponse response = null;
    conversationAPI(String.valueOf(input.getText()), context, inputWorkspaceId);

    targetLanguage.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
          case R.id.spanish:
            selectedTargetLanguage = Language.SPANISH;
            break;
          case R.id.french:
            selectedTargetLanguage = Language.FRENCH;
            break;
          case R.id.italian:
            selectedTargetLanguage = Language.ITALIAN;
            break;
        }
      }
    });

    input.addTextChangedListener(new EmptyTextWatcher() {
      @Override
      public void onEmpty(boolean empty) {
        if (empty) {
          translate.setEnabled(false);
        } else {
          translate.setEnabled(true);
        }
      }
    });

    mic.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        // mic.setEnabled(false);
        if (!listening) {
          capture = microphoneHelper.getInputStream(true);
          new Thread(new Runnable() {
            @Override
            public void run() {
              try {
                speechService.recognizeUsingWebSocket(capture, getRecognizeOptions(),
                    new MicrophoneRecognizeDelegate());
              } catch (Exception e) {
                showError(e);
              }
            }
          }).start();
          listening = true;
        } else {
          microphoneHelper.closeInputStream();
          listening = false;
        }
      }
    });

    translate.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        new TranslationTask().execute(input.getText().toString());
      }
    });

    translatedText.addTextChangedListener(new EmptyTextWatcher() {
      @Override
      public void onEmpty(boolean empty) {
        if (empty) {
          play.setEnabled(false);
        } else {
          play.setEnabled(true);
        }
      }
    });


    conv.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        //pressing the [Send] button passes the text to the WCS conversation service
        MessageResponse response = null;
        conversationAPI(String.valueOf(input.getText()), context, inputWorkspaceId);
      }
    });

    clear.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        //pressing the [Clear] button should clear the context
        //in essence starting the conversation again from scratch
        context = new HashMap();
        //should also clear the msgList
        msgList.clear();;
        msgView.setAdapter(msgList);
        msgView.smoothScrollToPosition(msgList.getCount() - 1);
        //invoke the initial message from WCS
        MessageResponse response = null;
        conversationAPI(String.valueOf(input.getText()), context, inputWorkspaceId);
      }
    });


    play.setEnabled(false);

    play.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        new SynthesisTask().execute(translatedText.getText().toString());
      }
    });

  }


  private void showTranslation(final String translation) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        translatedText.setText(translation);
      }
    });
  }

  private void showError(final Exception e) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        e.printStackTrace();
      }
    });
  }

  private void showMicText(final String text) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        input.setText(text);
      }
    });
  }

  private void enableMicButton() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        mic.setEnabled(true);
      }
    });
  }

  private SpeechToText initSpeechToTextService() {
    SpeechToText service = new SpeechToText();
    String username = getString(R.string.speech_text_username);
    String password = getString(R.string.speech_text_password);
    service.setUsernameAndPassword(username, password);
    service.setEndPoint(getString(R.string.speech_text_url));
    return service;
  }

  private TextToSpeech initTextToSpeechService() {
    TextToSpeech service = new TextToSpeech();
    String username = getString(R.string.text_speech_username);
    String password = getString(R.string.text_speech_password);
    service.setUsernameAndPassword(username, password);
    service.setEndPoint(getString(R.string.text_speech_url));
    return service;
  }

  private LanguageTranslator initLanguageTranslatorService() {
    LanguageTranslator service = new LanguageTranslator();
    String username = getString(R.string.language_translator_username);
    String password = getString(R.string.language_translator_password);
    service.setUsernameAndPassword(username, password);
    service.setEndPoint(getString(R.string.language_translator_url));
    return service;
  }

  private ConversationService initConversationService() {
    ConversationService service = new ConversationService(ConversationService.VERSION_DATE_2016_07_11);
    String username = getString(R.string.conversation_username);
    String password = getString(R.string.conversation_password);
    service.setUsernameAndPassword(username, password);
    service.setEndPoint(getString(R.string.conversation_url));
    return service;
  }

  private RecognizeOptions getRecognizeOptions() {
    return new RecognizeOptions.Builder().continuous(true).contentType(ContentType.OPUS.toString())
        .model("en-US_BroadbandModel").interimResults(true).inactivityTimeout(2000).build();
  }

  private abstract class EmptyTextWatcher implements TextWatcher {
    private boolean isEmpty = true; // assumes text is initially empty

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
      if (s.length() == 0) {
        isEmpty = true;
        onEmpty(true);
      } else if (isEmpty) {
        isEmpty = false;
        onEmpty(false);
      }
    }

    @Override
    public void afterTextChanged(Editable s) {}

    public abstract void onEmpty(boolean empty);
  }

  private class MicrophoneRecognizeDelegate extends BaseRecognizeCallback {

    @Override
    public void onTranscription(SpeechResults speechResults) {
      System.out.println(speechResults);
      if (speechResults.getResults() != null && !speechResults.getResults().isEmpty()) {
        String text = speechResults.getResults().get(0).getAlternatives().get(0).getTranscript();
        showMicText(text);
      }
    }

    @Override
    public void onError(Exception e) {
      showError(e);
      enableMicButton();
    }

    @Override
    public void onDisconnected() {
      enableMicButton();
    }
  }

  private class TranslationTask extends AsyncTask<String, Void, String> {

    @Override
    protected String doInBackground(String... params) {
      showTranslation(translationService.translate(params[0], Language.ENGLISH, selectedTargetLanguage).execute()
          .getFirstTranslation());
      return "Did translate";
    }
  }

  private class SynthesisTask extends AsyncTask<String, Void, String> {

    @Override
    protected String doInBackground(String... params) {
      player.playStream(textService.synthesize(params[0], Voice.EN_LISA).execute());
      return "Did synthesize";
    }
  }

  /**
   * On request permissions result.
   *
   * @param requestCode the request code
   * @param permissions the permissions
   * @param grantResults the grant results
   */
  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    switch (requestCode) {
      case MicrophoneHelper.REQUEST_PERMISSION: {
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
          Toast.makeText(this, "Permission to record audio denied", Toast.LENGTH_SHORT).show();
        }
      }
    }
  }

  /**
   * On activity result.
   *
   * @param requestCode the request code
   * @param resultCode the result code
   * @param data the data
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    //do nothing special
  }


  public void conversationAPI(String input, Map context, String workspaceId) {

    //conversationService
    MessageRequest newMessage = new MessageRequest.Builder()
            .inputText(input).context(context).build();

    //cannot use the following as it will attempt to run on the UI thread and crash
//    MessageResponse response = conversationService.message(workspaceId, newMessage).execute();

    //use the following so it runs on own async thread
    //then when get a response it calls displayMsg that will update the UI
    conversationService.message(workspaceId, newMessage).enqueue(new ServiceCallback<MessageResponse>() {
      @Override
      public void onResponse(MessageResponse response) {
        //output to system log output, just for verification/checking
        System.out.println(response);
        displayMsg(response);
      }
      @Override
      public void onFailure(Exception e) {
        showError(e);
      }
    });
  };

  public void displayMsg(MessageResponse msg)
  {
    final MessageResponse mssg=msg;
    handler.post(new Runnable() {

      @Override
      public void run() {

        //from the WCS API response
        //https://www.ibm.com/watson/developercloud/conversation/api/v1/?java#send_message
        //extract the text from output to display to the user
        String text = mssg.getText().get(0);

        //now output the text to the UI to show the chat history
        msgList.add(text);
        msgView.setAdapter(msgList);
        msgView.smoothScrollToPosition(msgList.getCount() - 1);

        //set the context, so that the next time we call WCS we pass the accumulated context
        context = mssg.getContext();

        //rather than converting response to a JSONObject and parsing through it
        //we can use the APIs for the MessageResponse .getXXXXX() to get the values as shown above
        //keeping the following just in case need this at a later date
        //
        //          https://developer.android.com/reference/org/json/JSONObject.html
/*
          JSONObject jObject = new JSONObject(mssg);
          JSONObject jsonOutput = jObject.getJSONObject("output");
          JSONArray jArray1 = jsonOutput.getJSONArray("text");
          for (int i=0; i < jArray1.length(); i++)
          {
            try {
              String textContent = String.valueOf(jArray1.getString(i));
              System.out.println(textContent);
              msgList.add(textContent);
              msgView.setAdapter(msgList);
              msgView.smoothScrollToPosition(msgList.getCount() - 1);
            } catch (JSONException e) {
              // Oops
              System.out.println(e);
            }
          }
          JSONArray jArray2 = jObject.getJSONArray("intents");
          for (int i=0; i < jArray2.length(); i++)
          {
            try {
              JSONObject oneObject = jArray2.getJSONObject(i);
              // Pulling items from the array
              String oneObjectsItem = oneObject.getString("confidence");
              String oneObjectsItem2 = oneObject.getString("intent");
              String jOutput = oneObjectsItem+" : "+oneObjectsItem2;
              msgList.add(jOutput);
              msgView.setAdapter(msgList);
              msgView.smoothScrollToPosition(msgList.getCount() - 1);
            } catch (JSONException e) {
              // Oops
            }
          }
*/
      }
    });

  };

}
