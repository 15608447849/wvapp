package com.bottle.wvapp.activitys;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.bottle.wvapp.beans.UrlDownloadDialog;
import com.bottle.wvapp.jsprovider.NativeActivityInterfaceDefault;
import com.bottle.wvapp.jsprovider.NativeJSInterface;
import com.bottle.wvapp.jsprovider.NativeServerImp;
import com.syd.oden.circleprogressdialog.core.CircleProgressDialog;

import java.io.File;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.os.BaseActivity;
import lee.bottle.lib.toolset.util.FileOpenUtils;
import lee.bottle.lib.webh5.SysWebView;
import lee.bottle.lib.webh5.interfaces.WebProgressI;

import static lee.bottle.lib.toolset.util.FileOpenUtils.openASpecificFile;


public class WebActivity extends BaseActivity {

    // 连接业务服务器及本地方法实现
    private final NativeServerImp nativeServerImp = new NativeServerImp();

    //底层图层
    protected FrameLayout frameLayout;
    // 浏览器
    protected SysWebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LLog.print(this +" ** onCreate " );
        super.onCreate(savedInstanceState);

        frameLayout = new FrameLayout(this);
        frameLayout.setLayoutParams(new ViewGroup.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT)
        );

        setContentView(frameLayout);


        final CircleProgressDialog circleProgressDialog = new CircleProgressDialog(this);

        circleProgressDialog.showDialog();


        webView = new SysWebView(this);

        // web下载事件
        webView.setDownloadListener(new UrlDownloadDialog(this){

            @Override
            protected void openDirectly(String url) {
                super.openDirectly(url);
                finish();
            }

            @Override
            protected void downloadNow(String url, File file) {
                circleProgressDialog.showDialog();
                super.downloadNow(url, file);
            }

            @Override
            protected void downloadComplete(File storeFile) {
                super.downloadComplete(storeFile);
                circleProgressDialog.dismiss();

                openASpecificFile(WebActivity.this,storeFile);
                finish();
            }

            @Override
            protected void cancelAction(String url) {
                finish();
            }
        });

        // 关联
        nativeServerImp.setNativeActivityInterface(  new NativeActivityInterfaceDefault(this,webView,
                new NativeJSInterface(webView.jsInterface,nativeServerImp)){
            @Override
            public void onJSPageInitialization() {
                // 加载完成通知
                circleProgressDialog.dismiss();
            }
        });

        webView.webProgressI = new WebProgressI() {
            @Override
            public void updateProgress(String url, int current, boolean isManual) {
                if (current>=100) {
                    circleProgressDialog.dismiss();
                }
            }
        };

        webView.bind(this,frameLayout);

        String indexURL = null;
        Intent intent = getIntent();
        if (intent != null){
            String url =  intent.getStringExtra("url");
            if (url != null){
                indexURL = url;
            }
        }

        if (indexURL == null){
            finish();
            return;
        }

        webView.open(indexURL);
    }

    public static void openASpecificFolderInFileManager(Context context, File file) {
//        Intent i = new Intent(Intent.ACTION_VIEW);
//        i.setDataAndType(Uri.parse(path), "resource/folder");
//        context.startActivity(Intent.createChooser(i, "Open with"));


//        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.setDataAndType(Uri.parse(path), "file/*");
//        context.startActivity(intent);


//        Uri uri = Uri.parse(path);
//        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.setType("*/*");
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
//        }
//        context.startActivity(intent);


//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        intent.setDataAndType(Uri.parse(Environment.DIRECTORY_DOCUMENTS ), DocumentsContract.Document.MIME_TYPE_DIR);
//        context.startActivity(intent);

//        file = file.getParentFile();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);

        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".fileProvider", file);
        } else {
            uri = Uri.fromFile(file);
        }
        LLog.print(" uri = "+ uri);

        intent.setDataAndType(uri, "file/*");
//        intent.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        context.startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        webView.onActivityResultHandle(requestCode,resultCode,data);
        super.onActivityResult(requestCode,resultCode,data);
    }

    @Override
    protected void onResume() {
        LLog.print(this +" ** onResume " );
        super.onResume();

    }

    @Override
    protected void onStart() {
        LLog.print(this +" ** onStart " );
        super.onStart();
    }

    @Override
    protected void onRestart() {
        LLog.print(this +" ** onRestart " );
        super.onRestart();
    }


    @Override
    protected void onDestroy() {
        LLog.print(this +" ** onDestroy " );
        if (webView != null){
            webView.close(true,true);
            webView = null;
        }

        super.onDestroy();
    }

    // 捕获返回键 处理
    @Override
    public void onBackPressed() {
        Intent intent = getIntent();
        boolean isRollback = intent.getBooleanExtra("isRollback", true);
        if (isRollback && webView.onBackPressed()) return;
        cur_back_time = 0;
        isExitApplication = false;
        super.onBackPressed();
    }


}