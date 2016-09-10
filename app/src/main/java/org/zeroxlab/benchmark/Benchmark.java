/*
 * Copyright (C) 2010-2011 0xlab - http://0xlab.org/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authored by Julian Chu <walkingice@0xlab.org> and
 *             Joseph Chang (bizkit) <bizkit@0xlab.org>
 */

package org.zeroxlab.benchmark;

import android.util.Log;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.StringBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.UUID;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.opensolaris.hub.libmicro.NativeCaseMicro;
import org.zeroxlab.byteunix.NativeCaseUbench;
import org.zeroxlab.utils.BenchUtil;
import org.zeroxlab.benchmark.R;

/* Construct a basic UI */
public class Benchmark extends TabActivity implements View.OnClickListener {

    public final static String TAG     = "Benchmark";
    public final static String PACKAGE = "org.zeroxlab.benchmark";

    private final static String mOutputFile = "0xBenchmark";

    private final static int GROUP_DEFAULT = 0;
    private final static int SETTINGS_ID = Menu.FIRST;

    private static String mXMLResult;
    private static String mJSONResult;
    private final static String mOutputXMLFile = "0xBenchmark.xml";
    private final static String mOutputJSONFile = "0xBenchmark.bundle";

    private Button   mRun;
    private Button   mShow;
    private CheckBox mCheckList[];
    private TextView mDesc[];
    private TextView mBannerInfo;

    private ScrollView   mScrollView;
    private LinearLayout mLinearLayout;
    private LinearLayout mMainView;
    private TabHost mTabHost;

    LinkedList<Case> mCases;
    boolean mTouchable = true;
    private int orientation = Configuration.ORIENTATION_UNDEFINED;

    private WakeLock mWakeLock;

    private final String MAIN = "Main";
    private final String D2 = "2D";
    private final String D3 = "3D";
    private final String MATH = "Math";
    private final String VM = "VM";
    private final String NATIVE = "Native";
    private final String MISC = "Misc";

    private CheckBox d2CheckBox;
    private CheckBox d3CheckBox;
    private CheckBox mathCheckBox;
    private CheckBox vmCheckBox;
    private CheckBox nativeCheckBox;
    private CheckBox miscCheckBox;

    private HashMap< String, HashSet<Case> > mCategory = new HashMap< String, HashSet<Case> >();

    private final String trackerUrl = "http://0xbenchmark.appspot.com/static/MobileTracker.html";

    boolean mAutoRun = false;
    boolean mCheckMath = false;
    boolean mCheck2D = false;
    boolean mCheck3D = false;
    boolean mCheckVM = false;
    boolean mCheckNative = false;
    boolean mCheckMisc = false;
    boolean mAutoUpload = false;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWakeLock.release();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 获取当前屏幕方向，其实AndroidManifest.xml已经配置了，设置屏幕亮度
        orientation = getResources().getConfiguration().orientation;
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
        mWakeLock.acquire();

        // 创建所有的要测试的分类中的项
        setContentView(R.layout.main);

        mCases = new LinkedList<Case>();
        Case arith  = new CaseArithmetic();
        Case javascript = new CaseJavascript();
        Case scimark2  = new CaseScimark2();
        Case canvas = new CaseCanvas();
        Case glcube = new CaseGLCube();
        Case circle = new CaseDrawCircle();
        Case nehe08 = new CaseNeheLesson08();
        Case nehe16 = new CaseNeheLesson16();
        Case teapot = new CaseTeapot();
        Case gc     = new CaseGC();
        Case libMicro = new NativeCaseMicro();
        Case libUbench = new NativeCaseUbench();

        Case dc2 = new CaseDrawCircle2();
        Case dr = new CaseDrawRect();
        Case da = new CaseDrawArc();
        Case di = new CaseDrawImage();
        Case dt = new CaseDrawText();

        // 往分类中添加分类，键值对，后面是HashSet，所以方便后面可以直接添加Case。
        mCategory.put(D2, new HashSet<Case>());
        mCategory.put(D3, new HashSet<Case>());
        mCategory.put(MATH, new HashSet<Case>());
        mCategory.put(VM, new HashSet<Case>());
        mCategory.put(NATIVE, new HashSet<Case>());
        mCategory.put(MISC, new HashSet<Case>());

        /**
         * 接下来主要是将所有的Case添加进入mCases中，并添加到对应的集合中。
         */

        // mflops
        mCases.add(arith);
        mCases.add(scimark2);
        mCases.add(javascript);

        mCategory.get(MATH).add(arith);
        mCategory.get(MATH).add(scimark2);
        mCategory.get(MISC).add(javascript);

        // 2d
        mCases.add(canvas);
        mCases.add(circle);
        mCases.add(dc2);
        mCases.add(dr);
        mCases.add(da);
        mCases.add(di);
        mCases.add(dt);

        mCategory.get(D2).add(canvas);
        mCategory.get(D2).add(circle);
        mCategory.get(D2).add(dc2);
        mCategory.get(D2).add(dr);
        mCategory.get(D2).add(da);
        mCategory.get(D2).add(di);
        mCategory.get(D2).add(dt);

        // 3d
        mCases.add(glcube);
        mCases.add(nehe08);
        mCases.add(nehe16);
        mCases.add(teapot);

        mCategory.get(D3).add(glcube);
        mCategory.get(D3).add(nehe08);
        mCategory.get(D3).add(nehe16);
        mCategory.get(D3).add(teapot);

        // vm
        mCases.add(gc);

        mCategory.get(VM).add(gc);

        // native
        mCases.add(libMicro);
        mCases.add(libUbench);

        mCategory.get(NATIVE).add(libMicro);
        mCategory.get(NATIVE).add(libUbench);

        // 初始化显示主界面
        initViews();

        // 获取意图参数，如果参数中有选择打开某个checkbox，那么就模拟点击，相当于选中
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            mAutoRun = bundle.getBoolean("autorun");
            mCheckMath = bundle.getBoolean("math");
            mCheck2D = bundle.getBoolean("2d");
            mCheck3D = bundle.getBoolean("3d");
            mCheckVM = bundle.getBoolean("vm");
            mCheckNative = bundle.getBoolean("native");
            mAutoUpload = bundle.getBoolean("autoupload");
        }

        if (mCheckMath && !mathCheckBox.isChecked()) {
            // 模拟点击
            mathCheckBox.performClick();
        }

        if (mCheck2D && !d2CheckBox.isChecked()) {
            d2CheckBox.performClick();
        }

        if (mCheck3D && !d3CheckBox.isChecked()) {
            d3CheckBox.performClick();
        }

        if (mCheckVM && !vmCheckBox.isChecked()) {
            vmCheckBox.performClick();
        }

        if (mCheckNative && !nativeCheckBox.isChecked()) {
            nativeCheckBox.performClick();
        }

        if (mCheckMisc && !miscCheckBox.isChecked()) {
            miscCheckBox.performClick();
        }
        /*
        if (intent.getBooleanExtra("AUTO", false)) {
            ImageView head = (ImageView)findViewById(R.id.banner_img);
            head.setImageResource(R.drawable.icon_auto);
            mTouchable = false;
            initAuto();
        }
        */

        // 相当于点击了mRun按钮。
        if (mAutoRun) {
            onClick(mRun);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem item1 = menu.add(GROUP_DEFAULT, SETTINGS_ID, Menu.NONE, R.string.menu_settings);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menu) {
        if (menu.getGroupId() == GROUP_DEFAULT && menu.getItemId() == SETTINGS_ID) {
            org.zeroxlab.utils.Util.launchActivity(this, "org.zeroxlab.benchmark.ActivitySettings");
        }
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mTouchable) {
            return super.dispatchTouchEvent(event);
        } else {
            return true;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mTouchable) {
            return super.dispatchKeyEvent(event);
        } else {
            return true;
        }
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent event) {
        if (mTouchable) {
            return super.dispatchTrackballEvent(event);
        } else {
            return true;
        }
    }

    private void _checkTagCase(String [] Tags) {
        Arrays.sort(Tags);
        for (int i = 0; i < mCheckList.length; i++) {
            String [] caseTags = mCases.get(i).mTags;
            for (String t: caseTags) {
                int search = Arrays.binarySearch(Tags, t);
                if (search >= 0)
                    mCheckList[i].setChecked(true);
            }
        }
    }

    private void _checkCatCase(String [] Cats) {
        Arrays.sort(Cats);
        for (int i = 0; i < mCheckList.length; i++) {
            int search = Arrays.binarySearch(Cats, mCases.get(i).mType);
            if (search  >= 0)
                mCheckList[i].setChecked(true);
        }
    }

    private void _checkAllCase(boolean check) {
        for (int i = 0; i < mCheckList.length; i++)
            mCheckList[i].setChecked(check);
    }

    private void initAuto() {
        Intent intent = getIntent();
        String TAG = intent.getStringExtra("TAG");
        String CAT = intent.getStringExtra("CAT");


        _checkAllCase(false);
        if (TAG != null)
            _checkTagCase( TAG.split(",") );
        if (CAT != null)
            _checkCatCase( CAT.split(",") );
        if (TAG == null && CAT == null)
            _checkAllCase(true);
        final Handler h = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 0x1234)
                    onClick(mRun);
            }
    };
    
    final ProgressDialog dialog = new ProgressDialog(this).show(this, "Starting Benchmark", "Please wait...", true, false);
    new Thread() {
            public void run() {
                SystemClock.sleep(1000);
                dialog.dismiss();
                Message m = new Message();
                m.what = 0x1234;
                h.sendMessage(m);
            }
        }.start();
        mTouchable = true;
    }

    private void initViews() {
        /*
        mRun = (Button)findViewById(R.id.btn_run);
        mRun.setOnClickListener(this);

        mShow = (Button)findViewById(R.id.btn_show);
        mShow.setOnClickListener(this);
        mShow.setClickable(false);

        mLinearLayout = (LinearLayout)findViewById(R.id.list_container);
        mMainView = (LinearLayout)findViewById(R.id.main_view);

        mBannerInfo = (TextView)findViewById(R.id.banner_info);
        mBannerInfo.setText("Hello!\nSelect cases to Run.\nUploaded results:\nhttp://0xbenchmark.appspot.com");
        */

        mTabHost = getTabHost();

        // 有多少个Case就创建多少个对应的CheckBox和TextView，并且根据对应Case来初始化
        int length = mCases.size();
        mCheckList = new CheckBox[length];
        mDesc      = new TextView[length];
        for (int i = 0; i < length; i++) {
            mCheckList[i] = new CheckBox(this);
            mCheckList[i].setText(mCases.get(i).getTitle());

            mDesc[i] = new TextView(this);
            mDesc[i].setText(mCases.get(i).getDescription());
            mDesc[i].setTextSize(mDesc[i].getTextSize() - 2);
            mDesc[i].setPadding(42, 0, 10, 10);
        }

        TabContentFactory mTCF = new TabContentFactory() {
            public View createTabContent(String tag) {
                ViewGroup.LayoutParams fillParent = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
                ViewGroup.LayoutParams fillWrap = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                // 水平布局宽度wrap_content，高度wrap_content
                LinearLayout.LayoutParams wrapContent = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                wrapContent.gravity = Gravity.CENTER;

                // 水平布局宽度fill_parent，高度wrap_content
                LinearLayout.LayoutParams weightedFillWrap = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                // 水平布局权重
                weightedFillWrap.weight = 1;

                if (tag.equals(MAIN)) {
                    // 创建线性布局，设置垂直方向，添加滚动条
                    LinearLayout mMainView = new LinearLayout(Benchmark.this);
                    mMainView.setOrientation(1);

                    // 滚动条
                    ScrollView mListScroll = new ScrollView(Benchmark.this);

                    // 创建容器设置方向
                    LinearLayout mMainViewContainer = new LinearLayout(Benchmark.this);
                    mMainViewContainer.setOrientation(1);

                    // 创建图像View，并设置图片
                    ImageView mIconView = new ImageView(Benchmark.this);
                    mIconView.setImageResource(R.drawable.icon);

                    // 创建说明文字View，并设置内容
                    TextView mBannerInfo = new TextView(Benchmark.this);
                    mBannerInfo.setText("0xbench\nSelect benchmarks in the tabs,\nor batch select:");


                    /**
                     * 创建CheckBox View，设置文字，并添加点击监听
                     */

                    d2CheckBox = new CheckBox(Benchmark.this);
                    d2CheckBox.setText(D2);
                    d2CheckBox.setOnClickListener(Benchmark.this);

                    d3CheckBox = new CheckBox(Benchmark.this);
                    d3CheckBox.setText(D3);
                    d3CheckBox.setOnClickListener(Benchmark.this);

                    mathCheckBox = new CheckBox(Benchmark.this);
                    mathCheckBox.setText(MATH);
                    mathCheckBox.setOnClickListener(Benchmark.this);

                    vmCheckBox = new CheckBox(Benchmark.this);
                    vmCheckBox.setText(VM);
                    vmCheckBox.setOnClickListener(Benchmark.this);

                    nativeCheckBox = new CheckBox(Benchmark.this);
                    nativeCheckBox.setText(NATIVE);
                    nativeCheckBox.setOnClickListener(Benchmark.this);

                    miscCheckBox = new CheckBox(Benchmark.this);
                    miscCheckBox.setText(MISC);
                    miscCheckBox.setOnClickListener(Benchmark.this);

                    // 显示网站信息内容
                    TextView mWebInfo = new TextView(Benchmark.this);
                    mWebInfo.setText("Uploaded results:\nhttp://0xbenchmark.appspot.com");

                    // 创建按钮容器，创建按钮，添加文字，添加监听处理，并添加入容器内
                    LinearLayout mButtonContainer = new LinearLayout(Benchmark.this);
                    mRun = new Button(Benchmark.this);
                    mShow = new Button(Benchmark.this);
                    mRun.setText("Run");
                    mShow.setText("Show");
                    mRun.setOnClickListener(Benchmark.this);
                    mShow.setOnClickListener(Benchmark.this);
                    mButtonContainer.addView(mRun, weightedFillWrap);
                    mButtonContainer.addView(mShow, weightedFillWrap);

                    // 添加web访问，并添加监听处理
                    WebView mTracker = new WebView(Benchmark.this);
                    mTracker.clearCache(true);
                    mTracker.setWebViewClient(new WebViewClient () {
                        public void onPageFinished(WebView view, String url) {
                            Log.i(TAG, "Tracker: " + view.getTitle() + " -> " + url);
                        }
                        public void onReceivedError(WebView view, int errorCode,
                                                    String description, String failingUrl) {
                            Log.e(TAG, "Track err: " + description);
                        }
                    });
                    mTracker.loadUrl(trackerUrl);

                    // 将上面所有的View进行组合
                    mMainViewContainer.addView(mIconView,wrapContent);
                    mMainViewContainer.addView(mBannerInfo);
                    mMainViewContainer.addView(mathCheckBox);
                    mMainViewContainer.addView(d2CheckBox);
                    mMainViewContainer.addView(d3CheckBox);
                    mMainViewContainer.addView(vmCheckBox);
                    mMainViewContainer.addView(nativeCheckBox);
                    mMainViewContainer.addView(miscCheckBox);
                    mMainViewContainer.addView(mWebInfo);
                    mMainViewContainer.addView(mButtonContainer, fillWrap);
                    // 设置添加的View的宽、高
                    mMainViewContainer.addView(mTracker, 0,0);

                    // 加入滚动
                    mListScroll.addView(mMainViewContainer, fillParent);

                    // 将滚动View加入显示View中
                    mMainView.addView(mListScroll, fillWrap);

                    return mMainView;

                }

                /**
                 * 以下内容相当于实现了else的内容
                 */

                LinearLayout mMainView = new LinearLayout(Benchmark.this);
                mMainView.setOrientation(1);

                ScrollView mListScroll = new ScrollView(Benchmark.this);

                LinearLayout mListContainer = new LinearLayout(Benchmark.this);
                mListContainer.setOrientation(1);

                // 加入滚动
                mListScroll.addView(mListContainer, fillParent);

                // 将滚动View加入显示View中
                mMainView.addView(mListScroll, fillWrap);

                boolean gray = true;
                int length = mCases.size();
                Log.i(TAG, "L: " + length);
                Log.i(TAG, "TCF: " + tag);
                for (int i = 0; i < length; i++) {

                    // 判断分类标签和Cases中的名字是否一样，如果不一样，则下一个
                    if (!mCategory.get(tag).contains(mCases.get(i)))
                        continue;

                    // 添加对应的CheckList和TextView到容器中，并按奇偶修改显示背景
                    Log.i(TAG, "Add: " + i); 
                    mListContainer.addView(mCheckList[i], fillWrap);
                    mListContainer.addView(mDesc[i], fillWrap);
                    if (gray) {
                        int color = 0xFF333333; //ARGB
                        mCheckList[i].setBackgroundColor(color);
                        mDesc[i].setBackgroundColor(color);
                    }
                    gray = !gray;
                }
                return mMainView;
            }
        };

        /**
         * 设置生成7个对应的Tab标签
         */
        mTabHost.addTab(mTabHost.newTabSpec(MAIN).setIndicator(MAIN, getResources().getDrawable(R.drawable.ic_eye)).setContent(mTCF));
        mTabHost.addTab(mTabHost.newTabSpec(D2).setIndicator(D2, getResources().getDrawable(R.drawable.ic_2d)).setContent(mTCF));
        mTabHost.addTab(mTabHost.newTabSpec(D3).setIndicator(D3, getResources().getDrawable(R.drawable.ic_3d)).setContent(mTCF));
        mTabHost.addTab(mTabHost.newTabSpec(MATH).setIndicator(MATH, getResources().getDrawable(R.drawable.ic_pi)).setContent(mTCF));
        mTabHost.addTab(mTabHost.newTabSpec(VM).setIndicator(VM, getResources().getDrawable(R.drawable.ic_vm)).setContent(mTCF));
        mTabHost.addTab(mTabHost.newTabSpec(NATIVE).setIndicator(NATIVE, getResources().getDrawable(R.drawable.ic_c)).setContent(mTCF));
        mTabHost.addTab(mTabHost.newTabSpec(MISC).setIndicator(MISC, getResources().getDrawable(R.drawable.ic_misc)).setContent(mTCF));
    }

    /**
     * 该函数主要处理三类事情：
     * 1. 运行按钮；
     * 2. 现实按钮；
     * 3. 选择需要测试的分类的按钮。
     *
     * @param v
     */
    public void onClick(View v) {

        Log.e("Benchmark", "View");

        // 判断是那个按钮被按下了
        if (v == mRun) {
            // 用于统计多少个Checkbox被选中
            int numberOfCaseChecked = 0;
            for (int i = 0; i < mCheckList.length; i++) {
                if (mCheckList[i].isChecked()) {
                    mCases.get(i).reset();
                    numberOfCaseChecked++;
                } else {
                    mCases.get(i).clear();
                }
            }

            // 如果Checkbox被选中，那么就运行测试程序
            if (numberOfCaseChecked > 0)
                runCase(mCases);

        } else if (v == mShow) {
            // 获取结果
            String result = getResult();
            Log.i(TAG,"\n\n"+result+"\n\n");
            writeResult(mOutputFile, result);

            // 跳转到Report界面
            Intent intent = new Intent();
            intent.putExtra(Report.REPORT, result);
            intent.putExtra(Report.XML, mXMLResult);
            if (mAutoUpload) {
                intent.putExtra(Report.AUTOUPLOAD, true);
                mAutoUpload = false;
            }
            intent.setClassName(Report.packageName(), Report.fullClassName());
            startActivity(intent);

        } else if (v == d2CheckBox || v == d3CheckBox || v == mathCheckBox ||
                   v == vmCheckBox || v == nativeCheckBox || v == miscCheckBox) {
            // 这里是选择一类，只要属于这一大类，就全部选中
            int length = mCases.size();
            String tag = ((CheckBox)v).getText().toString();
            for (int i = 0; i < length; i++) {
                if (!mCategory.get(tag).contains(mCases.get(i)))
                    continue;
                mCheckList[i].setChecked(((CheckBox)v).isChecked());
            }
        }
    }

    public void runCase(LinkedList<Case> list) {
        Case pointer = null;

        // 检查是否需要结束，同时忽略掉不需要测试的case
        boolean finish = true;
        for (int i = 0; i < list.size(); i++) {
            pointer = list.get(i);
            if (!pointer.isFinish()) {
                finish = false;
                break;
            }
        }

        /**
         * 最后执行完了，统计所有的数据，并将数据以合理的形式现实出来。
         */
        if (finish) {
//            mBannerInfo.setText("Benchmarking complete.\nClick Show to upload.\nUploaded results:\nhttp://0xbenchmark.appspot.com");
            // 获取测试结果，并写入输出文件
            String result = getResult();
            writeResult(mOutputFile, result);

            final ProgressDialog dialogGetXml = new ProgressDialog(this).show(this, "Generating XML Report", "Please wait...", true, false);
            new Thread() {
                public void run() {
                    mJSONResult = getJSONResult();
                    mXMLResult = getXMLResult();
                    Log.d(TAG, "XML: " + mXMLResult);
                    writeResult(mOutputXMLFile, mXMLResult);
                    Log.d(TAG, "JSON: " + mJSONResult);
                    writeResult(mOutputJSONFile, mJSONResult);

                    // 相当于点击了一下show按钮
                    mShow.setClickable(true);
                    onClick(mShow);

                    mTouchable = true;
                    dialogGetXml.dismiss();
                }
            }.start();
        } else {
            // 创建一个意图，开始演示
            Intent intent = pointer.generateIntent();
            if (intent != null) {
                startActivityForResult(intent, 0);
            }
        }
    }

    public String getXMLResult() {
        if (mCases.size() == 0)
            return "";

        Date date = new Date();
        //2010-05-28T17:40:25CST
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");

        String xml = "";
        xml += "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        xml += "<result";
        xml += " executedTimestamp=\"" + sdf.format(date) + "\"";
        xml += " manufacturer=\"" + Build.MANUFACTURER.replace(' ', '_') + "\"";
        xml += " model=\"" + Build.MODEL.replace(' ', '_') + ":" + Build.DISPLAY + "\"";
        xml += " buildTimestamp=\"" + sdf.format(new Date(Build.TIME)) + "\"";
        xml += " orientation=\"" + Integer.toString(orientation) + "\"";

        try { // read kernel version
            BufferedReader procVersion = new BufferedReader( new FileReader("/proc/version") );
            StringBuffer sbuff = new StringBuffer();
            String tmp;
            while ((tmp = procVersion.readLine()) != null)
                sbuff.append(tmp);
            procVersion.close();
            tmp = sbuff.toString().replace("[\n\r]+", " ").replace(" +", ".");
            xml += " version=\"" + tmp + "\"";
        } catch (IOException e){
            Log.e(TAG, "opening /proc/version failed: " + e.toString());
        }

        try { // read and parse cpu info
            BufferedReader procVersion = new BufferedReader(new FileReader("/proc/cpuinfo") );
            StringBuffer sbuff = new StringBuffer();
            String tmp;
            while ((tmp = procVersion.readLine()) != null)
                sbuff.append(tmp + "\n");
            procVersion.close();

            tmp = sbuff.toString();

            sbuff = new StringBuffer();

            Pattern p1 = Pattern.compile("(Processor\\s*:\\s*(.*)\\s*[\n\r]+)");
            Matcher m1 = p1.matcher(tmp);
            if (m1.find()) sbuff.append(m1.group(2));

            Pattern p2 = Pattern.compile("(Hardware\\s*:\\s*(.*)\\s*[\n\r]+)");
            Matcher m2 = p2.matcher(tmp);
            if (m2.find()) sbuff.append(":"+m2.group(2));

            Pattern p3 = Pattern.compile("(Revision\\s*:\\s*(.*)\\s*[\n\r]+)");
            Matcher m3 = p3.matcher(tmp);
            if (m3.find()) sbuff.append(":"+m3.group(2));

            Log.e(TAG, sbuff.toString());
            xml += " cpu=\"" + sbuff.toString() + "\"";
        } catch (IOException e) {
            Log.e(TAG, "opening /proc/version failed: " + e.toString());
        }

        xml += ">";

        Case mycase;
        for (int i = 0; i < mCases.size(); i++) {
            mycase = mCases.get(i);
            xml += mycase.getXMLBenchmark();
        }

        xml += "</result>";
        return xml;
    }

    /*
     * Add Linaro Dashboard Bundle's JSON format support
     * https://launchpad.net/linaro-python-dashboard-bundle/trunk
     */
    public String getJSONResult() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        JSONObject result = new JSONObject();
        try {
            JSONArray testRunsArray = new JSONArray();
            JSONObject testRunsObject = new JSONObject();
            testRunsObject.put("analyzer_assigned_date", sdf.format(date));
            testRunsObject.put("time_check_performed", false);
            // TODO: should be UUID version 1
            testRunsObject.put("analyzer_assigned_uuid", UUID.randomUUID().toString());
            testRunsObject.put("test_id", "0xbench");

            JSONArray testResultsList = new JSONArray();
            Case myCase;
            for (int i = 0; i < mCases.size(); i++) {
                myCase = mCases.get(i);
                JSONArray caseResultList = myCase.getJSONBenchmark();
                for (int j = 0; j < caseResultList.length(); j++) {
                    testResultsList.put(caseResultList.get(j));
                }
            }
            testRunsObject.put("test_results", testResultsList);

            testRunsArray.put(testRunsObject);
            result.put("test_runs", testRunsArray);
            result.put("format", "Dashboard Bundle Format 1.2");
        }
        catch (JSONException jsonE) {
            jsonE.printStackTrace();
        }
        return result.toString();
    }

    public String getResult() {
        String result = "";
        Case mycase;
        for (int i = 0; i < mCases.size(); i++) {
            mycase = mCases.get(i);
            if ( !mycase.couldFetchReport() ) continue;
            result += "============================================================\n";
            result += mycase.getTitle() + "\n";
            result += "------------------------------------------------------------\n";
            result += mycase.getResultOutput().trim() + "\n";
        }
        result += "============================================================\n";

        return result;
    }

    /**
     * 当一个 runCase(mCases)创建的Acctivity运行完时，这个函数将被调用，并且会继续下一个
     * 需要执行的Case，因为最后runCase(mCases)被调用了。
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {
            Log.i(TAG, "oooops....Intent is null");
            return;
        }

        Case mycase;
        for (int i = 0; i < mCases.size(); i++) {
            mycase = mCases.get(i);
            if (mycase.realize(data)) {
                mycase.parseIntent(data);
                break;
            }
        }
        runCase(mCases);
    }

    private boolean writeResult(String filename, String output) {
        File writeDir = new File(BenchUtil.getResultDir(this));
        if (!writeDir.exists()) {
            writeDir.mkdirs();
        }

        File file = new File(writeDir, filename);
        if (file.exists()) {
            Log.w(TAG, "File exists, delete " + writeDir.getPath() + filename);
            file.delete();
        }

        try {
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(output.getBytes());
            fos.flush();
        } catch (Exception e) {
            Log.i(TAG, "Write Failed.");
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
