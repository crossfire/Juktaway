package info.justaway;


import java.io.File;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;

import android.R.color;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class PostActivity extends Activity {

    private Twitter mTwitter;
    private EditText mEditText;
    private TextView mTextView;
    private Button mTweetButton;
    private Button mImgButton;
    private Button mSuddenlyButton;
    private ProgressDialog mProgressDialog;
    private Long inReplyToStatusId;
    private File imgPath;

    final Context c = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        JustawayApplication application = JustawayApplication.getApplication();

        Typeface fontello = Typeface.createFromAsset(getAssets(), "fontello.ttf");

        mEditText = (EditText) findViewById(R.id.status);
        mTextView = (TextView) findViewById(R.id.count);
        mTweetButton = (Button) findViewById(R.id.tweet);
        mImgButton = (Button) findViewById(R.id.img);
        mSuddenlyButton = (Button) findViewById(R.id.suddenly);
        mTwitter = application.getTwitter();

        mTweetButton.setTypeface(fontello);
        mImgButton.setTypeface(fontello);
        mSuddenlyButton.setTypeface(fontello);

        Intent intent = getIntent();
        String status = intent.getStringExtra("status");
        if (status != null) {
            mEditText.setText(status);
        }
        int selection = intent.getIntExtra("selection", 0);
        if (selection > 0) {
            mEditText.setSelection(selection);
        }
        inReplyToStatusId = intent.getLongExtra("inReplyToStatusId", 0);

        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            String text = intent.getData().getQueryParameter("text");
            String url = intent.getData().getQueryParameter("url");
            String hashtags = intent.getData().getQueryParameter("hashtags");
            if (text == null) {
                text = "";
            }
            if (url != null) {
                text += " " + url;
            }
            if (hashtags != null) {
                text += " #" + hashtags;
            }
            mEditText.setText(text);
        }

        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            if (intent.getExtras().get(Intent.EXTRA_STREAM) != null) {
                Uri imgUri = (Uri) intent.getExtras().get(Intent.EXTRA_STREAM);
                uriToFile(imgUri);
            } else {
                String pageUri = intent.getExtras().getString(Intent.EXTRA_TEXT);
                String pageTitle = intent.getExtras().getString(Intent.EXTRA_SUBJECT);
                if (pageTitle == null) {
                    pageTitle = "";
                }
                if (pageUri != null) {
                    pageTitle += " " + pageUri;
                }
                mEditText.setText(pageTitle);
            }
        }

        mTweetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgressDialog("送信中！！１１１１１");
                StatusUpdate super_sugoi = new StatusUpdate(mEditText.getText().toString());
                if (inReplyToStatusId > 0) {
                    super_sugoi.setInReplyToStatusId(inReplyToStatusId);
                }
                if (imgPath != null) {
                    super_sugoi.setMedia(imgPath);
                }
                new PostTask().execute(super_sugoi);
            }
        });

        mSuddenlyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int selectStart = mEditText.getSelectionStart();
                int selectEnd = mEditText.getSelectionEnd();
                String text = mEditText.getText().toString();
                String totsuzen = text.substring(selectStart, selectEnd) + "\n";
                String prefix = text.substring(0, selectStart);
                String suffix = text.substring(selectEnd);
                int i;
                String ue = "";
                String shita = "";
                int j = 0;
                String gentotsu = "";

                // 改行文字がある場所を見つけて上と下を作る
                for (i = 0; totsuzen.charAt(i) != '\n'; i++) {
                    int codeunit = totsuzen.codePointAt(i);
                    if (0xffff < codeunit) {
                        i++;
                    }
                    ue += "人";
                    shita += "^Y";
                }
                // 突然死したいテキストの文字をひとつづつ見る
                for (i = 0; totsuzen.length() > i; i++) {
                    // 一文字取り出して改行文字なのかチェック
                    if (totsuzen.charAt(i) == '\n') {
                        String gen = "＞ " + totsuzen.substring(j, i) + " ＜\n";
                        i++;
                        j = i;
                        gentotsu = gentotsu.concat(gen);
                    }
                }

                mEditText.setText(prefix + "＿" + ue + "＿\n" + gentotsu + "￣" + shita + "￣" + suffix);
            }

        });

        mImgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
            }
        });

        // 文字数をカウントしてボタンを制御する
        mEditText.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int textColor;
                String str = s.toString();
                int length = 140 - str.codePointCount(0, str.length());
                // 140文字をオーバーした時は文字数を赤色に
                if (length < 0) {
                    textColor = Color.RED;
                } else {
                    textColor = Color.WHITE;
                }
                mTextView.setTextColor(textColor);
                mTextView.setText(String.valueOf(length));

                // 文字数が0文字または140文字以上の時はボタンを無効
                if (str.codePointCount(0, str.length()) == 0
                        || str.codePointCount(0, str.length()) > 140) {
                    mTweetButton.setEnabled(false);
                } else {
                    mTweetButton.setEnabled(true);
                }
            }

            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Uri uri = data.getData();
            uriToFile(uri);
        }
    }

    private void uriToFile(Uri uri) {
        ContentResolver cr = getContentResolver();
        String[] columns = {MediaStore.Images.Media.DATA};
        Cursor c = cr.query(uri, columns, null, null, null);
        c.moveToFirst();
        File path = new File(c.getString(0));
        if (!path.exists()) {
            return;
        }
        this.imgPath = path;
        showToast("画像セットok");
        mImgButton.setTextColor(getResources().getColor(color.holo_blue_bright));
    }

    private class PostTask extends AsyncTask<StatusUpdate, Void, Boolean> {
        @Override
        protected Boolean doInBackground(StatusUpdate... params) {
            StatusUpdate super_sugoi = params[0];
            try {
                mTwitter.updateStatus(super_sugoi);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            dismissProgressDialog();
            if (success) {
                mEditText.setText("");
                finish();
            } else {
                showToast("残念~！もう一回！！");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.post, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.tweet_clear:
                mEditText.setText("");
                break;
            case R.id.tweet_battery:
                Intent batteryIntent = getApplicationContext().registerReceiver(null,
                        new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                int level = batteryIntent.getIntExtra("level", 0);
                int scale = batteryIntent.getIntExtra("scale", 100);
                int status = batteryIntent.getIntExtra("status", 0);
                int battery = level * 100 / scale;
                String model = Build.MODEL;

                switch (status) {
                    case BatteryManager.BATTERY_STATUS_FULL:
                        mEditText.setText(model + " のバッテリー残量:" + battery + "% (0゜・◡・♥​​)");
                        break;
                    case BatteryManager.BATTERY_STATUS_CHARGING:
                        mEditText.setText(model + " のバッテリー残量:" + battery + "% 充電なう(・◡・♥​​)");
                        break;
                    default:
                        if (level <= 10) {
                            mEditText.setText(model + " のバッテリー残量:" + battery + "% (◞‸◟)");
                        } else {
                            mEditText.setText(model + " のバッテリー残量:" + battery + "% (・◡・♥​​)");
                        }
                        break;
                }
                break;
        }
        return true;
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private void showProgressDialog(String message) {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(message);
        mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null)
            mProgressDialog.dismiss();
    }
}
