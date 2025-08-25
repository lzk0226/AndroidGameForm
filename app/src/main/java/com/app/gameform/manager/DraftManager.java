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
 * 修复版本：正确保存和加载游戏类型、游戏、版块信息
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
     */
    public Draft saveDraft(String title, String content,
                           Integer typeId, Integer gameId,
                           Integer sectionId, String sectionName) {
        try {
            List<Draft> draftList = getAllDrafts();

            // 生成草稿ID
            int draftId = getNextDraftId();

            // 创建草稿对象
            Draft draft = new Draft();
            draft.setDraftId(draftId);
            draft.setDraftTitle(title);
            draft.setDraftContent(content);
            draft.setTypeId(typeId);
            draft.setGameId(gameId);
            draft.setSectionId(sectionId);
            draft.setSectionName(sectionName);
            draft.setCreateTime(new Date());
            draft.setUpdateTime(new Date());

            // 添加到列表开头
            draftList.add(0, draft);

            // 保存到本地
            saveDraftList(draftList);

            Log.d(TAG, String.format("草稿保存成功: %s, typeId=%s, gameId=%s, sectionId=%s, sectionName=%s",
                    draft.getDisplayTitle(), typeId, gameId, sectionId, sectionName));
            return draft;

        } catch (Exception e) {
            Log.e(TAG, "保存草稿失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 更新现有草稿
     */
    public boolean updateDraft(int draftId, String title, String content,
                               Integer typeId, Integer gameId,
                               Integer sectionId, String sectionName) {
        try {
            List<Draft> draftList = getAllDrafts();

            for (int i = 0; i < draftList.size(); i++) {
                Draft draft = draftList.get(i);
                if (draft.getDraftId() != null && draft.getDraftId() == draftId) {
                    // 更新草稿内容
                    draft.setDraftTitle(title);
                    draft.setDraftContent(content);
                    draft.setTypeId(typeId);
                    draft.setGameId(gameId);
                    draft.setSectionId(sectionId);
                    draft.setSectionName(sectionName);
                    draft.setUpdateTime(new Date());

                    // 移到列表开头（最近编辑的在前面）
                    draftList.remove(i);
                    draftList.add(0, draft);

                    saveDraftList(draftList);
                    Log.d(TAG, String.format("草稿更新成功: %s, typeId=%s, gameId=%s, sectionId=%s, sectionName=%s",
                            draft.getDisplayTitle(), typeId, gameId, sectionId, sectionName));
                    return true;
                }
            }

            Log.w(TAG, "未找到要更新的草稿: " + draftId);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "更新草稿失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 删除草稿
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

            Log.w(TAG, "未找到要删除的草稿: " + draftId);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "删除草稿失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取所有草稿
     */
    public List<Draft> getAllDrafts() {
        try {
            String draftsJson = preferences.getString(KEY_DRAFTS, null);
            if (draftsJson != null && !draftsJson.isEmpty()) {
                Type listType = new TypeToken<List<Draft>>(){}.getType();
                List<Draft> drafts = gson.fromJson(draftsJson, listType);

                if (drafts != null) {
                    // 验证草稿数据完整性
                    for (Draft draft : drafts) {
                        if (draft != null) {
                            Log.d(TAG, String.format("加载草稿: id=%d, title=%s, typeId=%s, gameId=%s, sectionId=%s, sectionName=%s",
                                    draft.getDraftId(), draft.getDraftTitle(), draft.getTypeId(),
                                    draft.getGameId(), draft.getSectionId(), draft.getSectionName()));
                        }
                    }
                    return drafts;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "获取草稿列表失败: " + e.getMessage());
            // 如果解析失败，清空数据避免后续错误
            preferences.edit().remove(KEY_DRAFTS).apply();
        }
        return new ArrayList<>();
    }

    /**
     * 根据ID获取草稿
     */
    public Draft getDraftById(int draftId) {
        List<Draft> draftList = getAllDrafts();
        for (Draft draft : draftList) {
            if (draft.getDraftId() != null && draft.getDraftId() == draftId) {
                Log.d(TAG, String.format("找到草稿: id=%d, typeId=%s, gameId=%s, sectionId=%s",
                        draftId, draft.getTypeId(), draft.getGameId(), draft.getSectionId()));
                return draft;
            }
        }
        Log.w(TAG, "未找到草稿: " + draftId);
        return null;
    }

    /**
     * 检查是否有内容需要保存为草稿
     */
    public boolean hasContentToSave(String title, String content) {
        return (title != null && !title.trim().isEmpty()) ||
                (content != null && !content.trim().isEmpty());
    }

    /**
     * 生成草稿默认名称
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
        preferences.edit()
                .remove(KEY_DRAFTS)
                .remove(KEY_DRAFT_ID_COUNTER)
                .apply();
        Log.d(TAG, "所有草稿已清空");
    }

    /**
     * 获取草稿数量
     */
    public int getDraftCount() {
        return getAllDrafts().size();
    }

    /**
     * 检查是否存在指定ID的草稿
     */
    public boolean isDraftExists(int draftId) {
        return getDraftById(draftId) != null;
    }

    // 私有方法

    /**
     * 保存草稿列表到本地
     */
    private void saveDraftList(List<Draft> draftList) {
        try {
            String draftsJson = gson.toJson(draftList);
            preferences.edit().putString(KEY_DRAFTS, draftsJson).apply();
            Log.d(TAG, "草稿列表保存成功，共 " + draftList.size() + " 条记录");
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
        Log.d(TAG, "生成新草稿ID: " + nextId);
        return nextId;
    }
}