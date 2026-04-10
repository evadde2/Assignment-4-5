package com.cinco;

/*
* A concrete Item subclass representing billable labor that calculates costs based on hourly rates,
* a fixed base fee, and a specific service-related tax percentage.
*/

import com.google.gson.annotations.SerializedName;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("linkedhashmap")
public class Service extends Item {
    
    @SerializedName("CostPerHour")
    private final String costPerHour;

    @XStreamOmitField
    private transient final Person technician;
    
    @XStreamOmitField
    private transient final double hours;

    /*
     * The primary constructor used during invoice processing to link a specific technician and the number of hours
     *  worked to the service.
     */
    public Service(String uuid, String name, double costPerHour, Person technician, double hours) {
        super(uuid, name, "S");
        this.costPerHour = String.format("%.1f", costPerHour);
        this.technician = technician;
        this.hours = hours;
    }

    /*
     * A simplified constructor used by the DataLoader for initial item definitions where technician and hour 
     * data are not yet applicable.
     */
    public Service(String uuid, String name, double costPerHour) {
        this(uuid, name, costPerHour, null, 0.0);
    }

    //Parses the costPerHour string to return the numerical hourly rate of the service.
    @Override
    public double getCost() {
        return Double.parseDouble(costPerHour);
    }

    //Calculates the service tax by applying a flat rate of 3.15% to the hourly cost, rounded to two decimal places.
    @Override
    public double getTax() {
        return round(getCost() * 0.0315);
    }

    //Returns the string "Service" as a type descriptor for reporting purposes.
    @Override
    public String getDetails() {
        return "Service";
    }

    //Calculates the total billable amount for the service by multiplying the hourly cost by the number of hours worked.
    @Override
    public double getDisplayTotal() {
        return getCost() * hours;
    }

    //return technician
    public Person getTechnician() { return technician; }
    
    //return hours
    public double getHours() { return hours; }
}
