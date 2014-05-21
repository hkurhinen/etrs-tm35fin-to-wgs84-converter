package fi.elemmings.converter;

public class Coordinates {
	
	public Coordinates(double lng, double lat){
		this.lng = lng;
		this.lat = lat;
	}
	
	public double getLng() {
		return lng;
	}
	public void setLng(double lng) {
		this.lng = lng;
	}
	public double getLat() {
		return lat;
	}
	public void setLat(double lat) {
		this.lat = lat;
	}
	
	private double lng;
	private double lat;
	
}
