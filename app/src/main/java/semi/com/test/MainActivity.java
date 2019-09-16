/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package semi.com.test;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements MessageDialogFragment.Listener,View.OnClickListener{

    private static final String FRAGMENT_MESSAGE_DIALOG = "message_dialog";

    private static final String STATE_RESULTS = "results";

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;

    private SpeechService mSpeechService;

    private VoiceRecorder mVoiceRecorder;

    private String mFilePath = null;
    private int mFileNum = 0;
    FileOutputStream fos = null;

    String fileNm="";
    private final VoiceRecorder.Callback mVoiceCallback = new VoiceRecorder.Callback() {

        @Override
        public void onVoiceStart() {
            showStatus(true);
            mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() +"/record"+mFileNum+".pcm";
            try {
                fos = new FileOutputStream(mFilePath);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onVoice(byte[] data, int size) {
            try {
                fos.write(data,0,size);
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        @Override
        public void onVoiceEnd() {
            voiceEnd();
        }

    };
        private void voiceEnd(){
            showStatus(false);
            try {
                fos.close();
                File file = new File(mFilePath);
                mSpeechService.recognizeInputStream(new FileInputStream(file));
                mFileNum++;
                file.delete();
                handler.sendEmptyMessage(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what==1) {
                mPopupVw.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            }else {
                mCloseBtn.performClick();
            }
        }
    };

    // Resource caches
    private int mColorHearing;
    private int mColorNotHearing;

    // View references
    private TextView mStatus;
    private TextView mText;
    private ResultAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private RelativeLayout mPopupVw,mLangPopupVw;
    private Button mSpeechBtn,mSelLangBtn,mCloseBtn,mkoreaBtn,mEnglishBtn,mChineseBtn,mVIetnamBtn;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            mSpeechService = SpeechService.from(binder);
            mSpeechService.addListener(mSpeechServiceListener);
            mStatus.setVisibility(View.VISIBLE);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSpeechService = null;
        }

    };

    private final String[] lauguageList = {"ko-KR","en-US","zh-TW","vi-VN"};//한,영,중,베트남
    static String selLang= "";
    ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Resources resources = getResources();
        final Resources.Theme theme = getTheme();
        mColorHearing = ResourcesCompat.getColor(resources, R.color.status_hearing, theme);
        mColorNotHearing = ResourcesCompat.getColor(resources, R.color.status_not_hearing, theme);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        mStatus = (TextView) findViewById(R.id.status);
        mText = (TextView) findViewById(R.id.text);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        final ArrayList<String> results = savedInstanceState == null ? null :
                savedInstanceState.getStringArrayList(STATE_RESULTS);
        mAdapter = new ResultAdapter(results);
        mRecyclerView.setAdapter(mAdapter);

        mLangPopupVw = findViewById(R.id.popupLang);
        mLangPopupVw.setVisibility(View.GONE);

        mPopupVw = findViewById(R.id.popup);
        mPopupVw.setVisibility(View.GONE);

        mSelLangBtn = findViewById(R.id.selLang);
        mSpeechBtn = findViewById(R.id.speech);
        mCloseBtn = findViewById(R.id.close);

        mkoreaBtn = findViewById(R.id.korean);
        mEnglishBtn = findViewById(R.id.english);
        mChineseBtn = findViewById(R.id.chinese);
        mVIetnamBtn = findViewById(R.id.vietnam);

        mSelLangBtn.setOnClickListener(this);
        mSpeechBtn.setOnClickListener(this);
        mCloseBtn.setOnClickListener(this);
        mkoreaBtn.setOnClickListener(this);
        mEnglishBtn.setOnClickListener(this);
        mChineseBtn.setOnClickListener(this);
        mVIetnamBtn.setOnClickListener(this);

        progressBar = findViewById(R.id.pg);
        progressBar.setVisibility(View.GONE);

        selLang = lauguageList[0];
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Prepare Cloud Speech API

        // Start listening to voices
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {

        }  else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.RECORD_AUDIO)) {
            showPermissionMessageDialog();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    @Override
    protected void onStop() {
        // Stop listening to voice
        stopVoiceRecorder();


        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null) {
            outState.putStringArrayList(STATE_RESULTS, mAdapter.getResults());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (permissions.length == 1 && grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                showPermissionMessageDialog();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_file:
                mSpeechService.recognizeInputStream(getResources().openRawResource(R.raw.audio));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
        }
        mVoiceRecorder = new VoiceRecorder(mVoiceCallback);
        mVoiceRecorder.start();
    }

    private void stopVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
            mVoiceRecorder = null;
        }
    }

    private void showPermissionMessageDialog() {
        MessageDialogFragment
                .newInstance(getString(R.string.permission_message))
                .show(getSupportFragmentManager(), FRAGMENT_MESSAGE_DIALOG);
    }

    private void showStatus(final boolean hearingVoice) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStatus.setTextColor(hearingVoice ? mColorHearing : mColorNotHearing);
            }
        });
    }

    @Override
    public void onMessageDialogDismissed() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_RECORD_AUDIO_PERMISSION);
    }

    private final SpeechService.Listener mSpeechServiceListener =
            new SpeechService.Listener() {
                @Override
                public void onSpeechRecognized(final String text, final boolean isFinal) {
                    Log.e("semi","onSpeechRecognized");
                    handler.sendEmptyMessage(2);
                    if (isFinal) {
                        mVoiceRecorder.dismiss();
                    }
                    if (mText != null && !TextUtils.isEmpty(text)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isFinal) {
                                    mText.setText(null);
                                    mAdapter.addResult(text);
                                    mRecyclerView.smoothScrollToPosition(0);
                                } else {
                                    mText.setText(text);
                                }
                            }
                        });
                    }
                }
            };

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.selLang:
                if(mLangPopupVw.getVisibility()==View.GONE){
                    mLangPopupVw.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.speech:
                if(mPopupVw.getVisibility()==View.GONE){
                    mPopupVw.setVisibility(View.VISIBLE);
                    bindService(new Intent(this, SpeechService.class), mServiceConnection, BIND_AUTO_CREATE);
                    startVoiceRecorder();
                }
                break;

            case R.id.close:
                // Stop Cloud Speech API
                mSpeechService.removeListener(mSpeechServiceListener);
                unbindService(mServiceConnection);
                mSpeechService = null;
                stopVoiceRecorder();
                mPopupVw.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                break;
            case R.id.korean:
                mLangPopupVw.setVisibility(View.GONE);
                selLang = lauguageList[0];
                mSelLangBtn.setText("한국어");
                break;
            case R.id.english:
                mLangPopupVw.setVisibility(View.GONE);
                selLang = lauguageList[1];
                mSelLangBtn.setText("English");
                break;
            case R.id.chinese:
                mLangPopupVw.setVisibility(View.GONE);
                selLang = lauguageList[2];
                mSelLangBtn.setText("中国话");
                break;
            case R.id.vietnam:
                mLangPopupVw.setVisibility(View.GONE);
                selLang = lauguageList[3];
                mSelLangBtn.setText("Tiếng Việt");
                break;
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {

        TextView text;

        ViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.item_result, parent, false));
            text = (TextView) itemView.findViewById(R.id.text);
        }

    }

    private static class ResultAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final ArrayList<String> mResults = new ArrayList<>();

        ResultAdapter(ArrayList<String> results) {
            if (results != null) {
                mResults.addAll(results);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.text.setText(mResults.get(position));
        }

        @Override
        public int getItemCount() {
            return mResults.size();
        }

        void addResult(String result) {
            mResults.add(0, result);
            notifyItemInserted(0);
        }

        public ArrayList<String> getResults() {
            return mResults;
        }

    }

}