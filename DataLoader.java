package com.cinco;

/*
 * A central data management class that parses multiple CSV files to populate internal maps, establishing 
 * complex relationships between persons, companies, items, and invoices.
 */
import java.io.File;
import java.time.LocalDate;
import java.util.*;

public class DataLoader {

    public Map<String, Person> persons = new HashMap<>();
    public Map<String, Company> companies = new HashMap<>();
    public Map<String, ItemTemplate> itemTemplates = new HashMap<>();
    public Map<String, Invoice> invoices = new HashMap<>();
    
//Reads a CSV file containing personal information and populates the persons map.
    /* =========================
       LOAD PERSONS
       ========================= */
    public void loadPersons(String fileName) throws Exception {
        Scanner sc = new Scanner(new File(fileName));
        if (sc.hasNextLine()) sc.nextLine(); 

        while (sc.hasNextLine()) {
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] tokens = line.split(",");
            if (tokens.length < 4) continue;

            String uuid = tokens[0];
            String first = tokens[1];
            String last = tokens[2];
            String phone = tokens[3];

            List<String> emails = new ArrayList<>();
            for (int i = 4; i < tokens.length; i++) {
                if (!tokens[i].isBlank()) emails.add(tokens[i]);
            }

            persons.put(uuid, new Person(uuid, first, last, phone, emails));
        }
        sc.close();
    }

    //Parses company data and associates each company with a "Primary Contact" from the previously loaded persons.
    /* =========================
       LOAD COMPANIES
       ========================= */
    public void loadCompanies(String fileName) throws Exception {
        Scanner sc = new Scanner(new File(fileName));
        if (sc.hasNextLine()) sc.nextLine(); 

        while (sc.hasNextLine()) {
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] t = line.split(",");
            if (t.length < 7) continue;

            String uuid = t[0];
            String contactUuid = t[1];
            String name = t[2];
            String street = t[3];
            String city = t[4];
            String state = t[5];
            String zip = t[6];

            Person contact = persons.get(contactUuid);
            if (contact == null) {
                System.err.println("Company contact not found: " + contactUuid);
                continue;
            }

            Address address = new Address(street, city, state, zip);
            companies.put(uuid, new Company(uuid, contact, name, address));
        }
        sc.close();
    }

    //Creates templates for different types of business offerings (Equipment, Services, or Licenses).
    /* =========================
       LOAD ITEM TEMPLATES
       ========================= */
    public void loadItems(String fileName) throws Exception {
        Scanner sc = new Scanner(new File(fileName));
        if (sc.hasNextLine()) sc.nextLine();

        while (sc.hasNextLine()) {
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] t = line.split(",");
            String uuid = t[0];
            String name = t[1];
            String type = t[2];

            try {
                if (type.equals("E") || type.equals("S")) {
                    double baseRate = Double.parseDouble(t[3]);
                    itemTemplates.put(uuid, new ItemTemplate(uuid, type, name, baseRate));
                } else if (type.equals("L")) {
                    double monthlyFee = Double.parseDouble(t[3]);
                    double licenseFee = Double.parseDouble(t[4]);
                    itemTemplates.put(uuid, new ItemTemplate(uuid, type, name, monthlyFee, licenseFee));
                }
            } catch (Exception e) {
                System.err.println("Error parsing item template: " + uuid);
            }
        }
        sc.close();
    }

    //Initializes the "Header" of an invoice, connecting a customer to a salesperson.
    /* =========================
       LOAD INVOICES
       ========================= */
    public void loadInvoices(String fileName) throws Exception {
        Scanner sc = new Scanner(new File(fileName));
        if (sc.hasNextLine()) sc.nextLine();

        while (sc.hasNextLine()) {
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] t = line.split(",");
            String uuid = t[0];
            String customerUuid = t[1];
            String salesUuid = t[2];
            LocalDate date = LocalDate.parse(t[3]);

            Company customer = companies.get(customerUuid);
            Person salesPerson = persons.get(salesUuid);

            if (customer != null && salesPerson != null) {
                invoices.put(uuid, new Invoice(uuid, customer, salesPerson, date));
            }
        }
        sc.close();
    }

    //The "Junction" method that populates existing invoices with specific line items.
    /* =========================
       LOAD INVOICE ITEMS
       ========================= */
    public void loadInvoiceItems(String fileName) throws Exception {
        Scanner sc = new Scanner(new File(fileName));
        if (sc.hasNextLine()) sc.nextLine();

        while (sc.hasNextLine()) {
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] t = line.split(",");
            String invoiceUuid = t[0];
            String itemUuid = t[1];

            Invoice invoice = invoices.get(invoiceUuid);
            ItemTemplate template = itemTemplates.get(itemUuid);

            if (invoice == null || template == null) continue;

            try {
                String type = template.getTypeCode();
                
                if (type.equals("E")) {
                    invoice.addItem(new Equipment(template.getUuid(), template.getName(), template.getBaseRate()));
                
                } else if (type.equals("S")) {

                    invoice.addItem(new Service(template.getUuid(), template.getName(), template.getBaseRate()));
                
                } else if (type.equals("L")) {

                    invoice.addItem(new License(template.getUuid(), template.getName(), template.getExtraFee(), template.getBaseRate()));
                }
            } catch (Exception ex) {
                System.err.println("Error loading invoice item for " + invoiceUuid + ": " + ex.getMessage());
            }
        }
        sc.close();
    }

    /* =========================
       GETTERS
       ========================= */

    //returns an ArrayList of the populated Person class values
    public List<Person> getPersons() {
        return new ArrayList<>(this.persons.values());
    }
  //returns an ArrayList of the populated Company class values
    public List<Company> getCompanies() {
        return new ArrayList<>(this.companies.values());
    }
  //returns an ArrayList of the populated Item class values
    public List<ItemTemplate> getItemTemplates() {
        return new ArrayList<>(this.itemTemplates.values());
    }
  //returns an ArrayList of the populated Invoice class values
    public List<Invoice> getInvoices() {
        return new ArrayList<>(this.invoices.values());
    }
}
