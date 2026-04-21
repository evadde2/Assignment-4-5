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
        loadPersons();
        loadCompanies();
        loadItems();
        loadInvoices();
        loadInvoiceItems(); 
    }

    private void loadPersons() {
        String sql = "SELECT p.personId, p.personUuid, p.firstName, p.lastName, " +
                     "a.street, z.city, z.state, z.zip, e.emailAddress " +
                     "FROM Person p " +
                     "LEFT JOIN Address a ON p.addressId = a.addressId " +
                     "LEFT JOIN ZipCode z ON a.zipCodeId = z.zipCodeId " +
                     "LEFT JOIN Email e ON p.personId = e.personId";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String uuid = rs.getString("personUuid");
                if (!persons.containsKey(uuid)) {
                    Address addr = (rs.getString("street") == null) ? null :
                        new Address(null, rs.getString("street"), rs.getString("city"), rs.getString("state"), rs.getString("zip"));
                    persons.put(uuid, new Person(rs.getInt("personId"), uuid, rs.getString("firstName"), rs.getString("lastName"), addr));
                }
                if (rs.getString("emailAddress") != null) persons.get(uuid).addEmail(rs.getString("emailAddress"));
            }
        } catch (SQLException e) { logger.error("Error loading persons: ", e); }
    }

    private void loadCompanies() {
        String sql = "SELECT c.companyId, c.companyUuid, c.name, a.street, z.city, z.state, z.zip, p.personUuid AS contactUuid " +
                     "FROM Company c " +
                     "LEFT JOIN Address a ON c.addressId = a.addressId " +
                     "LEFT JOIN ZipCode z ON a.zipCodeId = z.zipCodeId " +
                     "LEFT JOIN Person p ON c.primaryContactId = p.personId";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Address addr = new Address(null, rs.getString("street"), rs.getString("city"), rs.getString("state"), rs.getString("zip"));
                companies.put(rs.getString("companyUuid"), new Company(rs.getInt("companyId"), rs.getString("companyUuid"), rs.getString("name"), persons.get(rs.getString("contactUuid")), addr));
            }
        } catch (SQLException e) { logger.error("Error loading companies: ", e); }
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
                    item = new Equipment(uuid, name, rs.getDouble("basePrice"), 0); 
                } else if (type.equals("S")) {
                    item = new Service(uuid, name, rs.getDouble("serviceRate"), 0.0);
                } else if (type.equals("L")) {
                    item = new License(uuid, name, rs.getDouble("basePrice"), 0);
                }
                
                if (item != null) {
                    itemTemplates.put(uuid, item);
                }
            }
        } catch (SQLException e) { logger.error("Error loading items: ", e); }
    }

    private void loadInvoices() {
        // We use JOIN to get the UUID strings from the Company and Person tables
        String sql = "SELECT i.invoiceUuid, i.invoiceDate, c.companyUuid, p.personUuid " +
                     "FROM Invoice i " +
                     "JOIN Company c ON i.customerId = c.companyId " +
                     "JOIN Person p ON i.salesPersonId = p.personId";
                     
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            int count = 0;
            while (rs.next()) {
                String invUuid = rs.getString("invoiceUuid");
                String compUuid = rs.getString("companyUuid");
                String persUuid = rs.getString("personUuid");

                // Look up the objects in your Map
                Company customer = companies.get(compUuid);
                Person salesPerson = persons.get(persUuid);

                // Safety check: Only add if the Map actually found the objects
                if (customer != null && salesPerson != null) {
                    invoices.put(invUuid, new Invoice(invUuid, customer, salesPerson, rs.getDate("invoiceDate").toLocalDate()));
                    count++;
                } else {
                    // This helps us see which specific UUID is failing the lookup
                    System.out.println("DEBUG: Lookup failed for Invoice " + invUuid);
                    if (customer == null) System.out.println("  -> Company UUID not found: " + compUuid);
                    if (salesPerson == null) System.out.println("  -> Person UUID not found: " + persUuid);
                }
            }
            System.out.println("Total Invoices loaded: " + count);
            
        } catch (SQLException e) {
            logger.error("Error in loadInvoices: ", e);
        }
    }

    private void loadInvoiceItems() {
        String sql = "SELECT i.invoiceUuid, it.itemUuid, it.itemType, it.name, it.basePrice, it.serviceRate, it.licenseFee, ii.quantityOrHours " +
                     "FROM InvoiceItem ii JOIN Invoice i ON ii.invoiceId = i.invoiceId " +
                     "JOIN Item it ON ii.itemId = it.itemId";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Invoice inv = invoices.get(rs.getString("invoiceUuid"));
                if (inv == null) continue;

                String type = rs.getString("itemType");
                String uuid = rs.getString("itemUuid");
                String name = rs.getString("name");
                double qh = rs.getDouble("quantityOrHours");

                if (type.equals("E")) {
                    inv.addItem(new Equipment(uuid, name, rs.getDouble("basePrice"), (int)qh));
                } else if (type.equals("S")) {
                    inv.addItem(new Service(uuid, name, rs.getDouble("serviceRate"), qh));
                } else if (type.equals("L")) {
                    inv.addItem(new License(uuid, name, rs.getDouble("basePrice"), (int)qh));
                }
            }
        } catch (SQLException e) { logger.error("Error loading invoice items: ", e); }
    }

    public Map<String, Person> getPersons() { return this.persons; }
    public Map<String, Company> getCompanies() { return this.companies; }
    public Map<String, Item> getItemTemplates() { return this.itemTemplates; }
    public Map<String, Invoice> getInvoices() { return this.invoices; }
    
    public List<Invoice> getInvoicesList() {
        return new ArrayList<>(this.invoices.values());
    }
}
