package com.app.gameform.utils;

import android.os.Build;
import android.text.Html;
import android.text.TextUtils;

public class HtmlUtils {
    /**
     * 移除HTML标签，只保留纯文本
     * @param htmlContent 包含HTML标签的内容
     * @return 纯文本内容
     */
    public static String removeHtmlTags(String htmlContent) {
        if (TextUtils.isEmpty(htmlContent)) {
            return "";
        }
        // 方法1：使用 Html.fromHtml() 去除HTML标签
        String plainText;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            plainText = Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            plainText = Html.fromHtml(htmlContent).toString();
        }
        // 去除多余的空行和空格
        plainText = plainText.replaceAll("\\n\\s*\\n", "\n").trim();

        return plainText;
    }

    /**
     * 备用方法：使用正则表达式移除HTML标签
     * 如果上面的方法不满足需求，可以使用这个方法
     */
    public static String removeHtmlTagsRegex(String htmlContent) {
        if (TextUtils.isEmpty(htmlContent)) {
            return "";
        }
        // 移除所有HTML标签
        String plainText = htmlContent.replaceAll("<[^>]*>", "");
        // 处理HTML实体字符
        plainText = plainText.replace("&nbsp;", " ");
        plainText = plainText.replace("&lt;", "<");
        plainText = plainText.replace("&gt;", ">");
        plainText = plainText.replace("&amp;", "&");
        plainText = plainText.replace("&quot;", "\"");
        plainText = plainText.replace("&#39;", "'");
        // 去除多余的空行和空格
        plainText = plainText.replaceAll("\\n\\s*\\n", "\n").trim();
        return plainText;
    }
}