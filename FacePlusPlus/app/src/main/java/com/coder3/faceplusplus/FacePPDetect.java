package com.coder3.faceplusplus;

import android.graphics.Bitmap;
import android.util.Log;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

/**
 * author:Created by Liu Shuangbo on 2016/9/13.
 * email:18330180757@163.com
 */
public class FacePPDetect {
    public interface CallBack {
        void error(FaceppParseException exception);
        void success(JSONObject result);
    }

    public static void detect(final Bitmap bitmap, final CallBack callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //request
                    HttpRequests requests = new HttpRequests(Constant.KEY, Constant.SECRET, true, true);
                    Bitmap bmSmall = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight());
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bmSmall.compress(Bitmap.CompressFormat.JPEG, 100, stream);

                    byte[] arrays = stream.toByteArray();

                    PostParameters parameters = new PostParameters();
                    parameters.setImg(arrays);
                    JSONObject jsonObject = requests.detectionDetect(parameters);

                    Log.e("TAG", jsonObject.toString());
                    System.out.println("运行成功");
                    if (callback != null) {
                        callback.success(jsonObject);
                    }
                } catch (FaceppParseException e) {
                    e.printStackTrace();
                    if (callback != null) {
                        callback.error(e);
                    }
                }
            }
        }).start();
    }
}
