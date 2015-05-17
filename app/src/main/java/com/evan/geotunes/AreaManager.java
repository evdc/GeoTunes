package com.evan.geotunes;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by andrew on 2/1/15.
 */
public class AreaManager {
    public AreaManager() {
        areaList = new ArrayList<Area>();
    }

    private ArrayList<Area> areaList;

    public void addArea(Area area) {
        areaList.add(area);
    }

    public ArrayList<Integer> getMatchingAreas(LatLng point) {
        ArrayList<Integer> out = new ArrayList<Integer>();
        for (Area area : areaList) {
            if (area.containsPoint(point)) {
                out.add(area.getId());
            }
        }
        return out;
    }

    public ArrayList<Change> diffIds(ArrayList<Integer> firstList, ArrayList<Integer> secondList) {
        ArrayList<Change> out = new ArrayList<Change>();
        for (int firstInt : firstList) {
            boolean found = false;
            for (int secondInt : secondList) {
                if (firstInt == secondInt) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                out.add(new Change(firstInt, Change.ChangeType.CHANGE_TYPE_EXIT));
            }
        }

        for (int secondInt : secondList) {
            boolean found = false;
            for (int firstInt : firstList) {
                if (firstInt == secondInt) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                out.add(new Change(secondInt, Change.ChangeType.CHANGE_TYPE_ENTER));
            }
        }
        return out;
    }

    public static class Area {
        private int mId;
        private LatLng mOrigin;
        private float mRadius;
        public Area(int id, LatLng origin, float radius) {
            mId = id;
            mOrigin = origin;
            mRadius = radius;
        }

        public boolean containsPoint(LatLng point) {
            float[] results = new float[1];
            Location.distanceBetween(mOrigin.latitude, mOrigin.longitude, point.latitude, point.longitude, results);
            return results[0] <= mRadius;
        }

        public int getId() {
            return mId;
        }
    }

    public static class Change {
        public int id;
        public ChangeType changeType;

        public Change(int aId, ChangeType aChangeType) {
            id = aId;
            changeType = aChangeType;
        }

        public static enum ChangeType {
            CHANGE_TYPE_ENTER,
            CHANGE_TYPE_EXIT
        }

        public String toString() {
            if (changeType == ChangeType.CHANGE_TYPE_ENTER) {
                return "Entered area " + String.valueOf(id);
            } else {
                return "Exited area " + String.valueOf(id);
            }
        }
    }
}
