package com.bottle.wvapp.activitys

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
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
import androidx.recyclerview.widget.RecyclerView
import com.bottle.wvapp.R
import com.bottle.wvapp.jsprovider.NativeServerImp.areaJson
import com.bottle.wvapp.tool.GaoDeMapUtil
import com.leezp.lib.recycles.BaseViewHolderDataModel
import com.leezp.lib.recycles.RecyclerUtil
import com.leezp.lib.recycles.more_view_adapter.AutomaticViewBaseViewHolder
import com.leezp.lib.recycles.more_view_adapter.ItemViewTemplateAttribute
import com.leezp.lib.recycles.more_view_adapter.ItemViewTemplateManage
import com.leezp.lib.recycles.more_view_adapter.MultiTypeAdapter
import kotlinx.android.synthetic.main.activity_city_select.*
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
        /*LLog.print("getview - " +   mList.get(position))*/
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
 * recycle view item 模板
 */
public class RecycleViewItemTemp(): ItemViewTemplateManage(){
    override fun initItemLayout() {
        addAttr(ItemViewTemplateAttribute(RecycleViewItemHolder::class.java, R.layout.listitem_text))
        addAttr(ItemViewTemplateAttribute(RecycleViewItemHolder::class.java, R.layout.listitem_text_grid))
        addAttr(ItemViewTemplateAttribute(RecycleViewItemHolder::class.java, R.layout.listitem_arealist))
    }
}

/**
 * recycle view data model
 */
public class RecycleViewDataModel(val item : AreaDataItem,var citySelectActivity: CitySelectActivity? = null): BaseViewHolderDataModel {
    override fun <DATA : Any?> convert(): DATA {
        return item as DATA
    }

    override fun getViewTemplateType(): Int {
        return when(item.type){
            -1 -> R.layout.listitem_arealist
            2 -> R.layout.listitem_text_grid
            else -> R.layout.listitem_text
        }
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
     * 关联data list 样式显示
     */
    override fun bindData(dataModel: RecycleViewDataModel?) {
        val data = dataModel?.convert<AreaDataItem>()
        if (data!!.type<0){
            val tv = itemView.findViewById<TextView>(R.id.list_item_area_text)
            val rv = itemView.findViewById<RecyclerView>(R.id.list_item_area_recycler)
            val citySelectActivity = dataModel.citySelectActivity ?: return
            tv.text =  data.letter
            citySelectActivity.setAreaRecycleView(rv)
        }else{
            val tv = itemView as TextView
            tv.text = when (data.type){
                0 -> {
                    tv.setBackgroundColor(Color.WHITE)
                    tv.setTextColor(Color.BLACK)
                    data.letter
                }else -> {
                    tv.setBackgroundColor(Color.WHITE)
                    tv.setTextColor(Color.GRAY)
                    data.label
                }
            }
        }
    }

}

public class CitySelectActivity  : AppCompatActivity(){
    companion object CONST{
        val REQUEST_SELECT_AREA_CODE = 100;
        val AREA_CODE = "areacode"
    }

    private var cityListAdapter: MultiTypeAdapter? = null
    //当前选中城市
    private var curItem:AreaDataItem? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_city_select)

        //返回按钮
        iv_title_back.setOnClickListener {
            curItem = null
            returnResult();
        }

        tv_auto_reset.setOnClickListener {
            ipLoc()
            AppUtils.toast(CitySelectActivity@this,"已重新定位")
        }

        //初始化城市列表
        initCityRecycleView()

        //异步加载城市数据
        asyncCityData()

    }

    private fun ipLoc() {
        IOUtils.run {
            val bean = GaoDeMapUtil.ipConvertAddress();
            if(bean!=null && bean.city!=null){
                runOnUiThread {
                    tv_auto_city.text = bean.city
                    auto_complete.setText(bean.city)
                }
                //搜索
                for(index in 0 until cityListAdapter?.dataList?.size!!){
                    val item = cityListAdapter?.dataList!![index].convert<AreaDataItem>()
                    //判断类型是否是字母
                    if (item?.type!! == 1 && item.label == bean.city){
                        curItem = item
                        asyncAreaData()
                        break
                    }
                }
            }
        }

    }

    private fun returnResult() {
        //返回按钮并且回传结果
        val intent = Intent();
        val result =  if (curItem != null && curItem?.value!! > 0) curItem?.value else 0
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
            for(index in 0 until cityListAdapter?.dataList?.size!!){
                val item = cityListAdapter?.dataList!![index].convert<AreaDataItem>()
                //判断类型是否是字母
                if (item?.type!! == 0 && item.letter == letter){
                    recycler_city.scrollToPosition(index);
                    val mLayoutManager = recycler_city.getLayoutManager()
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
            curItem = adapter.getItem(position);
            auto_complete.setText(curItem?.label);
//            LLog.print("自动匹配城市:"+curItem)
            recycler_city.smoothScrollToPosition(0)
            val mLayoutManager = recycler_city.getLayoutManager()
            if ( mLayoutManager is LinearLayoutManager){
                mLayoutManager.scrollToPositionWithOffset(0, 0);
            }
            asyncAreaData()
        }
    }

    private fun initCityRecycleView() {
        cityListAdapter = MultiTypeAdapter(this, RecycleViewItemTemp())
        RecyclerUtil.gridLayoutManagerSettingVerSpan1Norev(this, recycler_city)
        RecyclerUtil.associationAdapter(recycler_city, cityListAdapter)
        RecyclerUtil.setItemAnimator(recycler_city, DefaultItemAnimator())
        cityListAdapter?.setItemClickListener { vh, data, position ->
            curItem = data!!.convert<AreaDataItem>()
            if (curItem?.type!! > 0){
                auto_complete.setText(curItem?.label);
//                LLog.print("城市列表选择: "+ curItem)
                recycler_city.smoothScrollToPosition(0)
                val mLayoutManager = recycler_city.getLayoutManager()
                if ( mLayoutManager is LinearLayoutManager){
                    mLayoutManager.scrollToPositionWithOffset(0, 0);
                }
                asyncAreaData()
            }
        }
    }

    private fun getDataSource(areaCode:Long): MutableList<AreaDataItem> {
        val json = areaJson(areaCode);
        return GsonUtils.json2List(json,AreaDataItem::class.java)
    }

    //异步加载数据
    private fun asyncCityData() {
        IOUtils.run {
            val dataList = getDataSource(0)
            dataList.add(AreaDataItem("#","",0,-1)) // 区域显示区占位
            //添加字母
            val A = 'A'
            for (i in 0 until 26){
                dataList.add(AreaDataItem((A + i).toString()))
            }
            //对数据进行排序
            dataList.sortWith(Comparator sort@{ o1, o2 ->
                return@sort o1.letter.compareTo(o2.letter)
            })
            dataList.forEach {
                cityListAdapter?.addData(RecycleViewDataModel(it,this))
            }
            runOnUiThread {
                cityListAdapter?.notifyDataSetChanged()
                initAutoComplete(dataList)
            }
            //IP定位
            ipLoc()
        }
    }


    private var areaAdapter:MultiTypeAdapter? = null;

    fun setAreaRecycleView(rv: RecyclerView) {
        if (areaAdapter == null){
            areaAdapter =  MultiTypeAdapter(citySelectActivity@this, RecycleViewItemTemp());
            //2.设置关联
            RecyclerUtil.gridLayoutManagerSettingHorSpanSpcNorev(citySelectActivity@this, rv,3)
            RecyclerUtil.associationAdapter(rv, areaAdapter)
            RecyclerUtil.setItemAnimator(rv, DefaultItemAnimator())
            areaAdapter!!.setItemClickListener{ vh, data, position ->
                val curArea = data!!.convert<AreaDataItem>()
//                LLog.print("区域列表选择: "+ curArea)
                curItem = curArea
                returnResult()
            }
        }
    }

    fun asyncAreaData(){
        if (curItem == null || curItem!!.type <= 0) return
        IOUtils.run {
            areaAdapter!!.clearAll()
            val dataList = getDataSource(curItem!!.value)
            dataList.forEach {
                areaAdapter!!.addData(RecycleViewDataModel(it,this))
            }
            runOnUiThread {
                areaAdapter!!.notifyDataSetChanged()
            }
        }
    }



}