package com.example.mypoject1;

/**
 * MyUser class represents a user with basic attributes such as first name, last name,
 * phone number, year of birth, and a URI for the profile image.
 *
 * <p>
 * This class provides constructors for creating user objects and getter methods to access
 * each field. It is designed to be a simple model class for user information.
 * </p>
 */
public class MyUser {

    // Private fields representing user attributes.
    private String firstName;
    private String lastName;
    private String phone;
    private int yob;  // Year of birth.
    private String profileImageUri; // URI to the user's profile image.

    /**
     * Default no-argument constructor.
     *
     * <p>
     * This constructor is useful for frameworks that require a no-argument constructor,
     * or for creating an object that will have its fields set later.
     * </p>
     */
    public MyUser() {
        // No initialization is done here.
    }

    /**
     * Parameterized constructor to initialize a MyUser object with specific values.
     *
     * <p>
     * This constructor sets the first name, last name, phone number, and year of birth.
     * Note: The profileImageUri is not set by this constructor.
     * </p>
     *
     * @param firstName The first name of the user.
     * @param lastName  The last name of the user.
     * @param phone     The phone number of the user.
     * @param yob       The year of birth of the user.
     */
    public MyUser(String firstName, String lastName, String phone, int yob) {
        // Initialize the user's first name.
        this.firstName = firstName;
        // Initialize the user's last name.
        this.lastName = lastName;
        // Initialize the user's phone number.
        this.phone = phone;
        // Initialize the user's year of birth.
        this.yob = yob;
        this.profileImageUri = profileImageUri;
    }

    /**
     * Retrieves the first name of the user.
     *
     * @return A String representing the user's first name.
     */
    public String getFirstName() {
        // Return the value of the firstName field.
        return firstName;
    }

    /**
     * Retrieves the last name of the user.
     *
     * @return A String representing the user's last name.
     */
    public String getLastName() {
        // Return the value of the lastName field.
        return lastName;
    }

    /**
     * Retrieves the phone number of the user.
     *
     * @return A String representing the user's phone number.
     */
    public String getPhone() {
        // Return the value of the phone field.
        return phone;
    }

    /**
     * Retrieves the year of birth of the user.
     *
     * @return An integer representing the user's year of birth.
     */
    public int getYob() {
        // Return the value of the yob field.
        return yob;
    }

    /**
     * Retrieves the URI of the user's profile image.
     *
     * @return A String representing the URI of the user's profile image.
     */
    public String getProfileImageUri() {
        // Return the value of the profileImageUri field.
        return profileImageUri;
    }
}
