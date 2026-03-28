package com.agentbanking.common.geofence;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class GeofenceCheckerTest {

    @Test
    void isWithinGeofence_withCoordinatesWithin100m_returnsTrue() {
        // Agent location: KLCC
        BigDecimal agentLat = new BigDecimal("3.1578");
        BigDecimal agentLng = new BigDecimal("101.7123");
        // POS within ~10m of KLCC
        BigDecimal posLat = new BigDecimal("3.1579");
        BigDecimal posLng = new BigDecimal("101.7124");

        assertTrue(GeofenceChecker.isWithinGeofence(agentLat, agentLng, posLat, posLng));
    }

    @Test
    void isWithinGeofence_withCoordinatesBeyond100m_returnsFalse() {
        // Agent location: KLCC
        BigDecimal agentLat = new BigDecimal("3.1578");
        BigDecimal agentLng = new BigDecimal("101.7123");
        // POS ~2km away (Merdeka 118 area)
        BigDecimal posLat = new BigDecimal("3.1425");
        BigDecimal posLng = new BigDecimal("101.7001");

        assertFalse(GeofenceChecker.isWithinGeofence(agentLat, agentLng, posLat, posLng));
    }

    @Test
    void isWithinGeofence_withSameCoordinates_returnsTrue() {
        BigDecimal lat = new BigDecimal("3.1578");
        BigDecimal lng = new BigDecimal("101.7123");

        assertTrue(GeofenceChecker.isWithinGeofence(lat, lng, lat, lng));
    }

    @Test
    void isWithinGeofence_withNullPosLat_throwsException() {
        BigDecimal agentLat = new BigDecimal("3.1578");
        BigDecimal agentLng = new BigDecimal("101.7123");

        assertThrows(NullPointerException.class, () ->
                GeofenceChecker.isWithinGeofence(agentLat, agentLng, null, new BigDecimal("101.7123")));
    }

    @Test
    void isWithinGeofence_withNullPosLng_throwsException() {
        BigDecimal agentLat = new BigDecimal("3.1578");
        BigDecimal agentLng = new BigDecimal("101.7123");

        assertThrows(NullPointerException.class, () ->
                GeofenceChecker.isWithinGeofence(agentLat, agentLng, new BigDecimal("3.1578"), null));
    }

    @Test
    void calculateDistance_withKnownDistance_isReasonablyAccurate() {
        // KLCC to Merdeka 118 (~2.8km apart)
        double klccLat = 3.1578;
        double klccLng = 101.7123;
        double merdekaLat = 3.1425;
        double merdekaLng = 101.7001;

        double distance = GeofenceChecker.calculateDistance(klccLat, klccLng, merdekaLat, merdekaLng);

        // Should be approximately 2800m (allow 10% tolerance)
        assertTrue(distance > 2000 && distance < 4000, "Distance should be ~2800m, got " + distance);
    }
}
