package com.example.mypoject1;

public class MyUser {
    private String firstName;
    private String lastName;
    private String phone;
    private int yob;
    private String profileImageUri;

    public MyUser() {
    }

    public MyUser(String firstName, String lastName, String phone, int yob) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.yob = yob;
        this.profileImageUri = profileImageUri;
    }

    public String getFirstName() {
        return firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public String getPhone() {
        return phone;
    }
    public int getYob() {
        return yob;
    }
    public String getProfileImageUri() {
        return profileImageUri;
    }
}
