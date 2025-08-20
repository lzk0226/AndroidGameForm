package com.app.gameform.Activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.gameform.R;
import com.app.gameform.network.ApiConstants;
import com.app.gameform.utils.FileUtils;
import com.app.gameform.utils.SharedPrefManager;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 发帖页面
 */
public class NewPostActivity extends AppCompatActivity {

    private TextView btnClose, btnPublish, tvSelectedTopic, btnInsertImage;
    private EditText etContent, etTitle;   // 新增标题输入框
    private RecyclerView rvSelectedImages;

    private List<Uri> selectedImageUris = new ArrayList<>();
    private ImageAdapter imageAdapter;

    // 图片选择器
    private ActivityResultLauncher<String> pickImageLauncher;

    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newpost);

        initViews();
        initImagePicker();
        initEvents();
    }

    private void initViews() {
        btnClose = findViewById(R.id.btn_close);
        btnPublish = findViewById(R.id.btn_publish);
        tvSelectedTopic = findViewById(R.id.tv_selected_topic);
        btnInsertImage = findViewById(R.id.btn_insert_image);
        etContent = findViewById(R.id.et_content);
        etTitle = findViewById(R.id.et_title);   // 初始化标题输入框
        rvSelectedImages = findViewById(R.id.rv_selected_images);

        rvSelectedImages.setLayoutManager(new GridLayoutManager(this, 3));
        imageAdapter = new ImageAdapter(selectedImageUris);
        rvSelectedImages.setAdapter(imageAdapter);
    }

    private void initImagePicker() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUris.add(uri);
                        rvSelectedImages.setVisibility(View.VISIBLE);
                        imageAdapter.notifyDataSetChanged();
                    }
                }
        );
    }

    private void initEvents() {
        btnClose.setOnClickListener(v -> finish());

        btnInsertImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // 输入监听，控制发布按钮是否可点击
        etContent.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnPublish.setEnabled(s.length() > 0 || etTitle.getText().length() > 0);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etTitle.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnPublish.setEnabled(s.length() > 0 || etContent.getText().length() > 0);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnPublish.setOnClickListener(v -> publishPost());
    }

    /**
     * 发布帖子
     */
    private void publishPost() {
        String title = etTitle.getText().toString().trim();   // 读取标题
        String content = etContent.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "请输入标题", Toast.LENGTH_SHORT).show();
            return;
        }
        if (content.isEmpty()) {
            Toast.makeText(this, "请输入内容", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = SharedPrefManager.getInstance(this).getToken();
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 构造请求体
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        builder.addFormDataPart("postTitle", title);    // 使用用户输入的标题
        builder.addFormDataPart("postContent", content);
        builder.addFormDataPart("sectionId", "1"); // 默认版块，可扩展为选择

        // 添加图片
        for (int i = 0; i < selectedImageUris.size(); i++) {
            Uri uri = selectedImageUris.get(i);
            String path = FileUtils.getPath(this, uri);
            if (path != null) {
                File file = new File(path);
                if (file.exists()) {
                    builder.addFormDataPart("images", file.getName(),
                            RequestBody.create(file, MediaType.parse("image/*")));
                }
            }
        }

        RequestBody requestBody = builder.build();

        Request request = new Request.Builder()
                .url(ApiConstants.USER_POST) // 后端发帖接口
                .addHeader("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(NewPostActivity.this, "发布失败：" + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String respStr = response.body().string();
                Log.d("发帖响应", respStr);

                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        try {
                            JSONObject json = new JSONObject(respStr);
                            if (json.optInt("code") == 200) {
                                Toast.makeText(NewPostActivity.this, "发布成功", Toast.LENGTH_SHORT).show();
                                setResult(Activity.RESULT_OK);
                                finish();
                            } else {
                                Toast.makeText(NewPostActivity.this, json.optString("msg"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(NewPostActivity.this, "解析错误", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(NewPostActivity.this, "请求失败: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
