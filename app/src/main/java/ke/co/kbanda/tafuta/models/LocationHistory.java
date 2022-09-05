package ke.co.kbanda.tafuta.models;

public class LocationHistory {
    private String latitude;
    private String longitude;
    private String timeInMillis;

    public LocationHistory(String latitude, String longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public LocationHistory(String latitude, String longitude, String timeInMillis) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timeInMillis = timeInMillis;
    }

    public LocationHistory() {
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getTimeInMillis() {
        return timeInMillis;
    }

    public void setTimeInMillis(String timeInMillis) {
        this.timeInMillis = timeInMillis;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "LocationHistory{" +
                "latitude='" + latitude + '\'' +
                ", longitude='" + longitude + '\'' +
                ", timeInMillis='" + timeInMillis + '\'' +
                '}';
    }
}
