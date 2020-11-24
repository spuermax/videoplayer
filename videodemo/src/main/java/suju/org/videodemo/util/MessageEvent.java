package suju.org.videodemo.util;

/**
 * Created by RexXiang on 2017/8/2.
 */

public class MessageEvent<T> {

    public static final int NO_CODE = -1;

    public static final int SWITCH_PAGE = 16;
    public static final int TOKEN_EXPIRED = 17; //token过期
    public static final int INIT_APP = 19;
    public static final int SIGN_IN = 20; //登录
    public static final int SIGN_OUT = 21; //退出
    public static final int REFRESH_LIVE_STATUS = 22; //刷新预约状态
    public static final int REFRESH_FREETOPIC_STATUS = 23; //刷新题库状态

    public static final int VIDEO_FULL_SCREEN = 24; //视频全屏
    public static final int VIDEO_FIRST_TASK = 25; //显示第一个课时
    public static final int VIDEO_CLICK_TASK = 26; //显示选择课时
    public static final int VIDEO_HIDE_CONTROLLER = 27; //隐藏标题栏
    public static final int VIDEO_SHOW_CONTROLLER = 28; //显示标题栏
    public static final int VIDEO_REFRESH_SCHEDULE = 29; //刷新列表进度
    public static final int VIDEO_NO_AUDIO = 30; //暂无音频
    public static final int VIDEO_JOIN_CLOSE_DIALOG = 99;
    public static final int VIDEO_JOIN_VIDEO_CLOSE_DIALOG = 100;

    public static final int HOTCLASS_SHOW_DETAIL = 31; //热门班级 显示详情
    public static final int CLASS_PLAY_SUCCESS = 32; //班级购买成功

    public static final int EXAM_CHANGE_ANSWER = 33; //免费题 选择答案
    public static final int EXAM_NEXT_QUESTION = 34; //免费题 下一题
    public static final int EXAM_CARD_JUMP = 35;  //免费题 题卡跳转
    public static final int EXAM_MYQUESTION_REFRESH = 36;  //免费题 错题集列表/我的收藏列表 刷新

    public static final int USER_INFO_UPDATE = 37;  //用户信息更新

    public static final int DOWNLOAD_SWITCH_STATUS = 38;
    public static final int DOWNLOAD_CHECK_ALL = 39;
    public static final int DOWNLOAD_DELETE = 40;

    public static final int REFRESH_ORDER_LIST = 41; //刷新订单
    public static final int REFRESH_CLASS_DETAIL = 42; //刷新学习中心班级
    public static final int REFRESH_EXAM_HOME = 43; //刷新exam
    public static final int SHOW_RENBAO_TIPS = 44;//是否展示保单
    public static final int REFRESH_LIVE_DATA = 45;//是否展示保单
    public static final int REFRESH_SHOW_VIDEO_DATA = 46;//数据帅新
    public static final int REFRESH_SHOW_VIDEO_QUESTION = 47;//数据帅新
    public static final int REFRESH_SHOW_VIDEO_DATA_TIPS = 48;//数据刷新
    public static final int EXAM_NEXT_QUESTION_VIDEO = 49; //免费题 下一题

    public static final int FINISH_CLASSROOM_CODE = 9000; //刷新exam
    public static final int FINISH_VIDEO_COURSE_PLAY = 50;
    public static final int VIDEO_COURSE_NEXT_PLAY = 51;
    public static final int REFRESH_SHOW_EXAM_DIALOG = 51;// 刷新题库的弹窗


    public static final int SORT_OPEN = 1001;
    public static final int SORT_OFF = 1002;
    public static final int SORT_REFRESH = 1003;
    public static final int SORT_REFRESH_FRAGMENT = 1004;

    //登录是否选中单选按钮
    public static final int LOGIN_CHECK=10001;



    /**
     * 更新Adapter item状态：高亮、半圈
     */

    private T mMessage;
    private int mCode;

    public MessageEvent(T message) {
        mMessage = message;
        mCode = NO_CODE;
    }

    public MessageEvent(T message, int code) {
        mMessage = message;
        mCode = code;
    }

    public MessageEvent(int code) {
        mMessage = null;
        mCode = code;
    }

    public T getMessageBody() {
        return mMessage;
    }

    public int getType() {
        return mCode;
    }
}
