package com.jt.updatedownload.quanyan;

import android.animation.AnimatorSet;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.gtdev5.geetolsdk.mylibrary.beans.GetNewBean;
import com.gtdev5.geetolsdk.mylibrary.util.GTDownloadUtils;
import com.gtdev5.geetolsdk.mylibrary.util.LogUtils;
import com.gtdev5.geetolsdk.mylibrary.util.ToastUtils;
import com.gtdev5.geetolsdk.mylibrary.util.Utils;
import com.gtdev5.geetolsdk.mylibrary.widget.BaseDialog;
import com.gtdev5.geetolsdk.mylibrary.widget.NumberProgressBar;
import com.jt.updatedownload.R;


import java.io.File;

/**
 * Created by zl
 * 2020/05/19
 * 软件下载更新弹框
 */
public class QYDownloadApkDialog extends BaseDialog {
    private TextView mDownLoadText, mUpdateInfoText, mCancelText, mVersionText;
    private NumberProgressBar mNumberProgressBar;
    private LinearLayout llBtn,llProgressbar;
    private GetNewBean mGetNewBean;
    private Context mContext;
    private File file;
    private int currentProgress;
    private String mAuthority;

    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1002:
                    ToastUtils.showShortToast("下载完成");
                    if (QYDownloadApkDialog.this != null) {
                        QYDownloadApkDialog.this.dismiss();
                    }
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.putExtra("name", "");
                        intent.addCategory("android.intent.category.DEFAULT");
                        Uri data;
                        if (Build.VERSION.SDK_INT >= 24) {
                            // 临时允许
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            data = FileProvider.getUriForFile(mContext, mAuthority, file);
                        } else {
                            data = Uri.fromFile(file);
                        }
                        intent.setDataAndType(data, "application/vnd.android.package-archive");
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intent);
                    } catch (Exception e) {
                        LogUtils.e("安装失败", e.toString());
                        if (!TextUtils.isEmpty(mGetNewBean.getDownurl())) {
                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(mGetNewBean.getDownurl()));
                            mContext.startActivity(intent);
                        }
                    }
                    break;
                case 1003:
                    if (mNumberProgressBar != null) {
                        mNumberProgressBar.setProgress(currentProgress);
                        int length = mNumberProgressBar.getWidth()*currentProgress/mNumberProgressBar.getMax();

                    }
                    break;
                case 1004:
                    ToastUtils.showShortToast("下载失败，打开浏览器进行下载更新");
                    if (QYDownloadApkDialog.this != null) {
                        QYDownloadApkDialog.this.dismiss();
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //解除标题导致dialog不居中的印象
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
    }

    public QYDownloadApkDialog(@NonNull Context context, GetNewBean bean, String authority) {
        super(context);
        this.mContext = context;
        this.mGetNewBean = bean;
        this.mAuthority = authority;
        setCancelable(false);
    }

    @Override
    protected float setWidthScale() {
        return 0.9f;
    }

    @Override
    protected AnimatorSet setEnterAnim() {
        return null;
    }

    @Override
    protected AnimatorSet setExitAnim() {
        return null;
    }

    @Override
    protected void init() {
        mCancelText = findViewById(R.id.tv_cancel);
        mDownLoadText = findViewById(R.id.tv_update);
        mUpdateInfoText = findViewById(R.id.tv_update_info);
        mVersionText = findViewById(R.id.tv_version);
        mNumberProgressBar = findViewById(R.id.number_progressBar);
        llBtn = findViewById(R.id.ll_btn);
        llProgressbar = findViewById(R.id.ll_progressbar);
        if (mGetNewBean != null) {
            if (!TextUtils.isEmpty(mGetNewBean.getLog())) {
                mUpdateInfoText.setText(mGetNewBean.getLog());
            } else {
                mUpdateInfoText.setText("多处功能优化，体验更流畅，服务更稳定，马上更新吧！");
            }
            if (!TextUtils.isEmpty(mGetNewBean.getVername())) {
                mVersionText.setVisibility(View.VISIBLE);
                mVersionText.setText(String.format("最新版本：%s",mGetNewBean.getVername()));
            } else {
                mVersionText.setVisibility(View.GONE);
            }
        } else {
            mVersionText.setText("最新版本：3.0.0");
            mUpdateInfoText.setText("多处功能优化，体验更流畅，服务更稳定，马上更新吧！");
        }
        mCancelText.setOnClickListener(v -> {
            if (llBtn.getVisibility() == View.GONE ) {
                ToastUtils.showShortToast("正在下载更新中，无法关闭");
            } else {
                dismiss();
            }
        });
        mDownLoadText.setOnClickListener(v -> updateApp());
    }

    /**
     * 更新应用
     */
    private void updateApp() {
        //检查网络是否可用
        if (Utils.isNetworkAvailable(mContext)) {
            llProgressbar.setVisibility(View.VISIBLE);
            llBtn.setVisibility(View.GONE);
            //下载更新包
            GTDownloadUtils.get().download(mGetNewBean.getDownurl(), "Update", new GTDownloadUtils.OnDownloadListener() {
                @Override
                public void onDownloadSuccess(File files) {
                    //下载成功
                    file = files;
                    handler.sendEmptyMessage(1002);
                }

                @Override
                public void onDownloading(int progress) {
                    //在下载
                    handler.sendEmptyMessage(1003);
                    currentProgress = progress;
                }

                @Override
                public void onDownloadFailed() {
                    //下载失败
                    handler.sendEmptyMessage(1004);
                    if (!TextUtils.isEmpty(mGetNewBean.getDownurl())) {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(mGetNewBean.getDownurl()));
                        mContext.startActivity(intent);
                    }
                }
            });
        } else {
            ToastUtils.showShortToast("当前网络不可用，请检查网络");
        }
    }

    @Override
    public void show() {
        if (mContext != null) {
            Activity activity = (Activity) mContext;
            if (!activity.isFinishing()) {
                super.show();
            }
        }
    }

    @Override
    public void dismiss() {
        if (mContext != null) {
            Activity activity = (Activity) mContext;
            if (!activity.isFinishing()) {
                super.dismiss();
            }
        }
    }

    @Override
    protected int getContentViewId() {
        return R.layout.qy_dialog_download;
    }
}
