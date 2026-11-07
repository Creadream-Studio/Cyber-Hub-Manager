package com.cyberhub.manager;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;

/** MP-Manager 卡片风格文件列表适配器。 */
public final class FileListAdapter extends BaseAdapter {
    private final Context context;
    private final LayoutInflater inflater;
    private JSONArray entries=new JSONArray();
    public final HashSet<String> selected=new HashSet<>();

    public FileListAdapter(Context c){context=c;inflater=LayoutInflater.from(c);}

    public void setEntries(JSONArray arr){entries=arr==null?new JSONArray():arr;selected.retainAll(paths());notifyDataSetChanged();}

    public JSONArray entries(){return entries;}

    public JSONObject entryAt(int position){return entries.optJSONObject(position);}

    /** “..”返回上一级条目：不参与选择、不算文件数。 */
    public static boolean isParentEntry(JSONObject e){return e!=null&&e.optBoolean("parentEntry");}

    public boolean isSelected(int position){JSONObject e=entryAt(position);return !isParentEntry(e)&&selected.contains(e.optString("path"));}

    public void toggleSelect(int position){
        JSONObject e=entryAt(position);
        if(e==null||isParentEntry(e))return;
        String path=e.optString("path");
        if(!selected.remove(path))selected.add(path);
        notifyDataSetChanged();
    }

    public void selectAll(){
        selected.clear();
        for(String p:paths())selected.add(p);
        notifyDataSetChanged();
    }

    public void clearSelection(){selected.clear();notifyDataSetChanged();}

    private HashSet<String> paths(){
        HashSet<String> out=new HashSet<>();
        for(int i=0;i<entries.length();i++){JSONObject e=entries.optJSONObject(i);if(e!=null&&!isParentEntry(e))out.add(e.optString("path"));}
        return out;
    }

    @Override public int getCount(){return entries.length();}

    @Override public long getItemId(int position){return position;}

    @Override public Object getItem(int position){return entryAt(position);}

    @Override public View getView(int position,View convertView,ViewGroup parent){
        View row=convertView;
        if(row==null)row=inflater.inflate(R.layout.item_file,parent,false);
        JSONObject e=entryAt(position);
        if(e==null)return row;
        boolean up=isParentEntry(e);
        String name=e.optString("name");
        boolean dir=e.optBoolean("dir");
        String category=up?"parent":Ui.category(name,dir);
        ImageView icon=row.findViewById(R.id.ivIcon);
        icon.setImageResource(up?R.drawable.ic_up:Ui.iconRes(category));
        icon.setColorFilter(context.getColor(up?R.color.primary:Ui.tintRes(category)));
        TextView tvName=row.findViewById(R.id.tvName);
        tvName.setText(up?"..（返回上一级）":name);
        TextView tvSub=row.findViewById(R.id.tvSub);
        if(up){
            tvSub.setText("返回上一级目录");
        }else if(dir){
            long time=e.optLong("time",0);
            tvSub.setText(time>0?Ui.formatDate(time):"文件夹");
        }else{
            tvSub.setText(Ui.formatSize(e.optLong("size",0))+" · "+Ui.formatDate(e.optLong("time",0)));
        }
        boolean isSelected=!up&&selected.contains(e.optString("path"));
        LinearLayout card=row.findViewById(R.id.rowCard);
        card.setBackgroundResource(isSelected?R.drawable.bg_card_selected:R.drawable.bg_card);
        ImageView mark=row.findViewById(R.id.ivSelected);
        mark.setVisibility(isSelected?View.VISIBLE:View.GONE);
        mark.setColorFilter(Color.WHITE);
        return row;
    }
}
