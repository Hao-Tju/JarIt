
package net.jarit;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.google.android.gms.internal.it;
import com.google.android.gms.maps.model.LatLng;
import com.koushikdutta.ion.Ion;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class TreasureItemListAdapter extends ArrayAdapter<TreasureItem> {
    private final Context context;
    private final List<TreasureItem> items;

    public TreasureItemListAdapter(Context context, List<TreasureItem> objects) {
        super(context, R.layout.treasure_item, objects);
        this.context = context;
        this.items = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.treasure_item, parent, false);
        }

        TextView contentView = (TextView) convertView.findViewById(R.id.content_text);
        TextView locationView = (TextView) convertView.findViewById(R.id.location_text);
        TextView timeView = (TextView) convertView.findViewById(R.id.time_text);
        ImageView staticMapView = (ImageView) convertView.findViewById(R.id.map_image);

        TreasureItem item = getItem(position);
        contentView.setText(item.getContent());
        locationView.setText("@" + item.getLon() + ", " + item.getLat());
        SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        SimpleDateFormat outFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        try {
            date = inFormat.parse(item.getTime());
        } catch (ParseException e) {
        }
        timeView.setText(outFormat.format(date));

        Ion.with(staticMapView)
                .placeholder(R.drawable.map_image_placeholder)
                .load(Constant.getMapImage(new LatLng(item.getLat(), item.getLon())));

        return convertView;
    }

}
