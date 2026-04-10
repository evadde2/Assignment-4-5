package com.cinco;

import com.google.gson.annotations.SerializedName;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/*The Equipment class models tangible assets by calculating unit-based costs and a 5.25% tax while using annotations 
 * to control data visibility in exports.
 */

@XStreamAlias("linkedhashmap")
public class Equipment extends Item {
    
    @SerializedName("CostPerUnit")
    private final String costPerUnit; 

    // These fields are needed for InvoiceReports but must be hidden from JSON/XML
    @XStreamOmitField
    private transient final String status;
    
    @XStreamOmitField
    private transient final int quantity;

    @XStreamOmitField
    private final double PURCHASE__TAX__RATE = 0.0525;

    //The primary constructor used to initialize a full equipment record.
    public Equipment(String uuid, String name, double costPerUnit, String status, int quantity) {
        super(uuid, name, "E");
        this.costPerUnit = String.format("%.1f", costPerUnit);
        this.status = status;
        this.quantity = quantity;
    }

    //A simplified "overloaded" constructor typically used during initial data loading.
    public Equipment(String uuid, String name, double costPerUnit) {
        this(uuid, name, costPerUnit, "NONE", 1);
    }
//returns status value
    public String getStatus() {
        return status;
    }
//returns quantity value
    public int getQuantity() {
        return quantity;
    }

    //returns the cost 
    @Override
    public double getCost() {
        return Double.parseDouble(costPerUnit);
    }
    //returns the tax
    @Override
    public double getTax() {
        return round(getCost() * PURCHASE__TAX__RATE);
    }

    //returns the details
    @Override
    public String getDetails() {
        return String.format("%s (%d units at $%s/unit)", status, quantity, costPerUnit);
    }

    //returns the overall total
    @Override
    public double getDisplayTotal() {
        return getCost() * quantity;
    }
}
