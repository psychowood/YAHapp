package com.psychowood.yahapp;

class MainCard {
    int id;
    String title;
    String action;
    Integer backgroundImage;
    Integer backgroundColor;

    MainCard(int id, String title, String action, Integer backgroundImage, Integer backgroundColor) {
        this.id = id;
        this.title = title;
        this.action = action;
        this.backgroundImage = backgroundImage;
        this.backgroundColor = backgroundColor;
    }

    MainCard(int id, String title, String action) {
        this.id = id;
        this.title = title;
        this.action = action;
    }

}