package com.app.gameform.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;

import com.app.gameform.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.util.Arrays;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 图片加载工具类
 * 专门处理来自服务器的图片路径问题
 */
public class ImageUtils {

    private static final String TAG = "ImageUtils";
    // 修复基础URL，添加完整的图片路径
    private static final String BASE_URL = "http://10.0.2.2:8080/user/public/";

    // 支持的图片格式
    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"
    );

    /**
     * 规范化文件路径
     * 解决Windows路径分隔符问题
     */
    private static String normalizeFilePath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }

        // 将反斜杠替换为正斜杠
        String normalized = filePath.replace('\\', '/');

        // 确保路径不以斜杠开头（因为BASE_URL已经包含了完整路径）
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        return normalized;
    }

    /**
     * 构建完整的图片URL
     */
    private static String buildImageUrl(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return null;
        }

        // 如果已经是完整URL，直接返回
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return relativePath;
        }

        // 规范化路径
        String normalizedPath = normalizeFilePath(relativePath);
        if (normalizedPath == null) {
            return null;
        }

        // 拼接基础URL
        String fullUrl = BASE_URL + normalizedPath;

        Log.d(TAG, "Built image URL: " + fullUrl);
        return fullUrl;
    }

    /**
     * 检查是否为图片文件
     * 改进的文件扩展名检查，处理查询参数和片段
     */
    private static boolean isImageFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }

        // 移除查询参数和片段
        String cleanPath = filePath.split("\\?")[0].split("#")[0];
        String lowerPath = cleanPath.toLowerCase();

        return IMAGE_EXTENSIONS.stream().anyMatch(lowerPath::endsWith);
    }

    /**
     * 创建通用的RequestOptions
     */
    private static RequestOptions createRequestOptions(int placeholder, int error) {
        return new RequestOptions()
                .placeholder(placeholder)
                .error(error)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .skipMemoryCache(false)
                .timeout(10000); // 10秒超时
    }

    /**
     * 通用的图片加载方法
     */
    private static void loadImageWithGlide(Context context, ImageView imageView, String imageUrl,
                                           RequestOptions options, RequestListener<Drawable> listener) {
        try {
            Glide.with(context)
                    .load(imageUrl)
                    .apply(options)
                    .listener(listener)
                    .into(imageView);
        } catch (Exception e) {
            Log.e(TAG, "Error loading image: " + imageUrl, e);
            // 设置默认图片
            imageView.setImageResource(options.getErrorId() != 0 ? options.getErrorId() : R.color.light_gray);
        }
    }

    /**
     * 加载用户头像
     */
    public static void loadUserAvatar(Context context, CircleImageView imageView, String avatarUrl) {
        String fullUrl = buildImageUrl(avatarUrl);

        Log.d(TAG, "Loading avatar: " + fullUrl);

        if (fullUrl != null && !fullUrl.isEmpty() && isImageFile(fullUrl)) {
            RequestOptions options = createRequestOptions(R.mipmap.ic_launcher_round, R.mipmap.ic_launcher_round)
                    .circleCrop();

            RequestListener<Drawable> listener = new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                    Log.e(TAG, "Failed to load avatar: " + fullUrl, e);
                    imageView.setImageResource(R.mipmap.ic_launcher_round);
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    Log.d(TAG, "Avatar loaded successfully: " + fullUrl);
                    return false;
                }
            };

            loadImageWithGlide(context, imageView, fullUrl, options, listener);
        } else {
            Log.w(TAG, "Invalid avatar URL: " + avatarUrl);
            imageView.setImageResource(R.mipmap.ic_launcher_round);
        }
    }

    /**
     * 加载帖子图片
     */
    public static void loadPostImage(Context context, ImageView imageView, String photoUrl) {
        String fullUrl = buildImageUrl(photoUrl);

        Log.d(TAG, "Loading post image: " + fullUrl);

        if (fullUrl != null && !fullUrl.isEmpty() && isImageFile(fullUrl)) {
            RequestOptions options = createRequestOptions(R.color.light_gray, R.color.light_gray)
                    .centerCrop();

            RequestListener<Drawable> listener = new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                    Log.e(TAG, "Failed to load post image: " + fullUrl, e);
                    imageView.setImageResource(R.color.light_gray);
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    Log.d(TAG, "Post image loaded successfully: " + fullUrl);
                    return false;
                }
            };

            loadImageWithGlide(context, imageView, fullUrl, options, listener);
        } else {
            Log.w(TAG, "Invalid post image URL: " + photoUrl);
            imageView.setImageResource(R.color.light_gray);
        }
    }

    /**
     * 加载普通图片
     */
    public static void loadImage(Context context, ImageView imageView, String imageUrl, int placeholderRes, int errorRes) {
        String fullUrl = buildImageUrl(imageUrl);

        Log.d(TAG, "Loading image: " + fullUrl);

        if (fullUrl != null && !fullUrl.isEmpty() && isImageFile(fullUrl)) {
            RequestOptions options = createRequestOptions(placeholderRes, errorRes)
                    .fitCenter();

            RequestListener<Drawable> listener = new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                    Log.e(TAG, "Failed to load image: " + fullUrl, e);
                    imageView.setImageResource(errorRes);
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    Log.d(TAG, "Image loaded successfully: " + fullUrl);
                    return false;
                }
            };

            loadImageWithGlide(context, imageView, fullUrl, options, listener);
        } else {
            Log.w(TAG, "Invalid image URL: " + imageUrl);
            imageView.setImageResource(errorRes);
        }
    }

    /**
     * 清除Glide缓存（用于调试）
     */
    public static void clearCache(Context context) {
        try {
            Glide.get(context).clearMemory();
            new Thread(() -> {
                try {
                    Glide.get(context).clearDiskCache();
                } catch (Exception e) {
                    Log.e(TAG, "Error clearing disk cache", e);
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Error clearing cache", e);
        }
    }
}