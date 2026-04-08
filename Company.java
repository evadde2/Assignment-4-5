package com.cinco;
/*
 * This entity class models a business organization by linking its unique identifier 
 * and name to a primary contact person and a physical street address.
 */

public class Company {

    private String uuid;
    private Person contact;
    private String name;
    private Address address; 

    /*
     * Initializes the company instance by assigning a unique ID, 
     * a primary contact person, a legal name, and a physical address.
     */
    public Company(String uuid, Person contact, String name, Address address) {
        this.uuid = uuid;
        this.contact = new Person(contact.getUuid());;
        this.name = name;
        this.address = address;
    }

    /*
     * Returns the unique string identifier used to distinguish this company within the system.
     */
    public String getUuid() { return uuid; }
    /*
     * Returns the Person object representing the primary point of contact for the company.
     */
    public Person getContact() { return contact; }
    /*
     * Returns the official legal name of the business entity.
     */
    public String getName() { return name; }
    /*
     * Returns the Address object containing the company's specific geographic location data.
     */
    public Address getAddress() { return address; }
}
