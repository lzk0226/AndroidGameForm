package com.app.gameform.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import com.app.gameform.network.ApiConstants;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 图片上传工具类
 */
public class ImageUploadHelper {

    private static final String TAG = "ImageUploadHelper";
    private static final int MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB

    private Context context;
    private OkHttpClient client;

    public ImageUploadHelper(Context context) {
        this.context = context;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 上传图片
     * @param imageUri 图片URI
     * @param token 认证token
     * @param callback 回调接口
     */
    public void uploadImage(Uri imageUri, String token, ImageUploadCallback callback) {
        try {
            // 获取文件扩展名
            String fileExtension = getFileExtension(imageUri);
            if (fileExtension == null) {
                callback.onFailure("不支持的图片格式");
                return;
            }

            // 将 URI 转换为 Base64
            String base64Image = convertUriToBase64(imageUri);
            if (base64Image == null) {
                callback.onFailure("图片处理失败");
                return;
            }

            // 上传图片到服务器
            uploadImageToServer(base64Image, fileExtension, token, callback);

        } catch (Exception e) {
            Log.e(TAG, "图片上传错误: " + e.getMessage());
            callback.onFailure("图片处理失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(Uri imageUri) {
        try {
            // 方法1：通过MIME类型获取扩展名
            String mimeType = context.getContentResolver().getType(imageUri);
            if (mimeType != null) {
                switch (mimeType.toLowerCase()) {
                    case "image/jpeg":
                    case "image/jpg":
                        return "jpg";
                    case "image/png":
                        return "png";
                    case "image/gif":
                        return "gif";
                    case "image/webp":
                        return "webp";
                    case "image/bmp":
                        return "bmp";
                    default:
                        Log.w(TAG, "未知MIME类型: " + mimeType);
                        break;
                }
            }

            // 方法2：从文件路径获取扩展名
            String path = FileUtils.getPath(context, imageUri);
            if (path != null && path.contains(".")) {
                String ext = path.substring(path.lastIndexOf(".") + 1).toLowerCase();
                if (ext.matches("^(jpg|jpeg|png|gif|webp|bmp)$")) {
                    return ext.equals("jpeg") ? "jpg" : ext;
                }
            }

            // 方法3：默认使用jpg
            Log.w(TAG, "无法确定扩展名，使用默认jpg");
            return "jpg";

        } catch (Exception e) {
            Log.e(TAG, "获取扩展名错误: " + e.getMessage());
            return "jpg"; // 默认返回jpg
        }
    }

    /**
     * 将 URI 转换为 Base64 字符串
     */
    private String convertUriToBase64(Uri imageUri) {
        try {
            // 从 URI 获取输入流
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Log.e(TAG, "无法打开图片文件");
                return null;
            }

            // 读取图片数据
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();

            // 检查文件大小（限制为5MB）
            if (imageBytes.length > MAX_IMAGE_SIZE) {
                return null;
            }

            // 转换为 Base64
            String base64String = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

            // 添加数据URL前缀（根据图片类型）
            String mimeType = context.getContentResolver().getType(imageUri);
            if (mimeType == null) mimeType = "image/jpeg";

            return "data:" + mimeType + ";base64," + base64String;

        } catch (IOException e) {
            Log.e(TAG, "图片转换失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 生成随机字符串
     */
    private String generateRandomString() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 9; i++) {
            result.append(chars.charAt(random.nextInt(chars.length())));
        }

        return result.toString();
    }

    /**
     * 上传图片到服务器
     */
    private void uploadImageToServer(String base64Image, String fileExtension, String token, ImageUploadCallback callback) {
        try {
            // 生成文件名（与网页版保持一致的格式）
            String fileName = String.format("post_%d_%s.%s",
                    System.currentTimeMillis(),
                    generateRandomString(),
                    fileExtension);

            Log.d(TAG, "生成的文件名: " + fileName);

            // 构建请求体
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("fileName", fileName);
            jsonBody.put("base64Data", base64Image);

            RequestBody requestBody = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            // 构建请求
            Request request = new Request.Builder()
                    .url(ApiConstants.UPLOAD_POST_IMAGE)
                    .addHeader("Authorization", token)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build();

            Log.d(TAG, "开始上传，URL: " + ApiConstants.UPLOAD_POST_IMAGE);
            Log.d(TAG, "Token: " + token.substring(0, Math.min(token.length(), 50)) + "...");

            // 执行请求
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "请求失败: " + e.getMessage());
                    callback.onFailure(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String respStr = response.body().string();
                    Log.d(TAG, "状态码: " + response.code() + ", 响应: " + respStr);

                    try {
                        JSONObject json = new JSONObject(respStr);
                        if (json.optInt("code") == 200) {
                            // 上传成功，返回图片路径
                            String imageUrl = "images/user/post/" + fileName;
                            Log.d(TAG, "上传成功，图片路径: " + imageUrl);
                            callback.onSuccess(imageUrl);
                        } else {
                            String errorMsg = json.optString("message", json.optString("msg", "上传失败"));
                            Log.e(TAG, "服务器返回错误: " + errorMsg);
                            callback.onFailure(errorMsg);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析响应错误: " + e.getMessage());
                        callback.onFailure("响应解析失败");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "构建请求错误: " + e.getMessage());
            callback.onFailure("请求构建失败: " + e.getMessage());
        }
    }

    /**
     * 图片上传回调接口
     */
    public interface ImageUploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(String error);
    }

    /**
     * 销毁资源
     */
    public void destroy() {
        if (client != null) {
            client.dispatcher().executorService().shutdown();
        }
    }
}