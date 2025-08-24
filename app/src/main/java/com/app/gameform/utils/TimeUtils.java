package com.app.gameform.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 时间格式化工具类
 */
public class TimeUtils {

    /**
     * 格式化时间显示
     * @param date 时间对象
     * @return 格式化后的时间字符串
     */
    public static String formatTimeAgo(Date date) {
        if (date == null) {
            return "未知时间";
        }

        long currentTime = System.currentTimeMillis();
        long postTime = date.getTime();
        long timeDiff = currentTime - postTime;

        // 转换为秒、分钟、小时、天
        long seconds = timeDiff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return "刚刚";
        } else if (minutes < 60) {
            return minutes + "分钟前";
        } else if (hours < 24) {
            return hours + "小时前";
        } else if (days < 7) {
            return days + "天前";
        } else {
            // 超过7天显示具体日期
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
            return sdf.format(date);
        }
    }

    /**
     * 格式化完整时间显示
     * @param date 时间对象
     * @return 格式化后的完整时间字符串
     */
    public static String formatFullTime(Date date) {
        if (date == null) {
            return "未知时间";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(date);
    }
}