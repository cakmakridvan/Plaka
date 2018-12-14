package com.aeyacin.cemaradevicetrack.db;

import android.app.Application;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class RealmApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(this);
        RealmConfiguration configuration = new RealmConfiguration.Builder().name("data_gsensor.realm").deleteRealmIfMigrationNeeded().build();
        Realm.setDefaultConfiguration(configuration);
    }
}
