package uk.ac.ed.inf;

public class Drone {
    /** the current location of the drone */
    private LongLat currLoc;
    private String currLocName;
    /** angle to reach the next location */
    private int angle;
    /** where to go next*/
    private LongLat nextLoc;
    /** the target of the current lag of journey */
    private LongLat targetLoc;
    private String targetLocName;
    /** whether the drone is close to target location */
    private boolean arrived;
    /** whether the drone's planned move has intercepted with no-fly zones
     * before reaching current target location */
    private int intercept;
    /** how many moves the drone got left */
    private int moves;


    /**
     * construct the Drone object with where the Drone is
     * @param currLoc
     */
    public Drone(LongLat currLoc, String locName) {
        this.currLoc = currLoc;
        this.currLocName = locName;
        this.arrived = false;
        this.moves = 2000;
    }


    /**
     * set the target location of the drone
     * @param targetLoc
     */
    public void setTargetLoc(LongLat targetLoc, String targetLocName) {
        this.targetLoc = targetLoc;
        this.targetLocName = targetLocName;
        // a new lag has started so these fields are refreshed as they are storing
        // information only about one lag
        this.intercept = 0;
        this.arrived = false;
    }


    /**
     * manually set the nextLoc when automatically generated one is illegal
     * @param nextLoc
     */
    public void setNextLoc(LongLat nextLoc) {
        this.nextLoc = nextLoc;
    }

    /**
     * set when a drone touched a no-fly zone for the first time in a journey
     * @param orientation 1 for clockwise and -1 for anticlockwise
     */
    public void setIntercept(int orientation) {
        this.intercept = orientation;
    }


    /**
     * call this instead of getAngle when the drone has reached a destination
     */
    public void hover() {
        this.angle = -999;
    }


    /**
     * set nextPos with the target location
     */
    public void calculateAngle() {
        if (targetLoc == null) {
            System.err.println("The target of the drone is not yet set");
        }

        double ang = Math.toDegrees(Math.atan2(targetLoc.latitude - currLoc.latitude, targetLoc.longitude - currLoc.longitude));
        if (ang < 0) ang += 360;
        angle = (int) (Math.round(ang / 10) * 10);
    }

    public void planNextMove() {
        nextLoc = currLoc.nextPosition(angle);
    }

    /**
     * change currPos to nextPos
     */
    public void makeNextMove() {
        currLoc = nextLoc;
        if (currLoc.closeTo(targetLoc)) {
            arrived = true;
            currLocName = targetLocName;
        }
        moves --;
    }

    /** return how many moves the drone got left */
    public int getMoves() {
        return moves;
    }

    public LongLat getCurrLoc() {
        return currLoc;
    }

    public LongLat getNextLoc() {
        return nextLoc;
    }

    public int getAngle() {
        return angle;
    }

    public boolean hasArrived() {
        return arrived;
    }

    public String getCurrLocName() {
        return currLocName;
    }

    public String getTargetLocName() {
        return targetLocName;
    }

    public int getIntercept() {
        return intercept;
    }
}
