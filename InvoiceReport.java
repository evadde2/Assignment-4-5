package com.cinco;

/*
The InvoiceReport class acts as the system's central reporting engine by loading invoice data from CSV 
files and formatting it into sorted summaries and detailed financial reports for both console output and 
file storage.
 */

import java.io.File;
import java.io.PrintWriter;
import java.time.temporal.ChronoUnit;
import java.util.*;


public class InvoiceReport {

    public static void main(String[] args) {
        DataLoader dl = new DataLoader();
        
        try {
            dl.loadPersons("data/Persons.csv");
            dl.loadCompanies("data/Companies.csv");
            dl.loadItems("data/Items.csv");
            dl.loadInvoices("data/Invoices.csv");
            dl.loadInvoiceItems("data/InvoiceItems.csv");

            // Using the getter we added to DataLoader
            List<Invoice> invoices = dl.getInvoices();
            Map<String, Company> companyMap = dl.companies;

            StringBuilder sb = new StringBuilder();
            
            List<Invoice> sortedByTotal = new ArrayList<>(invoices);
            // Sorting by grand total descending
            sortedByTotal.sort((a, b) -> Double.compare(b.getGrandTotal(), a.getGrandTotal()));
            
            sb.append(generateSummaryByTotal(sortedByTotal));
            sb.append(generateCompanySummary(invoices, companyMap));
            sb.append(generateDetailedReport(sortedByTotal));

            String finalReport = sb.toString();
            System.out.println(finalReport);

            try (PrintWriter pw = new PrintWriter(new File("data/output.txt"))) {
                pw.print(finalReport);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Provides a quick look at the invoice UUID, customer name, item count, tax, and grand total for each invoice.
    private static String generateSummaryByTotal(List<Invoice> invoices) {
        StringBuilder sb = new StringBuilder();
        sb.append("+--------------------------------------------------------------------------------------------------------+\n");
        sb.append("| Summary Report - By Total                                                                              |\n");
        sb.append("+--------------------------------------------------------------------------------------------------------+\n");
        sb.append(String.format("%-36s %-30s %-10s %12s %12s\n", "Invoice #", "Customer", "Num Items", "Tax", "Total"));
        
        double totalTax = 0, totalGrand = 0;
        int totalItems = 0;
        for (Invoice inv : invoices) {
            sb.append(String.format("%-36s %-30.30s %-10d $ %11.2f $ %11.2f\n",
                inv.getUuid(), inv.getCustomer().getName(), inv.getItems().size(), inv.getTax(), inv.getGrandTotal()));
            totalTax += inv.getTax();
            totalGrand += inv.getGrandTotal();
            totalItems += inv.getItems().size();
        }
        sb.append("+--------------------------------------------------------------------------------------------------------+\n");
        sb.append(String.format("%68s %-10d $ %11.2f $ %11.2f\n\n", "", totalItems, totalTax, totalGrand));
        return sb.toString();
    }

    //Shows the business relationship volume (number of invoices and total spend) for every company in the system.
    private static String generateCompanySummary(Collection<Invoice> invoices, Map<String, Company> allCompanies) {
        Map<String, List<Invoice>> invoiceGroups = new HashMap<>();
        for (Invoice inv : invoices) {
            invoiceGroups.computeIfAbsent(inv.getCustomer().getUuid(), k -> new ArrayList<>()).add(inv);
        }

        Map<String, Company> sortedCompanies = new TreeMap<>();
        for (Company c : allCompanies.values()) {
            sortedCompanies.put(c.getName(), c);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("+----------------------------------------------------------------+\n");
        sb.append("| Company Invoice Summary Report                                 |\n");
        sb.append("+----------------------------------------------------------------+\n");
        sb.append(String.format("%-30s %-10s %-12s\n", "Company", "# Invoices", "Grand Total"));
        
        double totalGrand = 0;
        for (Company c : sortedCompanies.values()) {
            List<Invoice> companyInvoices = invoiceGroups.get(c.getUuid());
            int count = (companyInvoices == null) ? 0 : companyInvoices.size();
            // Changed Invoice::getTotal to Invoice::getGrandTotal
            double total = (companyInvoices == null) ? 0.0 : companyInvoices.stream().mapToDouble(Invoice::getGrandTotal).sum();
            
            sb.append(String.format("%-30.30s %-10d $ %11.2f\n", c.getName(), count, total));
            totalGrand += total;
        }
        sb.append("+----------------------------------------------------------------+\n");
        sb.append(String.format("%31s %-10d $ %11.2f\n\n", "", invoices.size(), totalGrand));
        return sb.toString();
    }

    /*
     * Provides the "fine print" for each transaction, including contact info, salesperson details, and itemized 
     * breakdowns.
     */
    private static String generateDetailedReport(List<Invoice> invoices) {
        StringBuilder sb = new StringBuilder();
        sb.append("INVOICE DETAILS\n===============\n");

        for (Invoice inv : invoices) {
            sb.append("Invoice#  ").append(inv.getUuid()).append("\n");
            sb.append("Date      ").append(inv.getDate()).append("\n");
            
            Company c = inv.getCustomer(); 
            sb.append("Customer: \n");
            sb.append(c.getName()).append(" (").append(c.getUuid()).append(")\n");
            
            Person contact = c.getContact();
            sb.append(contact.getLastName()).append(", ").append(contact.getFirstName());
            sb.append(" (").append(contact.getUuid()).append(") \n");
            // Changed getEmails() to getEmailList()
            sb.append("\t").append(contact.getEmailList()).append("\n");
            
            sb.append("\t").append(c.getAddress().getStreet()).append("\n");
            sb.append("\t").append(c.getAddress().getCity()).append(", ")
              .append(c.getAddress().getState()).append(" ")
              .append(c.getAddress().getZip()).append("\n\n");

            Person sp = inv.getSalesperson();
            sb.append("Sales Person: \n");
            sb.append(sp.getLastName()).append(", ").append(sp.getFirstName());
            sb.append(" (").append(sp.getUuid()).append(") \n");
            // Changed getEmails() to getEmailList()
            sb.append("\t").append(sp.getEmailList()).append("\n");

            sb.append(String.format("Items (%d) %50s %11s %11s\n", inv.getItems().size(), "", "Tax", "Total"));
            sb.append("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-                          -=-=-=-=-=- -=-=-=-=-=-\n");

            for (Item item : inv.getItems()) {
                String typeLabel = "";
                if (item instanceof Equipment e) {
                    typeLabel = e.getStatus().equals("P") ? "(Purchase)" : "(Lease)";
                } else if (item instanceof Service) {
                    typeLabel = "(Service)";
                } else if (item instanceof License) {
                    typeLabel = "(License)";
                }

                sb.append(String.format("%s %-10s %s\n", item.getUuid(), typeLabel, item.getName()));
                sb.append("\t").append(item.getDetails()).append("\n");
                
                // Changed getTotal() to getDisplayTotal() or getGrandTotal()
                sb.append(String.format("%61s $ %11.2f $ %11.2f\n", "", item.getTax(), item.getCost()));
            }
            
            sb.append("                                                             -=-=-=-=-=- -=-=-=-=-=-\n");
            sb.append(String.format("%51s Subtotals $ %11.2f $ %11.2f\n", "", inv.getTax(), inv.getGrandTotal() - inv.getTax()));
            sb.append(String.format("%51s Grand Total             $ %11.2f\n\n", "", inv.getGrandTotal()));
        }
        return sb.toString();
    }
}
