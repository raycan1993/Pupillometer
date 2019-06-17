package com.example.pupillometer;

public class Measurement {

    private int id;
    private double pupilLeft;
    private double pupilRight;
    private double difference;
    private String date;
    private String filepath;

    public Measurement(int id, double pupilLeft, double pupilRight, double difference, String date, String filepath){
        this.id = id;
        this.pupilLeft = pupilLeft;
        this.pupilRight = pupilRight;
        this.difference = difference;
        this.date = date;
        this.filepath = filepath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getPupilLeft() {
        return pupilLeft;
    }

    public void setPupilLeft(double pupilLeft) {
        this.pupilLeft = pupilLeft;
    }

    public double getPupilRight() {
        return pupilRight;
    }

    public void setPupilRight(double pupilRight) {
        this.pupilRight = pupilRight;
    }

    public double getDifference() {
        return difference;
    }

    public void setDifference(double difference) {
        this.difference = difference;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }
}
