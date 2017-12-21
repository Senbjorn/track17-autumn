package ru.track.prefork;

import java.io.Serializable;

public class Message implements Serializable {

    private long time;
    private String author = null;
    private String text;

    public Message(String text) {
        this.text = text;
        this.author = "Anonymous";
        time = System.currentTimeMillis();
    }

    public Message(long time, String text) {
        this.text = text;
        this.time = time;
    }

    public Message(String author, String text) {
        this.author = author;
        this.text = text;
        time = System.currentTimeMillis();
    }

    public Message(long time, String author, String text) {
        this.text = text;
        this.time = time;
        this.author = author;
    }

    @Override
    public String toString() {
        return String.format("Message{time=%d; text=\"%s\"; author=\"%s\"}", time, text, author);
    }

    public long getTime() {
        return time;
    }

    public String getText() {
        return text;
    }

    public String getAuthor() {
        return author;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}
