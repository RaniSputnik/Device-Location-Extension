package ${YYAndroidPackageName};

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class GMGeolocation {

	private Boolean watching = false;
	private LocationManager locationManager;;
	private LocationListener locationListener;
	private Location currentLocation = null;

	/** Called when the extension first starts **/
	public void GMGeolocation_Init() {
		RunnerActivity.ViewHandler.post( new Runnable() {
			public void run() {
				// Acquire a reference to the system Location Manager
				locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
				// Try and restore the current location from a cached value
				provideLocation(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
				provideLocation(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
		}});
	}

	/** Called when the extension is finished **/
	public void GMGeolocation_Final() {
		GMGeolocation_WatchEnd();
	}
	
	/** Begin watching location **/
	public double GMGeolocation_WatchStart() {
		if (watching) return 0;
		Log.i("yoyo","Starting location watch");
		RunnerActivity.ViewHandler.post( new Runnable() {
			public void run() {
				// Define a listener that responds to location updates
				locationListener = new LocationListener() {
					public void onLocationChanged(Location location) {
						// Called when a new location is found by the network location provider.
						provideLocation(location);
					}
					public void onStatusChanged(String provider, int status, Bundle extras) {}
					public void onProviderEnabled(String provider) {}
					public void onProviderDisabled(String provider) {}
				};

				// Register the listener with the Location Manager to receive location updates
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
				watching = true;
		}});
		return 1;
	}
	
	/** End watching location **/
	public double GMGeolocation_WatchEnd() {
		if (!watching) return 0;
		Log.i("yoyo","Ending location watch");
		RunnerActivity.ViewHandler.post( new Runnable() {
			public void run() {
				locationManager.removeUpdates(locationListener);
				watching = false;
		}});
		return 1;
	}
	
	/** Get whether or not we are watching location **/
	public double GMGeolocation_GetWatching() {
		if (watching) return 1;
		else return 0;
	}
	
	/** Get the current latutude **/
	public double GMGeolocation_GetLat() {
		if (currentLocation == null) return 0;
		else return currentLocation.getLatitude();
	}
	
	/** Get the current longitude **/
	public double GMGeolocation_GetLong() {
		if (currentLocation == null) return 0;
		else return currentLocation.getLongitude();
	}
	
	/** Get the bearing of the current location in degrees **/
	public double GMGeolocation_GetBearing() {
		if (currentLocation == null) return 0;
		else if (!currentLocation.hasBearing()) return 0;
		else return currentLocation.getBearing();
	}
	
	/** Get the altitude of the current location in meters above sea level */
	public double GMGeolocation_GetAltitude() {
		if (currentLocation == null) return 0;
		else if (!currentLocation.hasAltitude()) return 0;
		else return currentLocation.getAltitude();
	}
	
	/** Get the accuracy of the current location in meters **/
	public double GMGeolocation_GetAccuracy() {
		if (currentLocation == null) return 0;
		else if (!currentLocation.hasAccuracy()) return 0;
		else return currentLocation.getAccuracy();
	}
	
	// Prviate methods
	
	/** Try to determine if a new location is better than the current locations **/
	public void provideLocation(Location location) {
		if (location != null) {
			if (isBetterLocation(location,currentLocation)) {
				Log.i("yoyo","Location updated: "+location.toString());
				currentLocation = location;
				// TODO dispatch async event to inform Game Maker that there was a geo update
			}
		}
	}
	
	private static final int TWO_MINUTES = 1000 * 60 * 2;

	/** Determines whether one Location reading is better than the current Location fix
	  * @param location  The new Location that you want to evaluate
	  * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	  */
	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
		// If the new location is more than two minutes older, it must be worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
		  return provider2 == null;
		}
		return provider1.equals(provider2);
	}
	
	// Prviate helpers
	
	private Context getContext() {
		return RunnerActivity.CurrentActivity;
	}

}