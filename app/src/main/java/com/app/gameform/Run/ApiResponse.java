package com.app.gameform.Run;

import android.os.Parcel;
import android.os.Parcelable;

import com.app.gameform.emuns.ResultCodeEnum;
import com.google.gson.annotations.SerializedName;

/**
 * Android端API响应包装类
 * 支持序列化和反序列化，可在Activity/Fragment间传递
 *
 * @param <T> 响应数据类型
 * @author SockLightDust
 * @version 1.0
 */
public class ApiResponse<T> implements Parcelable {

    /**
     * 响应码
     */
    @SerializedName("code")
    private Integer code;

    /**
     * 响应消息
     */
    @SerializedName("message")
    private String message;

    /**
     * 响应数据
     */
    @SerializedName("data")
    private T data;

    /**
     * 时间戳
     */
    @SerializedName("timestamp")
    private Long timestamp;

    // 构造方法
    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public ApiResponse(Integer code, String message) {
        this();
        this.code = code;
        this.message = message;
    }

    public ApiResponse(Integer code, String message, T data) {
        this(code, message);
        this.data = data;
    }

    public ApiResponse(ResultCodeEnum resultCodeEnum) {
        this();
        this.code = resultCodeEnum.getCode();
        this.message = resultCodeEnum.getMessage();
    }

    public ApiResponse(ResultCodeEnum resultCodeEnum, T data) {
        this(resultCodeEnum);
        this.data = data;
    }

    // Parcelable构造方法
    protected ApiResponse(Parcel in) {
        code = in.readByte() == 0 ? null : in.readInt();
        message = in.readString();
        timestamp = in.readByte() == 0 ? null : in.readLong();
        // 注意：data字段需要根据具体类型处理，这里简化处理
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ResultCodeEnum.SUCCESS);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResultCodeEnum.SUCCESS, data);
    }

    /**
     * 成功响应（自定义消息）
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(ResultCodeEnum.SUCCESS.getCode(), message);
    }

    /**
     * 成功响应（自定义消息和数据）
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(ResultCodeEnum.SUCCESS.getCode(), message, data);
    }

    /**
     * 失败响应（默认失败消息）
     */
    public static <T> ApiResponse<T> error() {
        return new ApiResponse<>(ResultCodeEnum.FAIL);
    }

    /**
     * 失败响应（自定义消息）
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(ResultCodeEnum.FAIL.getCode(), message);
    }

    /**
     * 失败响应（指定错误码和消息）
     */
    public static <T> ApiResponse<T> error(Integer code, String message) {
        return new ApiResponse<>(code, message);
    }

    /**
     * 失败响应（使用枚举）
     */
    public static <T> ApiResponse<T> error(ResultCodeEnum resultCodeEnum) {
        return new ApiResponse<>(resultCodeEnum);
    }

    /**
     * 失败响应（使用枚举和数据）
     */
    public static <T> ApiResponse<T> error(ResultCodeEnum resultCodeEnum, T data) {
        return new ApiResponse<>(resultCodeEnum, data);
    }

    /**
     * 网络错误响应
     */
    public static <T> ApiResponse<T> networkError() {
        return new ApiResponse<>(ResultCodeEnum.NETWORK_ERROR);
    }

    /**
     * 网络超时响应
     */
    public static <T> ApiResponse<T> networkTimeout() {
        return new ApiResponse<>(ResultCodeEnum.NETWORK_TIMEOUT);
    }

    /**
     * 根据布尔值返回成功或失败
     */
    public static <T> ApiResponse<T> result(boolean flag) {
        return flag ? success() : error();
    }

    /**
     * 根据布尔值返回成功或失败（带消息）
     */
    public static <T> ApiResponse<T> result(boolean flag, String successMessage, String errorMessage) {
        return flag ? success(successMessage) : error(errorMessage);
    }

    /**
     * 根据布尔值返回成功或失败（带数据）
     */
    public static <T> ApiResponse<T> result(boolean flag, T data) {
        return flag ? success(data) : error();
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return ResultCodeEnum.SUCCESS.getCode().equals(this.code);
    }

    /**
     * 判断是否失败
     */
    public boolean isError() {
        return !isSuccess();
    }

    /**
     * 判断是否为网络错误
     */
    public boolean isNetworkError() {
        ResultCodeEnum resultCode = ResultCodeEnum.getByCode(this.code);
        return resultCode != null && resultCode.isNetworkError();
    }

    /**
     * 判断是否为服务器错误
     */
    public boolean isServerError() {
        ResultCodeEnum resultCode = ResultCodeEnum.getByCode(this.code);
        return resultCode != null && resultCode.isServerError();
    }

    /**
     * 获取错误信息，如果成功则返回null
     */
    public String getErrorMessage() {
        return isSuccess() ? null : message;
    }

    /**
     * 安全获取数据，如果失败或数据为空则返回null
     */
    public T getDataSafely() {
        return isSuccess() ? data : null;
    }

    // Getter 和 Setter 方法
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    // Parcelable 实现
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (code == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(code);
        }
        dest.writeString(message);
        if (timestamp == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(timestamp);
        }
        // 注意：data字段需要根据具体类型处理
    }

    public static final Creator<ApiResponse> CREATOR = new Creator<ApiResponse>() {
        @Override
        public ApiResponse createFromParcel(Parcel in) {
            return new ApiResponse(in);
        }

        @Override
        public ApiResponse[] newArray(int size) {
            return new ApiResponse[size];
        }
    };

    @Override
    public String toString() {
        return "ApiResponse{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", timestamp=" + timestamp +
                '}';
    }
}