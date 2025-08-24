package com.app.gameform.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.app.gameform.domain.Draft;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 草稿管理器 - 用于本地存储和管理草稿
 */
public class DraftManager {
    private static final String TAG = "DraftManager";
    private static final String PREF_NAME = "draft_storage";
    private static final String KEY_DRAFTS = "drafts";
    private static final String KEY_DRAFT_ID_COUNTER = "draft_id_counter";

    private static DraftManager instance;
    private SharedPreferences preferences;
    private Gson gson;

    private DraftManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized DraftManager getInstance(Context context) {
        if (instance == null) {
            instance = new DraftManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * 保存草稿
     * @param title 标题
     * @param content 内容
     * @param sectionId 版块ID
     * @param sectionName 版块名称
     * @param imageUris 图片URI列表（转为字符串存储）
     * @return 保存的草稿对象
     */
    public Draft saveDraft(String title, String content, Integer sectionId, String sectionName, String imageUris) {
        try {
            List<Draft> draftList = getAllDrafts();

            // 生成草稿ID
            int draftId = getNextDraftId();

            // 创建草稿对象
            Draft draft = new Draft();
            draft.setDraftId(draftId);
            draft.setDraftTitle(title);
            draft.setDraftContent(content);
            draft.setSectionId(sectionId);
            draft.setSectionName(sectionName);
            draft.setDraftImages(imageUris);
            draft.setCreateTime(new Date());
            draft.setUpdateTime(new Date());

            // 添加到列表开头
            draftList.add(0, draft);

            // 保存到本地
            saveDraftList(draftList);

            Log.d(TAG, "草稿保存成功: " + draft.getDisplayTitle());
            return draft;

        } catch (Exception e) {
            Log.e(TAG, "保存草稿失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 更新现有草稿
     * @param draftId 草稿ID
     * @param title 标题
     * @param content 内容
     * @param sectionId 版块ID
     * @param sectionName 版块名称
     * @param imageUris 图片URI列表
     * @return 是否更新成功
     */
    public boolean updateDraft(int draftId, String title, String content, Integer sectionId, String sectionName, String imageUris) {
        try {
            List<Draft> draftList = getAllDrafts();

            for (int i = 0; i < draftList.size(); i++) {
                Draft draft = draftList.get(i);
                if (draft.getDraftId() != null && draft.getDraftId() == draftId) {
                    draft.setDraftTitle(title);
                    draft.setDraftContent(content);
                    draft.setSectionId(sectionId);
                    draft.setSectionName(sectionName);
                    draft.setDraftImages(imageUris);
                    draft.setUpdateTime(new Date());

                    // 移到列表开头
                    draftList.remove(i);
                    draftList.add(0, draft);

                    saveDraftList(draftList);
                    Log.d(TAG, "草稿更新成功: " + draft.getDisplayTitle());
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            Log.e(TAG, "更新草稿失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 删除草稿
     * @param draftId 草稿ID
     * @return 是否删除成功
     */
    public boolean deleteDraft(int draftId) {
        try {
            List<Draft> draftList = getAllDrafts();

            for (int i = 0; i < draftList.size(); i++) {
                Draft draft = draftList.get(i);
                if (draft.getDraftId() != null && draft.getDraftId() == draftId) {
                    draftList.remove(i);
                    saveDraftList(draftList);
                    Log.d(TAG, "草稿删除成功: " + draft.getDisplayTitle());
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            Log.e(TAG, "删除草稿失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取所有草稿
     * @return 草稿列表
     */
    public List<Draft> getAllDrafts() {
        try {
            String draftsJson = preferences.getString(KEY_DRAFTS, null);
            if (draftsJson != null && !draftsJson.isEmpty()) {
                Type listType = new TypeToken<List<Draft>>(){}.getType();
                List<Draft> drafts = gson.fromJson(draftsJson, listType);
                return drafts != null ? drafts : new ArrayList<>();
            }
        } catch (Exception e) {
            Log.e(TAG, "获取草稿列表失败: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * 根据ID获取草稿
     * @param draftId 草稿ID
     * @return 草稿对象，未找到返回null
     */
    public Draft getDraftById(int draftId) {
        List<Draft> draftList = getAllDrafts();
        for (Draft draft : draftList) {
            if (draft.getDraftId() != null && draft.getDraftId() == draftId) {
                return draft;
            }
        }
        return null;
    }

    /**
     * 检查是否有内容需要保存为草稿
     * @param title 标题
     * @param content 内容
     * @return 是否有内容
     */
    public boolean hasContentToSave(String title, String content) {
        return (title != null && !title.trim().isEmpty()) ||
                (content != null && !content.trim().isEmpty());
    }

    /**
     * 生成草稿默认名称
     * @return 默认名称
     */
    public String generateDefaultDraftTitle() {
        List<Draft> draftList = getAllDrafts();
        int count = 1;

        // 查找未命名草稿的最大编号
        for (Draft draft : draftList) {
            String title = draft.getDraftTitle();
            if (title != null && title.startsWith("未命名草稿")) {
                try {
                    String numberStr = title.replace("未命名草稿", "");
                    if (!numberStr.isEmpty()) {
                        int number = Integer.parseInt(numberStr);
                        if (number >= count) {
                            count = number + 1;
                        }
                    }
                } catch (NumberFormatException ignored) {
                    // 忽略解析错误
                }
            }
        }

        return "未命名草稿" + count;
    }

    /**
     * 清空所有草稿
     */
    public void clearAllDrafts() {
        preferences.edit().remove(KEY_DRAFTS).apply();
        Log.d(TAG, "所有草稿已清空");
    }

    // 私有方法

    /**
     * 保存草稿列表到本地
     */
    private void saveDraftList(List<Draft> draftList) {
        try {
            String draftsJson = gson.toJson(draftList);
            preferences.edit().putString(KEY_DRAFTS, draftsJson).apply();
        } catch (Exception e) {
            Log.e(TAG, "保存草稿列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取下一个草稿ID
     */
    private int getNextDraftId() {
        int currentId = preferences.getInt(KEY_DRAFT_ID_COUNTER, 0);
        int nextId = currentId + 1;
        preferences.edit().putInt(KEY_DRAFT_ID_COUNTER, nextId).apply();
        return nextId;
    }
}