package com.cinco;

import com.google.gson.annotations.SerializedName;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.time.LocalDate;

/**
 * The License class represents a software or service license.
 * It calculates costs based on a monthly/annual rate and a one-time service fee.
 */
@XStreamAlias("linkedhashmap")
public class License extends Item {
    
    @SerializedName("fee")
    private final String serviceFee;
    
    @SerializedName("annualFee")
    private final String annualFee;

    @XStreamOmitField
    private transient final LocalDate start;
    
    @XStreamOmitField
    private transient final LocalDate end;
    
    @XStreamOmitField
    private transient final int quantity;

    /**
     * Bridge Constructor for DataLoader (Phase V).
     * Maps database columns directly to the object.
     */
    public License(String uuid, String name, double monthlyRate, double serviceFee, int quantity) {
        super(uuid, name, "L");
        this.annualFee = String.format("%.1f", monthlyRate);
        this.serviceFee = String.format("%.1f", serviceFee);
        this.quantity = quantity;
        this.start = null;
        this.end = null;
    }

    /**
     * Original Full Constructor for XML/JSON serialization.
     */
    public License(String uuid, String name, double serviceFee, double annualFee, LocalDate start, LocalDate end) {
        super(uuid, name, "L");
        this.serviceFee = String.format("%.1f", serviceFee);
        this.annualFee = String.format("%.1f", annualFee);
        this.start = start;
        this.end = end;
        this.quantity = 1; // Default for template items
    }

    /**
     * Simplified constructor for initial item definitions.
     */
    public License(String uuid, String name, double serviceFee, double annualFee) {
        this(uuid, name, serviceFee, annualFee, null, null);
    }

    @Override
    public double getCost() {
        return Double.parseDouble(annualFee);
    }

    @Override
    public double getTax() {
        // Licenses are typically tax-exempt in this business model.
        return 0.0;
    }

    @Override
    public String getDetails() {
        return String.format("License (%d units at $%s/unit + $%s fee)", 
                quantity, annualFee, serviceFee);
    }

    @Override
    public double getDisplayTotal() {
        // Total = (Rate * Quantity) + One-time Service Fee
        return (getCost() * quantity) + Double.parseDouble(serviceFee);
    }

    public int getQuantity() {
        return quantity;
    }
}
