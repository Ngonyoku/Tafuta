package ke.co.kbanda.tafuta.models.dto;

import java.text.SimpleDateFormat;
import java.util.Date;

import ke.co.kbanda.tafuta.models.LocationHistory;

public class LocationHistoryWithDate {
    private LocationHistory locationHistory;
    private String date;

    public LocationHistoryWithDate(LocationHistory locationHistory) {
        this.locationHistory = locationHistory;
        SimpleDateFormat newDateFormat = new SimpleDateFormat("dd MMM yyyy");
        Date date = new Date(Long.parseLong(locationHistory.getTimeInMillis()));
        this.date = newDateFormat.format(date);
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "LocationHistoryWithDate{" +
                "locationHistory=" + locationHistory +
                ", date=" + date +
                '}';
    }

    public LocationHistory getLocationHistory() {
        return locationHistory;
    }

    public void setLocationHistory(LocationHistory locationHistory) {
        this.locationHistory = locationHistory;
    }
}
