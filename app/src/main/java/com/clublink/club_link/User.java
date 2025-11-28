package com.clublink.club_link;

public class User {
    private int id;
    private String fullname;
    private String email;
    private String username;
    private String password;

    public User(String fullname, String email, String username, String password) {
        this.fullname = fullname;
        this.email = email;
        this.username = username;
        this.password = password;
    }

    public String getFullname() {
        return fullname; }
    public String getEmail() {
        return email; }
    public String getUsername() {
        return username; }
    public String getPassword() {
        return password; }


    }

