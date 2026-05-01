package com.cinco;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataLoader {
    private static final Logger logger = LogManager.getLogger(DataLoader.class);

    private final Map<String, Person> persons = new HashMap<>();
    private final Map<String, Company> companies = new HashMap<>();
    private final Map<String, Item> itemTemplates = new HashMap<>(); 
    private final Map<String, Invoice> invoices = new HashMap<>();

    public void loadAll() {
        // Log line to match expected output timestamp format if needed
        logger.info("Started..."); 
        
        loadPersons();
        loadCompanies();
        loadItems();
        loadInvoices();
        loadInvoiceItems(); 
    }

    private void loadPersons() {
        // We remove the phone column entirely to stop the Syntax Error
        String sql = "SELECT personId, personUuid, firstName, lastName FROM Person"; 
        
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String uuid = rs.getString("personUuid").trim();
                // We pass null for the phone/emails initially
                Person p = new Person(rs.getInt("personId"), uuid, 
                                    rs.getString("firstName"), rs.getString("lastName"), null);
                persons.put(uuid, p);
            }
        } catch (SQLException e) { 
            logger.error("Error loading persons: " + e.getMessage()); 
            throw new RuntimeException(e);
        }
    }
 // Inside DataLoader.java
    private void loadCompanies() {
        String sql = "SELECT c.companyId, c.companyUuid, c.name, a.addressId, a.street, z.city, z.state, z.zip, p.personUuid AS contactUuid " +
                     "FROM Company c " +
                     "LEFT JOIN Person p ON c.primaryContactId = p.personId " +
                     "LEFT JOIN Address a ON c.addressId = a.addressId " +
                     "LEFT JOIN ZipCode z ON a.zipCodeId = z.zipCodeId";
                     
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
             
        	while (rs.next()) {
        	    String uuid = rs.getString("companyUuid").trim();
        	    String name = rs.getString("name");
        	    
        	    // FETCH THE PERSON: Using the alias 'contactUuid' from your SELECT
        	    String contactUuid = rs.getString("contactUuid");
        	    Person contact = (contactUuid != null) ? persons.get(contactUuid.trim()) : null;

        	    // FETCH THE ADDRESS: Only create if addressId exists
        	    Address addr = null;
        	    int addrId = rs.getInt("addressId");
        	    if (!rs.wasNull()) {
        	        addr = new Address(
        	            addrId, 
        	            rs.getString("street"), 
        	            rs.getString("city"), 
        	            rs.getString("state"), 
        	            rs.getString("zip")
        	        );
        	    }

        	    companies.put(uuid, new Company(rs.getInt("companyId"), uuid, name, contact, addr));
        	}
        } catch (SQLException e) { 
            logger.error("Error loading companies: " + e.getMessage()); 
        }
    }    
    private void loadItems() {
        String sql = "SELECT itemUuid, itemType, name, basePrice, serviceRate, licenseFee FROM Item";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String uuid = rs.getString("itemUuid");
                String type = rs.getString("itemType");
                String name = rs.getString("name");
                Item item = null; 
                
                if (type.equals("E")) {
                    item = new Equipment(uuid, name, rs.getDouble("basePrice"), 0, "Purchase"); 
                } else if (type.equals("S")) {
                    item = new Service(uuid, name, rs.getDouble("serviceRate"), 0.0);
                } else if (type.equals("L")) {
                    item = new License(uuid, name, rs.getDouble("basePrice"), rs.getDouble("licenseFee"), 0);
                }
                
                if (item != null) {
                    itemTemplates.put(uuid, item);
                }
            }
        } catch (SQLException e) { logger.error("Error loading items: ", e); }
    }

    private void loadInvoices() {
        String sql = "SELECT i.invoiceUuid, i.invoiceDate, c.companyUuid, p.personUuid AS salesUuid " +
                     "FROM Invoice i " +
                     "LEFT JOIN Company c ON i.customerId = c.companyId " +
                     "LEFT JOIN Person p ON i.salesPersonId = p.personId";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
        	while (rs.next()) {
        	    // 1. Normalize the Invoice UUID
        	    String invUuid = rs.getString("invoiceUuid").trim().toLowerCase();
        	    
        	    // 2. Get Foreign Key UUIDs
        	    String compUuid = rs.getString("companyUuid");
        	    String salesUuid = rs.getString("salesUuid");

        	    // 3. Look up the objects in your existing Maps
        	    Company customerCompany = (compUuid != null) ? companies.get(compUuid.trim()) : null;
        	    Person sales = (salesUuid != null) ? persons.get(salesUuid.trim()) : null;

        	    // 4. Handle the Date
        	    java.sql.Date dbDate = rs.getDate("invoiceDate");
        	    LocalDate date = (dbDate != null) ? dbDate.toLocalDate() : LocalDate.now();

        	    // 5. THE FIX: Call the constructor with the actual variables
        	    // Assumes constructor: Invoice(String uuid, Company customer, Person sales, LocalDate date)
        	    Invoice inv = new Invoice(invUuid, customerCompany, sales, date);
        	    
        	    this.invoices.put(invUuid, inv);
        	}        } catch (SQLException e) { 
            logger.error("Error loading invoices", e); 
        }
    }
    
    private void loadInvoiceItems() {
    	String sql = "SELECT i.invoiceUuid, it.itemUuid, it.itemType, it.name, " +
                "it.basePrice, it.serviceRate, it.licenseFee, " +
                "ii.quantityOrHours, ii.startDate, ii.endDate, ii.purchaseOrLease, " +
                "p.personUuid " + 
                "FROM InvoiceItem ii " +
                "JOIN Invoice i ON ii.invoiceId = i.invoiceId " + // JOIN on IDs
                "JOIN Item it ON ii.itemId = it.itemId " +      // JOIN on IDs
                "LEFT JOIN Person p ON ii.servicePersonId = p.personId";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
        	while (rs.next()) {
        	    // 1. Get the Raw ID from the database
        	    String rawInvUuid = rs.getString("invoiceUuid");
        	    
        	    // 2. GUARD: Skip this row if the invoice UUID is null
        	    if (rawInvUuid == null) continue;

        	    // 3. Normalize and find the invoice
        	    String invKey = rawInvUuid.trim().toLowerCase();
        	    Invoice inv = invoices.get(invKey);
        	    
        	    if (inv != null) {
        	        // 4. GUARD: Check Item Type and UUID before trimming
        	        String rawType = rs.getString("itemType");
        	        String rawItemUuid = rs.getString("itemUuid");
        	        
        	        if (rawType == null || rawItemUuid == null) continue;

        	        String type = rawType.trim();
        	        String itemUuid = rawItemUuid.trim();
        	        String name = rs.getString("name");
        	        double qty = rs.getDouble("quantityOrHours");

        	        if (type.equals("E")) {
        	            inv.addItem(new Equipment(itemUuid, name, rs.getDouble("basePrice"), (int)qty, rs.getString("purchaseOrLease")));
        	        } else if (type.equals("S")) {
        	            String techUuid = rs.getString("personUuid");
        	            Person technician = (techUuid != null) ? persons.get(techUuid.trim()) : null;
        	            inv.addItem(new Service(itemUuid, name, rs.getDouble("serviceRate"), technician, qty));
        	        } else if (type.equals("L")) {
        	            java.sql.Date start = rs.getDate("startDate");
        	            java.sql.Date end = rs.getDate("endDate");
        	            if (start != null && end != null) {
        	                inv.addItem(new License(itemUuid, name, rs.getDouble("basePrice"), rs.getDouble("licenseFee"), 
        	                            start.toLocalDate(), end.toLocalDate()));
        	            }
        	        }
        	    }
        	}
        } catch (SQLException e) {
            logger.error("Error loading invoice items", e);
        }
    }
    
    public Map<String, Person> getPersons() { return this.persons; }
    public Map<String, Company> getCompanies() { return this.companies; }
    public Map<String, Item> getItemTemplates() { return this.itemTemplates; }
    public Map<String, Invoice> getInvoices() { return this.invoices; }
    
    public List<Invoice> getInvoicesList() {
        return new ArrayList<>(this.invoices.values());
    }
}
