package com.ByteKnights.com.resturarent_system.util;

public class DistanceUtil {

    private static final int EARTH_RADIUS_KM = 6371;

    /**
     * Calculates the distance between two points (in decimal degrees) using the Haversine formula.
     *
     * @param startLat Starting point latitude
     * @param startLong Starting point longitude
     * @param endLat Destination point latitude
     * @param endLong Destination point longitude
     * @return Distance in kilometers
     */
    public static double calculateDistance(double startLat, double startLong, double endLat, double endLong) {
        double dLat = Math.toRadians(endLat - startLat);
        double dLong = Math.toRadians(endLong - startLong);
        
        startLat = Math.toRadians(startLat);
        endLat = Math.toRadians(endLat);
        
        double a = Math.pow(Math.sin(dLat / 2), 2) +
                   Math.pow(Math.sin(dLong / 2), 2) * Math.cos(startLat) * Math.cos(endLat);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c;
    }
}
