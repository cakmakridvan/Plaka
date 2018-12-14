package com.aeyacin.cemaradevicetrack.db.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class DataModel extends RealmObject {

    private String x;
    private String y;
    private String z;
    @PrimaryKey
    private int id;

    public int getID() {
        return id;
    }

    public void setID(int id) {
        this.id = id;
    }

    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    public String getY() {
        return y;
    }

    public void setY(String y) {
        this.y = y;
    }

    public String getZ() {
        return z;
    }

    public void setZ(String z) {
        this.z = z;
    }


}
