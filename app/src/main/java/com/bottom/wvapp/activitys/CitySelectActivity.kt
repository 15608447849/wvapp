package com.bottom.wvapp.activitys

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.bottom.wvapp.R
import com.bottom.wvapp.jsprovider.NativeServerImp.areaJson
import com.leezp.lib.recycles.BaseViewHolderDataModel
import com.leezp.lib.recycles.RecyclerUtil
import com.leezp.lib.recycles.more_view_adapter.AutomaticViewBaseViewHolder
import com.leezp.lib.recycles.more_view_adapter.ItemViewTemplateAttribute
import com.leezp.lib.recycles.more_view_adapter.ItemViewTemplateManage
import com.leezp.lib.recycles.more_view_adapter.MultiTypeAdapter
import kotlinx.android.synthetic.main.activity_city_select.*
import lee.bottle.lib.toolset.log.LLog
import lee.bottle.lib.toolset.threadpool.IOUtils
import lee.bottle.lib.toolset.util.AppUtils
import lee.bottle.lib.toolset.util.GsonUtils


/** 数据item*/
public data class AreaDataItem(val letter:String="",val label:String = "", val value:Long=0,val type:Int=0)

/**
 * 自动补全适配器
 */
private class AutoCompAdapter(val context : Context, val dataSource:MutableList<AreaDataItem>?): BaseAdapter(), Filterable {
    private var mList = ArrayList<AreaDataItem>()
    override fun getItem(position: Int): AreaDataItem {
        return mList[position];
    }
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
    override fun getCount(): Int {
        return mList.size
    }
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val tv:TextView = if (convertView == null){
            View.inflate(context, R.layout.listitem_text,null) as TextView
        }else{
            convertView as TextView
        }
        tv.text = if (mList.get(position).type == 0){
            mList.get(position).letter
        }else{
            mList.get(position).label
        }
        LLog.print("getview - " +   mList.get(position))
        return tv
    }
     //搜索过滤
    override fun getFilter(): Filter {
       return object:Filter(){
           override fun performFiltering(prefix: CharSequence?): FilterResults? {
//               Log.w("过滤","performFiltering , prefix = "+prefix)
                mList.clear()
               if (prefix!=null && prefix.isNotEmpty()){
                   dataSource?.forEach {
                       if (it.type > 0 &&  it.label.contains(prefix!!) || it.letter.contains(prefix.toString().toUpperCase())) mList.add(it)
                   }
               }
               return null
           }

           override fun publishResults(constraint: CharSequence?, results: Filter.FilterResults?) {
//               Log.w("过滤","publishResults , constraint = "+constraint+" , results = "+ results)
               if (mList.size > 0) {
                   notifyDataSetChanged();
               } else {
                   notifyDataSetInvalidated();
               }
           }
       }
    }

}


/**
 * recycle view data model
 */
public class RecycleViewDataModel(val item : AreaDataItem): BaseViewHolderDataModel {
    override fun <DATA : Any?> convert(): DATA {
        return item as DATA
    }

    override fun getViewTemplateType(): Int {
        return R.layout.listitem_text
    }

}

/**
 * recycle view holder
 */
public class RecycleViewItemHolder(itemView:View): AutomaticViewBaseViewHolder<RecycleViewDataModel>(itemView) {

    /**
     * 关联view
     */
    override fun automaticView(rootView: View?) {}

    /**
     * 关联data listTime 样式显示
     */
    override fun bindData(dataModel: RecycleViewDataModel?) {
        if (itemView is TextView){
            val data = dataModel?.convert<AreaDataItem>()
            itemView.text = when (data?.type){
                0 -> {
                    itemView.setBackgroundColor(Color.WHITE)
                    itemView.setTextColor(Color.BLACK)
                    data.letter
                }else -> {
                    itemView.setBackgroundColor(Color.WHITE)
                    itemView.setTextColor(Color.GRAY)
                    data!!.label
                }
            }
        }
    }
}

/**
 * recycle view item 模板
 */
public class RecycleViewItemTemp: ItemViewTemplateManage(){
    override fun initItemLayout() {
        addAttr(ItemViewTemplateAttribute(RecycleViewItemHolder::class.java, R.layout.listitem_text))
    }
}

public class CitySelectActivity  : AppCompatActivity(){

    var adapter: MultiTypeAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_city_select)

        iv_title_back.setOnClickListener {
                returnResult();
        }

        initRecycleView()

        asyncArea()
    }

    private fun returnResult() {
        //返回按钮并且回传结果
        val intent = Intent();
        val result =  if (curerntItem != null && curerntItem?.value!! > 0) curerntItem?.value else 0
        LLog.print("返回数据: "+ result)
        //把返回数据存入Intent
        intent.putExtra(AREA_CODE,result);
        //设置返回数据
        setResult(RESULT_OK, intent);

        finish()
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(keyCode== KeyEvent.KEYCODE_BACK) return true;//不执行父类点击事件
        return super.onKeyDown(keyCode, event);//继续执行父类其他点击事件
    }

    private fun initAutoComplete(list:MutableList<AreaDataItem>?) {

        //侧边栏事件监听
        wave_side_bar.setOnTouchLetterChangeListener { letter ->
            for(index in 0 until adapter?.dataList?.size!!){
                val item = adapter?.dataList!![index].convert<AreaDataItem>()
                //判断类型是否是字母
                if (item?.type!! == 0 && item.letter == letter){
                    recycler.scrollToPosition(index);
                    val mLayoutManager = recycler.getLayoutManager()
                    if ( mLayoutManager is LinearLayoutManager){
                        mLayoutManager.scrollToPositionWithOffset(index, 0);
                    }
                    break
                }
            }
        }

        //自动适配器
        val adapter = AutoCompAdapter(this,list)

        //将适配器与当前控件绑定
        auto_complete.setAdapter(adapter)

        //自动匹配出来的数据
        auto_complete.setOnItemClickListener { parent, view, position, id ->
            auto_complete.clearFocus()
            AppUtils.hideSoftInputFromWindow(CitySelectActivity@this)
            curerntItem = adapter.getItem(position);
            auto_complete.setText(curerntItem?.label);
            LLog.print("自动匹配:"+curerntItem)
            checkTo2page();
        }
    }

    private fun initRecycleView() {
        adapter = MultiTypeAdapter(this, RecycleViewItemTemp())
        RecyclerUtil.gridLayoutManagerSettingVerSpan1Norev(this, recycler)
        RecyclerUtil.associationAdapter(recycler, adapter)
        RecyclerUtil.setItemAnimator(recycler, DefaultItemAnimator())
        adapter?.setItemClickListener { vh, data, position ->
            curerntItem = data!!.convert<AreaDataItem>()
            if (curerntItem?.type == 0) return@setItemClickListener
            auto_complete.setText(curerntItem?.label);
            LLog.print("列表选择: "+ curerntItem)
            checkTo2page();
        }
    }


    private fun checkTo2page() {
        if (isOpen2Page && curerntItem!=null && curerntItem!!.value>0){
            val intent = Intent(this@CitySelectActivity, CitySelectActivity::class.java)
            intent.putExtra(AREA_CODE,curerntItem!!.value)
            startActivityForResult(intent, REQUEST_SELECT_AREA_CODE)
        }
    }


    //当前选中
    var curerntItem:AreaDataItem? = null;

    private fun getDataSource(areaCode:Long): MutableList<AreaDataItem> {
        val json = areaJson(areaCode);
        return GsonUtils.json2List(json,AreaDataItem::class.java)
    }

    companion object CONST{
        val REQUEST_SELECT_AREA_CODE = 100;
        val AREA_CODE = "areacode"
    }

    private var isOpen2Page: Boolean = false

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_SELECT_AREA_CODE) {
                val areacode = data!!.getLongExtra(AREA_CODE, 0)
                if(areacode > 0){
                    LLog.print("二级页面传递过来的数据" + areacode)
                    curerntItem = AreaDataItem(value = areacode)
                    returnResult()
                }
            }
        }
    }

    //异步加载数据
    private fun asyncArea() {
        IOUtils.run {
            val areaCode = intent.getLongExtra(AREA_CODE,0)
            if (areaCode == 0L) isOpen2Page = true;
            LLog.print("初始化_是否打开二级页面:$isOpen2Page")
            val dataList = getDataSource(areaCode)
            addLetter(dataList)
            //对数据进行排序
            dataList!!.sortWith(Comparator sort@{ o1, o2 ->
                return@sort o1.letter.compareTo(o2.letter)
            })
            dataList.forEach {
                Log.d("加载数据"," adp = ${adapter} - $it")
                adapter?.addData(RecycleViewDataModel(it))
            }
            runOnUiThread {
                adapter?.notifyDataSetChanged()
                initAutoComplete(dataList)
            }
        }
    }


    private fun addLetter(dataList: MutableList<AreaDataItem>?) {
        val A = 'A'
        for (i in 0 until 26){
            dataList?.add(AreaDataItem((A + i).toString()))
        }
    }

}