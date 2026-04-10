package com.agentbanking.common.geofence;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Geofencing utility for checking if a point is within a radius of another point.
 * Uses Haversine formula for distance calculation.
 */
public class GeofenceChecker {

    private static final double EARTH_RADIUS_METERS = 6371000.0;
    private static final double MAX_DISTANCE_METERS = 100.0;

    /**
     * Checks if the GPS coordinates are within the allowed radius of the merchant's registered GPS.
     * 
     * @param merchantLat Merchant registered latitude
     * @param merchantLng Merchant registered longitude
     * @param deviceLat Device current latitude
     * @param deviceLng Device current longitude
     * @param maxDistanceMeters Maximum allowed distance in meters (default 100m)
     * @return true if within geofence, false otherwise
     */
    public static boolean isWithinGeofence(BigDecimal merchantLat, BigDecimal merchantLng,
                                            BigDecimal deviceLat, BigDecimal deviceLng,
                                            double maxDistanceMeters) {
        if (merchantLat == null || merchantLng == null || deviceLat == null || deviceLng == null) {
            return false;
        }
        double distance = calculateDistance(
            merchantLat.doubleValue(), merchantLng.doubleValue(),
            deviceLat.doubleValue(), deviceLng.doubleValue()
        );
        return distance <= maxDistanceMeters;
    }

    /**
     * Checks if the GPS coordinates are within the default 100m radius.
     */
    public static boolean isWithinGeofence(BigDecimal merchantLat, BigDecimal merchantLng,
                                            BigDecimal deviceLat, BigDecimal deviceLng) {
        return isWithinGeofence(merchantLat, merchantLng, deviceLat, deviceLng, MAX_DISTANCE_METERS);
    }

    /**
     * Calculates the distance between two GPS coordinates using Haversine formula.
     * 
     * @return Distance in meters
     */
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLng / 2) * Math.sin(dLng / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_METERS * c;
    }
}
