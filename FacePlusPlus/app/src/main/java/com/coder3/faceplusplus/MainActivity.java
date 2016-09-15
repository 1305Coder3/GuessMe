package com.coder3.faceplusplus;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.facepp.error.FaceppParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PICK_CODE = 0x110;
    private ImageView faceImage;
    private Button btnImage;
    private Button btnAct;
    private TextView textShow;
    private View view;
    private String mPath;
    private Bitmap mPhtotImg;
    private Paint mPaint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        intiEvents();
        mPaint = new Paint();

    }

    private void intiEvents() {
        btnAct.setOnClickListener(this);
        btnImage.setOnClickListener(this);
    }

    private void initViews() {
        faceImage = (ImageView) findViewById(R.id.photo);
        btnImage = (Button) findViewById(R.id.btnGetImage);
        btnAct = (Button) findViewById(R.id.btnDetect);
        textShow = (TextView) findViewById(R.id.text1);
        view = findViewById(R.id.frame1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_CODE) {
            if (data != null) {
                Uri uri = data.getData();
                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                cursor.moveToFirst();

                int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                mPath = cursor.getString(idx);
                cursor.close();

                resizePhoto();
                faceImage.setImageBitmap(mPhtotImg);
                textShow.setText("Click Detect ==>");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    //压缩图片
    private void resizePhoto() {
        BitmapFactory.Options option = new  BitmapFactory.Options();
        option.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(mPath,option);
        double ratio = Math.max(option.outWidth *1.0d / 1024f, option.outHeight *1.0d / 1024f);
        option.inSampleSize = (int) Math.ceil(ratio);
        option.inJustDecodeBounds = false;
        mPhtotImg = BitmapFactory.decodeFile(mPath, option);
    }

    private static final int MSG_SUCCESS = 0x111;
    private static final int MSG_ERROR = 0x112;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ERROR:
                    view.setVisibility(View.VISIBLE);
                    String errorMsg = (String) msg.obj;
                    if (TextUtils.isEmpty(errorMsg)) {
                        textShow.setText("Error!");
                    } else {
                        textShow.setText(errorMsg);
                    }
                    break;
                case MSG_SUCCESS:
                    view.setVisibility(View.VISIBLE);
                    JSONObject rs = (JSONObject) msg.obj;

                    prepareRsBitmap(rs);

                    faceImage.setImageBitmap(mPhtotImg);
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private void prepareRsBitmap(JSONObject rs) {
        Bitmap bitmap = Bitmap.createBitmap(mPhtotImg.getWidth(), mPhtotImg.getHeight(), mPhtotImg.getConfig());
        Canvas canva = new Canvas(bitmap);
        canva.drawBitmap(mPhtotImg, 0, 0, null);

        try {
            JSONArray faces = rs.getJSONArray("face");

            int faceCount = faces.length();
            textShow.setText("find" + faceCount);

            for (int i = 0; i < faceCount ; i++) {
                //拿到单独的face对象
                JSONObject face = faces.getJSONObject(i);
                JSONObject posObj = face.getJSONObject("position");

                float x = (float) posObj.getJSONObject("center").getDouble("x");
                float y = (float) posObj.getJSONObject("center").getDouble("y");

                float w = (float) posObj.getDouble("width");
                float h = (float) posObj.getDouble("height");

                x = x/100 * bitmap.getWidth();
                y = y/100 * bitmap.getHeight();

                w = w/100 * bitmap.getWidth();
                h = h/100 * bitmap.getHeight();

                //绘制box
                mPaint.setColor(0xffffffff);
                mPaint.setStrokeWidth(3);

                canva.drawLine(x - w/2, y - h/2, x - w/2, y + h/2, mPaint);
                canva.drawLine(x - w/2, y - h/2, x + w/2, y - h/2, mPaint);
                canva.drawLine(x + w/2, y - h/2, x + w/2, y + h/2, mPaint);
                canva.drawLine(x - w/2, y + h/2, x + w/2, y + h/2, mPaint);

                //get age and gender
                int age = face.getJSONObject("attribute").getJSONObject("age").getInt("value");
                String gender = face.getJSONObject("attribute").getJSONObject("gender").getString("value");

                Bitmap ageBitmap = buildAgeBitmap(age, "Male".equals(gender));
                int ageWidth = ageBitmap.getWidth();
                int ageHeight = ageBitmap.getHeight();

                if (bitmap.getWidth() < faceImage.getWidth() && bitmap.getHeight() < faceImage.getHeight()){
                    float ratio = Math.max(bitmap.getWidth() * 1.0f / faceImage.getWidth(),
                            bitmap.getHeight() * 1.0f/faceImage.getHeight());
                    ageBitmap = Bitmap.createScaledBitmap(ageBitmap, (int) (ageWidth * ratio),
                            (int) (ageHeight * ratio),false);
                }
                canva.drawBitmap(ageBitmap, x - ageBitmap.getWidth() / 2, y - h/2 - ageBitmap.getHeight(), null);
                mPhtotImg = bitmap;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    //年龄显示框
    private Bitmap buildAgeBitmap(int age, boolean isMale) {
        TextView tv = (TextView) view.findViewById(R.id.ageShow);
        tv.setText(age + "");
        if (isMale) {
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.male), null, null, null);
        }else {
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.female), null, null, null);
        }

        tv.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(tv.getDrawingCache());
        tv.destroyDrawingCache();

        return bitmap;
    }
    //点击事件，按钮监听器
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnDetect:
                view.setVisibility(View.VISIBLE);
                if (mPath != null && !mPath.trim().equals("")) {
                    resizePhoto();
                } else {
                    mPhtotImg = BitmapFactory.decodeResource(getResources(), R.drawable.bighero);
                }
                FacePPDetect.detect(mPhtotImg, new FacePPDetect.CallBack() {

                    @Override
                    public void error(FaceppParseException exception) {
                        Message msg = Message.obtain();
                        msg.what = MSG_ERROR;
                        msg.obj = exception.getErrorMessage();
                        mHandler.sendMessage(msg);
                    }

                    @Override
                    public void success(JSONObject result) {
                        Message msg = Message.obtain();
                        msg.what = MSG_SUCCESS;
                        msg.obj = result;
                        mHandler.sendMessage(msg);
                    }
                });
                break;
            case R.id.btnGetImage:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_CODE);
                break;
        }
    }
}
