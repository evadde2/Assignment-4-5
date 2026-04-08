package com.cinco;

/*
* A fundamental data class that stores personal identification and contact information,
* including a unique ID, name, phone number, and a list of electronic mail addresses.
*/

import java.util.List;
import java.util.ArrayList;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.xstream.annotations.XStreamAlias;


@XStreamAlias("person")
public class Person {
    private String uuid;
    private String firstName;
    private String lastName;
    private String phoneNumber; 
    
    @SerializedName("emails")
    @XStreamAlias("emails")
    private List<String> emailList; 

    /*
     * Initializes the person instance with a unique identifier, first and last name,
     * phone number, and a collection of email addresses.
     */
    public Person(String uuid, String firstName, String lastName, String phone, List<String> emails) {
        this.uuid = uuid;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phone;
        
        this.emailList = emails;
    }
    //returns just the uuid
    public Person(String uuid) {
        this(uuid, null, null, null, null);
    }

    //returns the Uuid
    public String getUuid() { return uuid; }
    
    //returns the first name
    public String getFirstName() { return firstName; }
    
    //returns the last name
    public String getLastName() { return lastName; }
    
    //returns the phone number
    public String getPhoneNumber() { return phoneNumber; }
    
    //returns the email list
    public List<String> getEmailList() { return emailList; }
}
