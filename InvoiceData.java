package com.cinco;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.sql.Statement;

public class InvoiceData {
	public static void main(String[] args) {
	    try {
	        clearDatabase();

	        // --- PERSONS ---
	        addPerson("p1", "Armando", "Bearblock", "Springfield");
	        addPerson("p2", "Sena", "Rucklidge", "Portland");
	        addPerson("p3", "Hortensia", "Thickins", "Austin");

	        // --- EMAILS ---
	        addEmail("p1", "abearblock0@constantcontact.com");
	        addEmail("p2", "srucklidge1@tuttocitta.it");
	        addEmail("p2", "srucklidge1@surveymonkey.com");
	        addEmail("p3", "hthickins2@zimbio.com");

	        // --- COMPANIES ---
	        addCompany("c1", "Bearblock Solutions", "p1", "123 Main St", "Springfield", "IL", "62701");
	        addCompany("c2", "Rucklidge Consulting", "p2", "456 Oak Avenue", "Portland", "OR", "97205");
	        addCompany("c3", "Thickins Trading", "p3", "789 Pine Road", "Austin", "TX", "73301");

	        // --- ITEMS ---
	        addEquipment("i1", "3D Printer", 1200.50);
	        addEquipment("i2", "Robotic Arm", 500.00);
	        addService("i3", "Consulting Hour", 150.00);
	        addLicense("i4", "Project Management Software", 500.00, 12000.00);
	        addLicense("i5", "Enterprise Analytics License", 1000.00, 15000.00);

	        // --- INVOICES ---
	        addInvoice("inv1", "c1", "p1", LocalDate.of(2026, 2, 12));
	        addInvoice("inv2", "c2", "p2", LocalDate.of(2026, 1, 13));
	        addInvoice("inv3", "c3", "p3", LocalDate.of(2026, 1, 25));

	        // --- INVOICE ITEMS ---
	        addEquipmentPurchaseToInvoice("inv1", "i1", 2);
	        addEquipmentLeaseToInvoice("inv1", "i2", 2);

	        addServiceToInvoice("inv2", "i3", "p2", 1.0);
	        addLicenseToInvoice("inv2", "i4", LocalDate.of(2026,1,1), LocalDate.of(2026,6,30));

	        addLicenseToInvoice("inv3", "i5", LocalDate.of(2026,1,1), LocalDate.of(2028,6,30));

	        System.out.println("Staging Complete.");

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

    /**
     * 1. Clear Database: Disables foreign keys to ensure all tables are wiped clean.
     */
    public static void clearDatabase() {
        String[] tables = {"InvoiceItem", "Invoice", "Email", "Company", "Item", "Person"};
        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.prepareStatement("SET FOREIGN_KEY_CHECKS = 0").execute();
            for (String table : tables) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM " + table)) {
                    ps.executeUpdate();
                }
            }
            conn.prepareStatement("SET FOREIGN_KEY_CHECKS = 1").execute();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    /**
     * 2. Add Person: Handles basic person data.
     */
    public static void addPerson(Object personCode, String firstName, String lastName, String city) {
        // Using the city name as a dummy zip ensures uniqueness for different cities
        String zipInsert = "INSERT IGNORE INTO ZipCode (zip, city, state) VALUES (?, ?, 'N/A')";
        String zipSelect = "SELECT zipCodeId FROM ZipCode WHERE city = ?";
        String addrSql = "INSERT INTO Address (street, zipCodeId) VALUES ('N/A', ?)";
        String personSql = "INSERT INTO Person (personUuid, firstName, lastName, addressId) VALUES (?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection()) {
            // --- STEP 1: Get or Create ZipCodeId ---
            int zipCodeId = -1;
            try (PreparedStatement ps = conn.prepareStatement(zipInsert)) {
                ps.setString(1, city); // Use city name as the zip placeholder
                ps.setString(2, city);
                ps.executeUpdate();
            }
            
            try (PreparedStatement ps = conn.prepareStatement(zipSelect)) {
                ps.setString(1, city);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    zipCodeId = rs.getInt("zipCodeId");
                }
            }

            if (zipCodeId == -1) {
                throw new RuntimeException("Failed to find or create zipCodeId for city: " + city);
            }

            // --- STEP 2: Create Address ---
            int addressId = -1;
            try (PreparedStatement ps = conn.prepareStatement(addrSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, zipCodeId);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    addressId = rs.getInt(1);
                }
            }

            // --- STEP 3: Create Person ---
            try (PreparedStatement ps = conn.prepareStatement(personSql)) {
                ps.setString(1, personCode.toString());
                ps.setString(2, firstName);
                ps.setString(3, lastName);
                if (addressId != -1) {
                    ps.setInt(4, addressId);
                } else {
                    ps.setNull(4, java.sql.Types.INTEGER);
                }
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * 3. Add Email: Links email to person via subquery.
     */
    public static void addEmail(Object personCode, String email) {
        String sql = "INSERT INTO Email (personId, emailAddress) VALUES ((SELECT personId FROM Person WHERE personUuid = ?), ?)";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, personCode.toString());
            ps.setString(2, email);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    /**
     * 4. Add Company: Uses a subquery for the primary contact.
     */
    public static void addCompany(Object companyCode, String name, Object contactCode, String street, String city, String state, String zip) {
        String zipSql = "INSERT IGNORE INTO ZipCode (zip, city, state) VALUES (?, ?, ?)";
        String addrSql = "INSERT INTO Address (street, zipCodeId) VALUES (?, (SELECT zipCodeId FROM ZipCode WHERE zip = ? LIMIT 1))";
        String compSql = "INSERT INTO Company (companyUuid, name, primaryContactId, addressId) VALUES (?, ?, (SELECT personId FROM Person WHERE personUuid = ?), ?)";

        try (Connection conn = ConnectionFactory.getConnection()) {
            // 1. Ensure ZipCode exists
            try (PreparedStatement ps = conn.prepareStatement(zipSql)) {
                ps.setString(1, zip);
                ps.setString(2, city);
                ps.setString(3, state);
                ps.executeUpdate();
            }

            // 2. Create Address and get addressId
            int addressId = -1;
            try (PreparedStatement ps = conn.prepareStatement(addrSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, street);
                ps.setString(2, zip);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) addressId = rs.getInt(1);
            }

            // 3. Create Company
            try (PreparedStatement ps = conn.prepareStatement(compSql)) {
                ps.setString(1, companyCode.toString());
                ps.setString(2, name);
                ps.setString(3, contactCode.toString());
                if (addressId != -1) {
                    ps.setInt(4, addressId);
                } else {
                    ps.setNull(4, java.sql.Types.INTEGER);
                }
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /* --- ITEM METHODS (Supports Equipment, Service, and License) --- */

    public static void addEquipment(Object itemCode, String name, double pricePerUnit) {
        String sql = "INSERT INTO Item (itemUuid, itemType, name, basePrice) VALUES (?, 'E', ?, ?)";
        executeItemUpdate(itemCode, name, pricePerUnit, sql);
    }

    public static void addService(Object itemCode, String name, double hourlyRate) {
        String sql = "INSERT INTO Item (itemUuid, itemType, name, basePrice, serviceRate) VALUES (?, 'S', ?, 0.0, ?)";
        executeItemUpdate(itemCode, name, hourlyRate, sql);
    }

    public static void addLicense(Object itemCode, String name, double serviceFee, double annualFee) {
        String sql = "INSERT INTO Item (itemUuid, itemType, name, basePrice, licenseFee) VALUES (?, 'L', ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, itemCode.toString());
            ps.setString(2, name);
            ps.setDouble(3, serviceFee);
            ps.setDouble(4, annualFee);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    /* --- INVOICE METHODS --- */

    public static void addInvoice(Object invoiceCode, Object companyCode, Object salespersonCode, LocalDate date) {
        // 1. First, we need to make sure the Company and Salesperson actually exist in the DB
        String checkSql = "SELECT " +
                          "(SELECT companyId FROM Company WHERE companyUuid = ?) AS compId, " +
                          "(SELECT personId FROM Person WHERE personUuid = ?) AS salesId";
                          
        String insertSql = "INSERT INTO Invoice (invoiceUuid, customerId, salesPersonId, invoiceDate) VALUES (?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection()) {
            int dbCompanyId = -1;
            int dbSalesPersonId = -1;

            // --- STEP 1: Verify Parent Records ---
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, companyCode.toString());
                ps.setString(2, salespersonCode.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    dbCompanyId = rs.getInt("compId");
                    dbSalesPersonId = rs.getInt("salesId");
                }
            }

            // --- STEP 2: Error Handling for Missing Data ---
            if (dbCompanyId <= 0) {
                throw new RuntimeException("Error: Company with UUID " + companyCode + " does not exist. Cannot create invoice.");
            }
            if (dbSalesPersonId <= 0) {
                throw new RuntimeException("Error: Person with UUID " + salespersonCode + " does not exist. Cannot create invoice.");
            }

            // --- STEP 3: Insert Invoice ---
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, invoiceCode.toString());
                ps.setInt(2, dbCompanyId);
                ps.setInt(3, dbSalesPersonId);
                ps.setDate(4, java.sql.Date.valueOf(date));
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    
    private static void addItemToInvoice(Object invCode, Object itemCode, double quantity, LocalDate start, LocalDate end, Object personCode) {

        String sql =
            "INSERT INTO InvoiceItem (invoiceId, itemId, quantityOrHours) VALUES " +
            "((SELECT invoiceId FROM Invoice WHERE invoiceUuid = ?), " +
            "(SELECT itemId FROM Item WHERE itemUuid = ?), ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, invCode.toString());
            ps.setString(2, itemCode.toString());
            ps.setDouble(3, quantity);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void addEquipmentPurchaseToInvoice(Object invCode, Object itemCode, int quantity) { addItemToInvoice(invCode, itemCode, (double)quantity, null, null, null); }
    public static void addEquipmentLeaseToInvoice(Object invCode, Object itemCode, int quantity) { addItemToInvoice(invCode, itemCode, (double)quantity, null, null, null); }
    public static void addServiceToInvoice(Object invCode, Object itemCode, Object personCode, double hours) { addItemToInvoice(invCode, itemCode, hours, null, null, personCode); }
    public static void addLicenseToInvoice(Object invCode, Object itemCode, LocalDate start, LocalDate end) { addItemToInvoice(invCode, itemCode, 1.0, start, end, null); }

    private static void executeItemUpdate(Object code, String name, double value, String sql) {
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code.toString());
            ps.setString(2, name);
            ps.setDouble(3, value);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }
}
