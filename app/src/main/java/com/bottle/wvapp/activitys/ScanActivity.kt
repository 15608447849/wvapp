package com.bottle.wvapp.activitys

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import cn.bingoogolapple.qrcode.core.QRCodeView
import com.bottle.wvapp.R
import kotlinx.android.synthetic.main.activity_scan.*
import lee.bottle.lib.toolset.log.LLog
import lee.bottle.lib.toolset.util.DialogUtil

/**
 * Created by Leeping on 2019/6/27.
 * email: 793065165@qq.com
 */
public class ScanActivity : AppCompatActivity() , QRCodeView.Delegate, CompoundButton.OnCheckedChangeListener{
    companion object CONST{
        val SCAN_RESULT_CODE = 101;
        val SCAN_RES = "scanres"
    }

    var res:String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_scan)

        iv_title_back.setOnClickListener {
            backUp();
        }
        zbar_view.setDelegate(this)

        tb_flash.setOnCheckedChangeListener(this)

    }

    fun backUp(){
        //返回按钮并且回传结果
        val intent = Intent();
        //把返回数据存入Intent
        intent.putExtra(SCAN_RES,res);
        //设置返回数据
        setResult(RESULT_OK, intent);
        finish()
    }

    override fun onStart() {
        super.onStart()
        zbar_view.startCamera()//打开摄像头,并未识别
        zbar_view.startSpotAndShowRect(); // 显示扫描框，并开始识别
    }

    override fun onPause() {
        zbar_view.stopSpot();
        super.onPause()
    }

    override fun onStop() {
        zbar_view.stopCamera()//关闭摄像头预览，并且隐藏扫描框
        tb_flash.setChecked(false)
        super.onStop()
    }

    override fun onDestroy() {
        zbar_view.closeFlashlight() //关灯
        zbar_view.onDestroy()// 销毁二维码扫描控件
        super.onDestroy()
    }

    /**
     * 处理扫描结果
     *
     * @param result 摄像头扫码时只要回调了该方法 result 就一定有值，不会为 null。
     * 解析本地图片或 Bitmap 时 result 可能为 null
     */
    override fun onScanQRCodeSuccess(result: String) {
        LLog.print("扫描结果: "+ result)
        res = result
        backUp()
    }

    /**
     * 处理打开相机出错
     */
    override fun onScanQRCodeOpenCameraError() {
        DialogUtil.dialogSimple(this@ScanActivity, "打开相机出错.", "关闭", object : DialogUtil.Action0 {
            override fun onAction0() {
                backUp()
            }
        })
        DialogUtil.changeSystemDialogColor(
                DialogUtil.dialogSimple(this@ScanActivity, "打开相机出错.", "关闭", object : DialogUtil.Action0 {
                    override fun onAction0() {
                        backUp()
                    }
                }), 0, 0, Color.RED, 0, 0)

    }

    /**
     * 摄像头环境亮度发生变化
     *
     * @param isDark 是否变暗
     */
    override fun onCameraAmbientBrightnessChanged(isDark: Boolean) {
        if (isDark) {
            tb_flash.isChecked = true
        }
    }

    //打开闪光灯
    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (isChecked) {
            zbar_view.openFlashlight()
        } else {
            zbar_view.closeFlashlight()
        }
    }

}