package uk.ac.ed.inf;

import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class AppTest {

    private static final String VERSION = "1.0.5";
    private static final String RELEASE_DATE = "September 28, 2021";

    private final LongLat appletonTower = new LongLat(-3.186874, 55.944494);
    private final LongLat businessSchool = new LongLat(-3.1873,55.9430);
    private final LongLat greyfriarsKirkyard = new LongLat(-3.1928,55.9469);

    @Test
    public void testIsConfinedTrueA(){
        assertTrue(appletonTower.isConfined());
    }

    @Test
    public void testIsConfinedTrueB(){
        assertTrue(businessSchool.isConfined());
    }

    @Test
    public void testIsConfinedFalse(){
        assertFalse(greyfriarsKirkyard.isConfined());
    }

    private boolean approxEq(double d1, double d2) {
        return Math.abs(d1 - d2) < 1e-12;
    }

    @Test
    public void testDistanceTo(){
        double calculatedDistance = 0.0015535481968716011;
        assertTrue(approxEq(appletonTower.distanceTo(businessSchool), calculatedDistance));
    }

    @Test
    public void testCloseToTrue(){
        LongLat alsoAppletonTower = new LongLat(-3.186767933982822, 55.94460006601717);
        assertTrue(appletonTower.closeTo(alsoAppletonTower));
    }


    @Test
    public void testCloseToFalse(){
        assertFalse(appletonTower.closeTo(businessSchool));
    }


    private boolean approxEq(LongLat l1, LongLat l2) {
        return approxEq(l1.longitude, l2.longitude) &&
                approxEq(l1.latitude, l2.latitude);
    }

    @Test
    public void testAngle0(){
        LongLat nextPosition = appletonTower.nextPosition(0);
        LongLat calculatedPosition = new LongLat(-3.186724, 55.944494);
        assertTrue(approxEq(nextPosition, calculatedPosition));
    }

    @Test
    public void testAngle20(){
        LongLat nextPosition = appletonTower.nextPosition(20);
        LongLat calculatedPosition = new LongLat(-3.186733046106882, 55.9445453030215);
        assertTrue(approxEq(nextPosition, calculatedPosition));
    }

    @Test
    public void testAngle50(){
        LongLat nextPosition = appletonTower.nextPosition(50);
        LongLat calculatedPosition = new LongLat(-3.186777581858547, 55.94460890666647);
        assertTrue(approxEq(nextPosition, calculatedPosition));
    }

    @Test
    public void testAngle90(){
        LongLat nextPosition = appletonTower.nextPosition(90);
        LongLat calculatedPosition = new LongLat(-3.186874, 55.944644);
        assertTrue(approxEq(nextPosition, calculatedPosition));
    }

    @Test
    public void testAngle140(){
        LongLat nextPosition = appletonTower.nextPosition(140);
        LongLat calculatedPosition = new LongLat(-3.1869889066664676, 55.94459041814145);
        assertTrue(approxEq(nextPosition, calculatedPosition));
    }

    @Test
    public void testAngle190(){
        LongLat nextPosition = appletonTower.nextPosition(190);
        LongLat calculatedPosition = new LongLat(-3.1870217211629517, 55.94446795277335);
        assertTrue(approxEq(nextPosition, calculatedPosition));
    }

    @Test
    public void testAngle260(){
        LongLat nextPosition = appletonTower.nextPosition(260);
        LongLat calculatedPosition = new LongLat(-3.18690004722665, 55.944346278837045);
        assertTrue(approxEq(nextPosition, calculatedPosition));
    }

    @Test
    public void testAngle300(){
        LongLat nextPosition = appletonTower.nextPosition(300);
        LongLat calculatedPosition = new LongLat(-3.186799, 55.94436409618943);
        assertTrue(approxEq(nextPosition, calculatedPosition));
    }

    @Test
    public void testAngle350(){
        LongLat nextPosition = appletonTower.nextPosition(350);
        LongLat calculatedPosition = new LongLat(-3.1867262788370483, 55.94446795277335);
        assertTrue(approxEq(nextPosition, calculatedPosition));
    }

    @Test
    public void testAngle999(){
        // The special junk value -999 means "hover and do not change position"
        LongLat nextPosition = appletonTower.nextPosition(-999);
        assertTrue(approxEq(nextPosition, appletonTower));
    }

    @Test
    public void testMenusOne() {
        // The webserver must be running on port 9898 to run this test.
        Menus menus = new Menus("localhost", "9898");
        int totalCost = menus.getDeliveryCost(
                "Ham and mozzarella Italian roll"
        );
        // Don't forget the standard delivery charge of 50p
        assertEquals(230 + 50, totalCost);
    }

    @Test
    public void testMenusTwo() {
        // The webserver must be running on port 9898 to run this test.
        Menus menus = new Menus("localhost", "9898");
        int totalCost = menus.getDeliveryCost(
                "Ham and mozzarella Italian roll",
                "Salami and Swiss Italian roll"
        );
        // Don't forget the standard delivery charge of 50p
        assertEquals(230 + 230 + 50, totalCost);
    }

    @Test
    public void testMenusThree() {
        // The webserver must be running on port 9898 to run this test.
        Menus menus = new Menus("localhost", "9898");
        int totalCost = menus.getDeliveryCost(
                "Ham and mozzarella Italian roll",
                "Salami and Swiss Italian roll",
                "Flaming tiger latte"
        );
        // Don't forget the standard delivery charge of 50p
        assertEquals(230 + 230 + 460 + 50, totalCost);
    }

    @Test
    public void testMenusFourA() {
        // The webserver must be running on port 9898 to run this test.
        Menus menus = new Menus("localhost", "9898");
        int totalCost = menus.getDeliveryCost(
                "Ham and mozzarella Italian roll",
                "Salami and Swiss Italian roll",
                "Flaming tiger latte",
                "Dirty matcha latte"
        );
        // Don't forget the standard delivery charge of 50p
        assertEquals(230 + 230 + 460 + 460 + 50, totalCost);
    }

    @Test
    public void testMenusFourB() {
        // The webserver must be running on port 9898 to run this test.
        Menus menus = new Menus("localhost", "9898");
        int totalCost = menus.getDeliveryCost(
                "Flaming tiger latte",
                "Dirty matcha latte",
                "Strawberry matcha latte",
                "Fresh taro latte"
        );
        // Don't forget the standard delivery charge of 50p
        assertEquals(4 * 460 + 50, totalCost);
    }

    @Test
    public void testW3WParsing() {
        String words = "pest.round.peanut";
        Location loc = W3W.convertW3W("localhost", "9898", words);
        assertNotNull(loc);
        assertEquals(-3.186103, loc.getCoordinates().lng, 0.0);
        assertEquals(55.944656, loc.getCoordinates().lat, 0.0);
    }

    @Test
    public void testPathInterceptPolygon() {
        // creating the testing polygon
        Point polyA = Point.fromLngLat(1, 1);
        Point polyB = Point.fromLngLat(1, 9);
        Point polyC = Point.fromLngLat(9, 1);
        Point polyD = Point.fromLngLat(9, 9);
        List<Point> poly = new ArrayList<>();
        poly.add(polyA);
        poly.add(polyB);
        poly.add(polyC);
        poly.add(polyD);
        poly.add(polyA);
        LineString polyOuter = LineString.fromLngLats(poly);
        Polygon polygon = Polygon.fromOuterInner(polyOuter);

        Point testATrue1 = Point.fromLngLat(0, 0);
        Point testBTrue1 = Point.fromLngLat(10, 10);
        Point testATrue2 = Point.fromLngLat(0, 2);
        Point testBTrue2 = Point.fromLngLat(10, 2);
        Point testATrue3 = Point.fromLngLat(1, 10);
        Point testBTrue3 = Point.fromLngLat(3, 7);
        Point testATrue4 = Point.fromLngLat(3, 10);
        Point testBTrue4 = Point.fromLngLat(3, 7);

        Point testAFalse1 = Point.fromLngLat(0, 11);
        Point testBFalse1 = Point.fromLngLat(4, 12);
        Point testAFalse2 = Point.fromLngLat(0, 1);
        Point testBFalse2 = Point.fromLngLat(0, 6);


        assertTrue(GeoJsonUtils.pathInterceptPolygon(testATrue1, testBTrue1, polygon));
        assertTrue(GeoJsonUtils.pathInterceptPolygon(testATrue2, testBTrue2, polygon));
        assertTrue(GeoJsonUtils.pathInterceptPolygon(testATrue3, testBTrue3, polygon));
        assertTrue(GeoJsonUtils.pathInterceptPolygon(testATrue4, testBTrue4, polygon));

        assertFalse(GeoJsonUtils.pathInterceptPolygon(testAFalse1, testBFalse1, polygon));
        assertFalse(GeoJsonUtils.pathInterceptPolygon(testAFalse2, testBFalse2, polygon));
    }
}