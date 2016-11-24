package com.qiniu.android.http;


import com.qiniu.android.collect.Config;
import com.qiniu.android.collect.UploadInfoCollector;
import com.qiniu.android.common.Constants;
import com.qiniu.android.utils.StringUtils;

import org.json.JSONObject;

import java.util.Locale;

/**
 * 定义HTTP请求的日志信息和常规方法
 */
public final class ResponseInfo {
    public static final int ZeroSizeFile = -6;
    public static final int InvalidToken = -5;
    public static final int InvalidArgument = -4;
    public static final int InvalidFile = -3;
    public static final int Cancelled = -2;
    public static final int NetworkError = -1;

    // <-- error code copy from ios
    public static final int TimedOut = -1001;
    public static final int UnknownHost = -1003;
    public static final int CannotConnectToHost = -1004;
    public static final int NetworkConnectionLost = -1005;

    // -->
    /**
     * 回复状态码
     */
    public final int statusCode;
    /**
     * 七牛日志扩展头
     */
    public final String reqId;
    /**
     * 七牛日志扩展头
     */
    public final String xlog;
    /**
     * cdn日志扩展头
     */
    public final String xvia;
    /**
     * 错误信息
     */
    public final String error;
    /**
     * 请求消耗时间，单位秒
     */
    public final double duration;
    /**
     * 服务器域名
     */
    public final String host;
    /**
     * 服务器IP
     */
    public final String ip;

    /**
     * 服务器端口
     */
    public final int port;

    /**
     * 访问路径
     */
    public final String path;

    /**
     * user agent id
     */
    public final String id;

    /**
     * log 时间戳
     */
    public final long timeStamp;

    /**
     * 已发送字节数
     */
    public final long sent;

    public final String accessKey;

    /**
     * hide, 内部使用
     */
    public final JSONObject response;

    private ResponseInfo(JSONObject json, int statusCode, String reqId, String xlog, String xvia, String host,
                         String path, String ip, int port, double duration, long sent, String error, String accessKey) {
        response = json;
        this.statusCode = statusCode;
        this.reqId = reqId;
        this.xlog = xlog;
        this.xvia = xvia;
        this.host = host;
        this.path = path;
        this.duration = duration;
        this.error = error;
        this.ip = ip;
        this.port = port;
        this.id = UserAgent.instance().id;
        this.timeStamp = System.currentTimeMillis() / 1000;
        this.sent = sent;
        this.accessKey = accessKey;
    }

    public static ResponseInfo create(final JSONObject json, final int statusCode, final String reqId, final String xlog, final String xvia, final String host,
                                      final String path, final String ip, final int port, final double duration, final long sent, final String error, final String accessKey) {

        ResponseInfo res = new ResponseInfo(json, statusCode, reqId, xlog, xvia, host, path, ip, port, duration, sent, error, accessKey);

        if (Config.isRecord) {
            final String _id = res.id;
            final String _error = error + "";
            final String _timeStamp = res.timeStamp + "";
            UploadInfoCollector.handle(
                    // 延迟序列化.如果判断不记录,则不执行序列化
                    new UploadInfoCollector.RecordMsg() {

                        @Override
                        public String toRecordMsg() {
                            String[] ss = {statusCode + "", reqId, replace(xlog), replace(xvia), host, path, ip, port + "", duration + "",
                                    accessKey, "android:" + Constants.VERSION, _id, _timeStamp, sent + "", replace(_error)};
                            return StringUtils.join(ss, ", ");
                        }
                    });
        }
        return res;
    }

    public static ResponseInfo zeroSize(String accessKey) {
        return create(null, ZeroSizeFile, "", "", "", "", "", "", -1, 0, 0, "file or data size is zero", accessKey);
    }

    public static ResponseInfo cancelled(String accessKey) {
        return create(null, Cancelled, "", "", "", "", "", "", -1, 0, 0, "cancelled by user", accessKey);
    }

    public static ResponseInfo invalidArgument(String message, String accessKey) {
        return create(null, InvalidArgument, "", "", "", "", "", "", -1, 0, 0, message, accessKey);
    }

    public static ResponseInfo invalidToken(String message) {
        return create(null, InvalidToken, "", "", "", "", "", "", -1, 0, 0, message, null);
    }

    public static ResponseInfo fileError(Exception e, String accessKey) {
        return create(null, InvalidFile, "", "", "", "", "", "", -1, 0, 0, e.getMessage(), accessKey);
    }

    public boolean isCancelled() {
        return statusCode == Cancelled;
    }

    public boolean isOK() {
        return statusCode == 200 && error == null && (hasReqId() || response != null);
    }

    public boolean isNetworkBroken() {
        return statusCode == NetworkError || statusCode == UnknownHost
                || statusCode == CannotConnectToHost || statusCode == TimedOut
                || statusCode == NetworkConnectionLost;
    }

    public boolean isServerError() {
        return (statusCode >= 500 && statusCode < 600 && statusCode != 579)
                || statusCode == 996;
    }

    public boolean needSwitchServer() {
        return isNetworkBroken() || isServerError();
    }

    public boolean needRetry() {
        return !isCancelled() && (needSwitchServer() || statusCode == 406
                || (statusCode == 200 && error != null));
    }

    public boolean isNotQiniu() {
        return statusCode < 500 && statusCode >= 200 && (!hasReqId() && response == null);
    }

    public String toString() {
        return String.format(Locale.ENGLISH, "{ver:%s,ResponseInfo:%s,status:%d, reqId:%s, xlog:%s, xvia:%s, host:%s, path:%s, ip:%s, port:%d, duration:%f s, time:%d, sent:%d,error:%s}",
                Constants.VERSION, id, statusCode, reqId, xlog, xvia, host, path, ip, port, duration, timeStamp, sent, error);
    }

    public boolean hasReqId() {
        return reqId != null;
    }

}
