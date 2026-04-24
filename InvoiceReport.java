package com.cinco;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.cinco.staging.InvoiceDataStaging;

public class InvoiceReport {

    public static void main(String[] args) {
        // 1. STAGE: Fill the database first. 
        // Ensure DatabaseDataLoader is the class that runs your addPerson/addInvoice methods.
        InvoiceDataStaging.main(args); 

        // 2. LOAD: Pull the data from the DB into the maps
        DataLoader loader = new DataLoader();
        loader.loadAll();
        
        InvoiceReport report = new InvoiceReport();
        List<Invoice> invoices = loader.getInvoicesList();
        
        // 3. REPORT
        report.printSummaryReport(invoices);
        report.printCompanySummaryReport(invoices, new ArrayList<>(loader.getCompanies().values()));
        report.printDetailReports(invoices);
    }

    public void printSummaryReport(List<Invoice> invoices) {
        // Sort by Grand Total Descending
        invoices.sort((a, b) -> Double.compare(b.getGrandTotal(), a.getGrandTotal()));

        System.out.println("+--------------------------------------------------------------------------------------------------------+");
        System.out.println("| Summary Report - By Total                                                                              |");
        System.out.println("+--------------------------------------------------------------------------------------------------------+");
        System.out.printf("%-36s %-30s %-10s %10s %11s\n", "Invoice #", "Customer", "Num Items", "Tax", "Total");

        double totalTax = 0;
        double totalAmount = 0;
        int totalItems = 0;

        for (Invoice inv : invoices) {
            System.out.printf("%-36s %-30s %-10d $ %10.2f $ %10.2f\n",
                    inv.getUuid(),
                    inv.getCustomer().getName(),
                    inv.getItems().size(),
                    inv.getTotalTax(),
                    inv.getGrandTotal());
            totalTax += inv.getTotalTax();
            totalAmount += inv.getGrandTotal();
            totalItems += inv.getItems().size();
        }

        System.out.println("+--------------------------------------------------------------------------------------------------------+");
        // Aligned to match the expected output summary line
        System.out.printf("%68d          $ %10.2f $ %10.2f\n\n", totalItems, totalTax, totalAmount);
    }

    public void printCompanySummaryReport(List<Invoice> invoices, List<Company> allCompanies) {
        // Sort companies alphabetically
        allCompanies.sort(Comparator.comparing(Company::getName));

        System.out.println("+----------------------------------------------------------------+");
        System.out.println("| Company Invoice Summary Report                                 |");
        System.out.println("+----------------------------------------------------------------+");
        System.out.printf("%-30s %-10s %-15s\n", "Company", "# Invoices", "Grand Total");

        double overallTotal = 0;
        int overallInvoices = 0;

        for (Company c : allCompanies) {
            int count = 0;
            double companyTotal = 0;
            
            for (Invoice inv : invoices) {
                if (inv.getCustomer().getUuid().equals(c.getUuid())) {
                    count++;
                    companyTotal += inv.getGrandTotal();
                }
            }
            
            System.out.printf("%-30s %-10d $ %10.2f\n", c.getName(), count, companyTotal);
            overallTotal += companyTotal;
            overallInvoices += count;
        }

        System.out.println("+----------------------------------------------------------------+");
        System.out.printf("%31d          $ %10.2f\n\n", overallInvoices, overallTotal);
    }

    public void printDetailReports(List<Invoice> invoices) {
        System.out.println("INVOICE DETAILS");
        System.out.println("===============");
        
        for (Invoice inv : invoices) {
            System.out.println("Invoice#  " + inv.getUuid());
            System.out.println("Date      " + inv.getDate());
            System.out.println("Customer: ");
            System.out.println(inv.getCustomer().getName() + " (" + inv.getCustomer().getUuid() + ")");
            System.out.println(inv.getCustomer().getContact().getLastName() + ", " + inv.getCustomer().getContact().getFirstName());
            // Assuming Person has a getEmails method
            System.out.println("\t" + inv.getCustomer().getContact().getEmailList());
            System.out.println();
            System.out.println("\t" + inv.getCustomer().getAddress().getStreet());
            System.out.println("\t" + inv.getCustomer().getAddress().getCity() + " " + inv.getCustomer().getAddress().getState() + " " + inv.getCustomer().getAddress().getZip());
            
            System.out.println("Sales Person: ");
            System.out.println(inv.getSalesperson().getLastName() + ", " + inv.getSalesperson().getFirstName());

            System.out.printf("Items (%d) %60s %11s\n", inv.getItems().size(), "Tax", "Total");
            System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-                          -=-=-=-=-=- -=-=-=-=-=-");

            for (Item item : inv.getItems()) {
                String typeStr = item.getType(); // e.g., "Purchase", "Lease", "Service", "License"
                
                System.out.printf("%-36s (%-10s) %s\n", item.getUuid(), typeStr, item.getName());
                System.out.print(item.getDetails()); 
                System.out.printf("\n%70s %11.2f $ %10.2f\n", "$", item.getTax(), item.getTotal());
            }

            System.out.println("                                                             -=-=-=-=-=- -=-=-=-=-=-");
            System.out.printf("%50s Subtotals $ %10.2f $ %10.2f\n", "", inv.getTotalTax(), inv.getSubTotal());
            System.out.printf("%50s Grand Total             $ %10.2f\n\n", "", inv.getGrandTotal());
        }
    }
}
