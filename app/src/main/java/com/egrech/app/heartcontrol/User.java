package com.egrech.app.heartcontrol;

import java.util.ArrayList;

public class User {

    String userName;
    String userId;

    enum sportingLevel {
        NULL,
        NOSPORT,
        SPORTING,
        PROFISPORTING

    }
    int smoker;
    int patology;
    int workingActivity;
    int age;
    int gender;
    String sporting;

    int averageHeartRate;
    String sleepAverageValues;

    public User(String userId) {
        this.userId = userId;
    }

    public User() {
    }

    public String getSleepAverageValues() {
        return sleepAverageValues;
    }

    public void setSleepAverageValues(String sleepAverageValues) {
        this.sleepAverageValues = sleepAverageValues;
    }

    public int getAverageHeartRate() {
        return averageHeartRate;
    }

    public void setAverageHeartRate(int averageHeartRate) {
        this.averageHeartRate = averageHeartRate;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int isSmoker() {
        return smoker;
    }

    public void setSmoker(int smoker) {
        this.smoker = smoker;
    }

    public int isPatology() {
        return patology;
    }

    public void setPatology(int patology) {
        this.patology = patology;
    }

    public int isWorkingActivity() {
        return workingActivity;
    }

    public void setWorkingActivity(int workingActivity) {
        this.workingActivity = workingActivity;
    }

    public String getSporting() {
        return sporting;
    }

    public void setSporting(String sporting) {
        this.sporting = sporting;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public User.sportingLevel setSportingLevel(int position) {
        switch (position) {
            case 0: return sportingLevel.NOSPORT;
            case 1: return sportingLevel.SPORTING;
            case 2: return sportingLevel.PROFISPORTING;
        }
        return null;
    }

    public int getSportingLevel(String level) {
        switch (level) {
            case "NOSPORT": return 0;
            case "SPORTING": return 1;
            case "PROFISPORTING": return 2;
        }
        return 0;
    }
}
