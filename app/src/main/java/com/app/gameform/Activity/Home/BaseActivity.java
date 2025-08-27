package com.app.gameform.Activity.Home;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.app.gameform.utils.DoubleBackExitHelper;

/**
 * 基础Activity类
 * 提供双击返回键退出应用的功能
 */
public abstract class BaseActivity extends AppCompatActivity {

    private DoubleBackExitHelper doubleBackExitHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化双击返回键退出工具
        doubleBackExitHelper = new DoubleBackExitHelper(this, "再按一次退出应用");
    }

    @Override
    public void onBackPressed() {
        // 使用双击退出工具处理返回键
        boolean handled = doubleBackExitHelper.onBackPressed();

        if (!handled) {
            // 如果工具类没有处理，执行默认行为
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理资源
        if (doubleBackExitHelper != null) {
            doubleBackExitHelper.destroy();
        }
    }

    /**
     * 设置自定义的退出提示信息
     * @param message 提示信息
     */
    protected void setExitMessage(String message) {
        if (doubleBackExitHelper != null) {
            doubleBackExitHelper.setExitMessage(message);
        }
    }

    /**
     * 重置双击状态
     */
    protected void resetDoubleBackState() {
        if (doubleBackExitHelper != null) {
            doubleBackExitHelper.reset();
        }
    }
}