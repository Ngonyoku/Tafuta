package ke.co.kbanda.tafuta.adapters;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;

import ke.co.kbanda.tafuta.R;
import ke.co.kbanda.tafuta.models.LocationHistory;

public class LocationHistoryRecyclerViewAdapter extends RecyclerView.Adapter<LocationHistoryRecyclerViewAdapter.LocationHistoryViewHolder> {
    private static final String TAG = "LocationHistoryRecycler";
    private Context context;
    private List<LocationHistory> locationHistories;

    public LocationHistoryRecyclerViewAdapter(Context context, List<LocationHistory> locationHistories) {
        this.context = context;
        this.locationHistories = locationHistories;
    }

    @NonNull
    @Override
    public LocationHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new LocationHistoryViewHolder(
                LayoutInflater
                        .from(context)
                        .inflate(R.layout.item_tracking_history, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull LocationHistoryViewHolder holder, int position) {
        LocationHistory history = locationHistories.get(position);
        String coordinates = history.getLatitude() + ", " + history.getLongitude();
        holder.latLng.setText(coordinates);
        holder.time.setText(String.valueOf(getTimestamp(history)));
        Double lat = Double.parseDouble(history.getLatitude());
        Double lng = Double.parseDouble(history.getLongitude());

        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            Address locationAddress = addresses.get(0);
            String locationInfo = locationAddress.getCountryName();
            locationInfo = locationInfo + "\n" + locationAddress.getAdminArea();
            locationInfo = locationInfo + "\n" + locationAddress.getLocality();

            holder.place.setText(locationInfo);
            Log.d(TAG, "onBindViewHolder: Location Information -> " + locationInfo);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    @NonNull
    private Timestamp getTimestamp(LocationHistory history) {
        //Time
        final long time = Long.parseLong(history.getTimeInMillis());
        final Timestamp timestamp = new Timestamp(time);
        return timestamp;
    }

    @Override
    public int getItemCount() {
        return locationHistories.size();
    }

    class LocationHistoryViewHolder extends RecyclerView.ViewHolder {
        public TextView time;
        public TextView latLng;
        public TextView place;

        public LocationHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            time = itemView.findViewById(R.id.time);
            latLng = itemView.findViewById(R.id.latlng);
            place = itemView.findViewById(R.id.place);
        }
    }
}
