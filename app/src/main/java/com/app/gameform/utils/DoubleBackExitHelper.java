package com.app.gameform.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 * 双击返回键退出应用工具类
 * 提供双击返回键退出应用的功能
 */
public class DoubleBackExitHelper {

    private static final int DOUBLE_BACK_TIME_DELTA = 2000; // 双击间隔时间（毫秒）

    private Context context;
    private long lastBackPressTime = 0;
    private String exitMessage;
    private Handler handler = new Handler(Looper.getMainLooper());

    /**
     * 构造函数
     * @param context 上下文
     */
    public DoubleBackExitHelper(Context context) {
        this.context = context;
        this.exitMessage = "再按一次退出应用"; // 默认提示信息
    }

    /**
     * 构造函数
     * @param context 上下文
     * @param exitMessage 自定义退出提示信息
     */
    public DoubleBackExitHelper(Context context, String exitMessage) {
        this.context = context;
        this.exitMessage = exitMessage;
    }

    /**
     * 处理返回键按下事件
     * @return true 表示已处理，false 表示未处理（执行默认行为）
     */
    public boolean onBackPressed() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastBackPressTime < DOUBLE_BACK_TIME_DELTA) {
            // 双击退出 - 销毁进程
            exitApplication();
            return true;
        } else {
            // 第一次点击 - 显示提示信息
            lastBackPressTime = currentTime;
            showExitMessage();
            return true; // 拦截返回事件，不执行默认的返回行为
        }
    }

    /**
     * 显示退出提示信息
     */
    private void showExitMessage() {
        Toast.makeText(context, exitMessage, Toast.LENGTH_SHORT).show();
    }

    /**
     * 退出应用
     */
    private void exitApplication() {
        try {
            // 如果context是Activity，先finish
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                activity.finishAffinity(); // 清除任务栈中的所有Activity
            }

            // 延迟一点时间后彻底退出进程
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // 彻底杀死进程
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(0);
                }
            }, 100); // 延迟100ms确保UI操作完成

        } catch (Exception e) {
            e.printStackTrace();
            // 如果出现异常，直接退出进程
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }
    }

    /**
     * 设置退出提示信息
     * @param message 提示信息
     */
    public void setExitMessage(String message) {
        this.exitMessage = message;
    }

    /**
     * 重置双击状态（可选，在某些场景下可能需要）
     */
    public void reset() {
        lastBackPressTime = 0;
    }

    /**
     * 清理资源
     */
    public void destroy() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        context = null;
    }
}