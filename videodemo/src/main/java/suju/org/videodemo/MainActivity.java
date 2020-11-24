package suju.org.videodemo;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;


import com.edusoho.videoplayer.ui.VideoPlayerFragment;
import com.edusoho.videoplayer.util.*;
import com.edusoho.videoplayer.util.VLCOptions;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suju.org.videodemo.data.VideoModel;
import suju.org.videodemo.util.MessageEvent;

public class MainActivity extends AppCompatActivity {

    private String mediaUrl;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        viewPager = findViewById(R.id.view_pager);

        //事件监听
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                //requestMediaUrl();
//                //loadVideoPlayer("http://default.andy.dev.qiqiuyun.cn:8071/video-player/examples/server/playlist.m3u8");
//                 loadVideoPlayer("http://mov.bn.netease.com/open-movie/nos/mp4/2016/10/24/SC37RM09R_hd.mp4");
////                 loadVideoPlayer("http://dev-myh.saiyoujiaoyu.com/player/21129/file/WijUlofofPDXZ5Zu8BgrzI0yTOa3ic8J/nologin");
//                //loadVideoPlayer("http://demo.edusoho.com/hls/2875/playlist/vri5h4wud6PQDJFiNMowZvMfGBwcJdEo.m3u8");
//                //loadAudioPlayer("http://yinyueshiting.baidu.com/data2/music/0da46a611fa4381fc02823d1a14ce952/300672476/300672476.mp3?xcode=4aa420456406eda005ffee87c3adb48c");
//            }
//        }).start();

        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("http://mov.bn.netease.com/open-movie/nos/mp4/2016/10/24/SC37RM09R_hd.mp4");
        arrayList.add("http://mov.bn.netease.com/open-movie/nos/mp4/2016/10/24/SC37RM09R_hd.mp4");
        arrayList.add("http://mov.bn.netease.com/open-movie/nos/mp4/2016/10/24/SC37RM09R_hd.mp4");
        arrayList.add("http://mov.bn.netease.com/open-movie/nos/mp4/2016/10/24/SC37RM09R_hd.mp4");
        arrayList.add("http://mov.bn.netease.com/open-movie/nos/mp4/2016/10/24/SC37RM09R_hd.mp4");
        arrayList.add("http://mov.bn.netease.com/open-movie/nos/mp4/2016/10/24/SC37RM09R_hd.mp4");
        arrayList.add("http://mov.bn.netease.com/open-movie/nos/mp4/2016/10/24/SC37RM09R_hd.mp4");
        arrayList.add("http://mov.bn.netease.com/open-movie/nos/mp4/2016/10/24/SC37RM09R_hd.mp4");
        arrayList.add("http://mov.bn.netease.com/open-movie/nos/mp4/2016/10/24/SC37RM09R_hd.mp4");
        arrayList.add("http://mov.bn.netease.com/open-movie/nos/mp4/2016/10/24/SC37RM09R_hd.mp4");
        arrayList.add("http://mov.bn.netease.com/open-movie/nos/mp4/2016/10/24/SC37RM09R_hd.mp4");
        arrayList.add("http://mov.bn.netease.com/open-movie/nos/mp4/2016/10/24/SC37RM09R_hd.mp4");
        arrayList.add("http://mov.bn.netease.com/open-movie/nos/mp4/2016/10/24/SC37RM09R_hd.mp4");
        arrayList.add("http://mov.bn.netease.com/open-movie/nos/mp4/2016/10/24/SC37RM09R_hd.mp4");
        arrayList.add("http://mov.bn.netease.com/open-movie/nos/mp4/2016/10/24/SC37RM09R_hd.mp4");
        arrayList.add("http://mov.bn.netease.com/open-movie/nos/mp4/2016/10/24/SC37RM09R_hd.mp4");
        arrayList.add("http://mov.bn.netease.com/open-movie/nos/mp4/2016/10/24/SC37RM09R_hd.mp4");

        viewPager.setAdapter(new ViewPagerAdapter(this, arrayList));
//        viewPager.setAdapter(new ViewPagerAdapter2(arrayList, this));

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                EventBus.getDefault().post(new MessageEvent<>(MessageEvent.EXAM_NEXT_QUESTION_VIDEO));

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

    }

    private static Bitmap maskImageForSmallSDK17(Bitmap bitmap) {
        Bitmap scaleBitmap = scaleBitmap(bitmap, 0.05f);
        scaleBitmap = scaleBitmap(scaleBitmap, 20.0f);
        Bitmap canvasBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(canvasBitmap);
        Paint paint = new Paint();
        paint.setMaskFilter(new BlurMaskFilter(100f, BlurMaskFilter.Blur.SOLID));

        canvas.drawBitmap(scaleBitmap, 0, 0, paint);
        canvas.drawColor(Color.parseColor("#3f000000"));

        bitmap.recycle();
        return canvasBitmap;
    }

    private static Bitmap scaleBitmap(Bitmap bitmap, float scale) {
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale); //长和宽放大缩小的比例
        Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizeBmp;
    }

    public Bitmap maskImage(Context context, Bitmap bitmap) {

        Bitmap scaleBitmap = scaleBitmap(bitmap, 0.7f);
        bitmap.recycle();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return scaleBitmap;
        }
        RenderScript rs = RenderScript.create(context);
        Allocation allocation = Allocation.createFromBitmap(rs, bitmap);
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, allocation.getElement());
        blurScript.setInput(allocation);
        blurScript.setRadius(20f);
        blurScript.forEach(allocation);
        allocation.copyTo(bitmap);
        rs.destroy();
        return bitmap;
    }

    private void requestMediaUrl() {
        try {
            URL url = new URL("http://trymob.edusoho.cn/mapi_v2/Lesson/getLesson?courseId=87&lessonId=494&token=j1z92o1u8ls84og80sg0kckcogkgwc4");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            InputStream stream = urlConnection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line = null;
            StringBuilder sb = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            JSONObject jsonObject = new JSONObject(sb.toString());
            mediaUrl = jsonObject.optString("mediaUri");
        } catch (Exception e) {
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadVideoPlayer(mediaUrl);
            }
        });
    }

    protected void loadAudioPlayer(String mediaUri) {

       /* FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Fragment fragment = new CustomAudioFragment();
        getIntent().putExtra(AudioPlayerFragment.PLAY_URI, mediaUri);
        //getIntent().putExtra(VideoPlayerFragment.PLAY_URI, "http://default.andy.dev.qiqiuyun.cn:8071/video-player/examples/server/playlist.m3u8");
        fragment.setArguments(getIntent().getExtras());
        transaction.add(R.id.fl_video_container, fragment);
        transaction.commit();*/
    }

    protected void loadVideoPlayer(String mediaUri) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        VideoPlayerFragment fragment = new VideoPlayerFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(VideoPlayerFragment.PLAY_MEDIA_CODER, VLCOptions.NONE_RATE);
        bundle.putString(VideoPlayerFragment.PLAY_URI, mediaUri);
        //getIntent().putExtra(VideoPlayerFragment.PLAY_URI, "http://default.andy.dev.qiqiuyun.cn:8071/video-player/examples/server/playlist.m3u8");
        fragment.setArguments(bundle);
        transaction.add(R.id.fl_video_container, fragment);

        transaction.commit();
        fragment.playVideo(mediaUri);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }


    public class ViewPagerAdapter extends PagerAdapter {

        private List<String> arrayList;
        private Context mContext;

        public ViewPagerAdapter(Context mContext, List<String> arrayList) {
            super();
            this.arrayList = arrayList;
            this.mContext = mContext;
        }

        @Override
        public int getCount() {
            return arrayList.size();
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            Log.i("AAAAAAAA", "-----销毁 老弟 ------" + object.toString());
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull final ViewGroup container, final int position) {
            final int po = position;
            final View inflate = LayoutInflater.from(mContext).inflate(R.layout.adapter_item_pager, container, false);
            Log.i("AAAAAAAA", "-----来了 老弟 ------" + position + "--" + inflate.toString());

            final RelativeLayout  linearLayout = inflate.findViewById(R.id.ll_layout);
            final FrameLayout video_container = inflate.findViewById(R.id.video_container);

            inflate.findViewById(R.id.btn_start);
//            inflate.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    fragment viewById = inflate.findViewById(R.id.fragment_layout);
//                }
//            });


            inflate.findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    VideoTestFragment videoFragment = new VideoTestFragment();

                    Bundle bundle = new Bundle();
                    bundle.putInt(VideoPlayerFragment.PLAY_MEDIA_CODER, VLCOptions.NONE_RATE);
                    bundle.putString(VideoPlayerFragment.PLAY_URI, arrayList.get(po));
                    videoFragment.setArguments(bundle);
                    AppCompatActivity activity = (AppCompatActivity) mContext;
                    activity.getSupportFragmentManager().getFragments().size();
                    FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.video_container, videoFragment, "111");
                    transaction.commitAllowingStateLoss();
                    videoFragment.playVideo(arrayList.get(po));


                    Log.i("AAAAAAAA", "-----------" + po + "----" + video_container.hashCode());
                    Log.i("AAAAAAAA", "-----------" + po + "----" + inflate.hashCode());
                    Log.i("AAAAAAAA", "-----------" + po + "----" + videoFragment.isHidden()  + videoFragment.getUserVisibleHint());
                    Log.i("AAAAAAAA", "-----------" + po + "----" + activity.getSupportFragmentManager().getFragments().size());

                }
            });


            container.addView(inflate);
            return inflate;
        }
    }

    @Subscribe
    public void onReceiveMessage(MessageEvent messageEvent) {
    }
}
