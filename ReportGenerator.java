package com.cinco;

import java.util.*;

public class ReportGenerator {

    public static String generateReports(List<Invoice> invoices) {
        if (invoices == null || invoices.isEmpty()) {
            return "No invoices found.";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // 1. Summary Report (Sorted by Total Descending)
        List<Invoice> summaryList = new ArrayList<>(invoices);
        summaryList.sort((a, b) -> Double.compare(b.getGrandTotal(), a.getGrandTotal()));
        sb.append(getSummaryReport(summaryList));
        sb.append("\n\n");

        // 2. Company Summary Report (Alphabetical)
        sb.append(getCompanySummaryReport(summaryList));
        sb.append("\n\n");

        // 3. Detail Reports
        sb.append(getDetailReports(summaryList));

        return sb.toString();
    }

    private static String getSummaryReport(List<Invoice> invoices) {
        StringBuilder sb = new StringBuilder();
        sb.append("+--------------------------------------------------------------------------------------------------------+\n");
        sb.append("| Summary Report - By Total                                                                              |\n");
        sb.append("+--------------------------------------------------------------------------------------------------------+\n");
        // Header widths: 36, 30, 10, 11, 11
        sb.append(String.format("%-36s %-30s %-10s %11s %11s\n", "Invoice #", "Customer", "Num Items", "Tax", "Total"));

        double totalTax = 0, totalAmount = 0;
        int totalItems = 0;

        for (Invoice inv : invoices) {
            sb.append(String.format("%-36s %-30s %-10d $ %10.2f $ %10.2f\n",
                    inv.getUuid(), 
                    truncate(inv.getCustomer().getName(), 30), 
                    inv.getItems().size(), 
                    inv.getTotalTax(), 
                    inv.getGrandTotal()));

            totalTax += inv.getTotalTax();
            totalAmount += inv.getGrandTotal();
            totalItems += inv.getItems().size();
        }
        sb.append("+--------------------------------------------------------------------------------------------------------+\n");
        // Footer: Space(68) + NumItems(10) + Tax + Total
        sb.append(String.format("%-68s %-10d $ %10.2f $ %10.2f", "", totalItems, totalTax, totalAmount));
        return sb.toString();
    }

    private static String getCompanySummaryReport(List<Invoice> invoices) {
        StringBuilder sb = new StringBuilder();
        Map<String, Integer> counts = new TreeMap<>();
        Map<String, Double> totals = new TreeMap<>();

        for (Invoice inv : invoices) {
            String name = inv.getCustomer().getName();
            counts.put(name, counts.getOrDefault(name, 0) + 1);
            totals.put(name, totals.getOrDefault(name, 0.0) + inv.getGrandTotal());
        }

        sb.append("+----------------------------------------------------------------+\n");
        sb.append("| Company Invoice Summary Report                                 |\n");
        sb.append("+----------------------------------------------------------------+\n");
        sb.append(String.format("%-30s %-10s %-15s\n", "Company", "# Invoices", "Grand Total"));

        double overallTotal = 0;
        int overallInvoices = 0;

        for (String name : counts.keySet()) {
            sb.append(String.format("%-30s %-10d $ %10.2f\n", name, counts.get(name), totals.get(name)));
            overallTotal += totals.get(name);
            overallInvoices += counts.get(name);
        }
        sb.append("+----------------------------------------------------------------+\n");
        sb.append(String.format("%-31s %-10d $ %10.2f", "", overallInvoices, overallTotal));
        return sb.toString();
    }

    private static String getDetailReports(List<Invoice> invoices) {
        StringBuilder sb = new StringBuilder();
        sb.append("INVOICE DETAILS\n===============\n");
        for (Invoice inv : invoices) {
            sb.append("Invoice#  ").append(inv.getUuid()).append("\n");
            sb.append("Date      ").append(inv.getDate()).append("\n");
            
            // Customer Section
            sb.append("Customer: \n");
            sb.append(inv.getCustomer().getName()).append(" (").append(inv.getCustomer().getUuid()).append(")\n");
            
            // Print Contact Person (Name, UUID, Emails)
            Person c = inv.getCustomer().getContact();
            sb.append(c.getLastName()).append(", ").append(c.getFirstName());
            sb.append(" (").append(c.getUuid()).append(")\n");
            sb.append("\t").append(c.getEmailList()).append("\n\n"); // Using Person's bracketed list

            // Print Company Address
            if (inv.getCustomer().getAddress() != null) {
                Address a = inv.getCustomer().getAddress();
                sb.append("\t").append(a.getStreet()).append("\n");
                sb.append("\t").append(a.getCity()).append(" ").append(a.getState()).append(" ").append(a.getZip()).append("\n");
            }

            // Sales Person Section
            sb.append("Sales Person: \n");
            // Leverages Person.toString() for name, UUID, emails, and personal address if present
            sb.append(inv.getSalesperson().toString()).append("\n");

            // Items Section
            int itemCount = inv.getItems().size();
            sb.append(String.format("Items (%d) %60s %11s\n", itemCount, "Tax", "Total"));
            sb.append("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-                          -=-=-=-=-=- -=-=-=-=-=-\n");

            for (Item item : inv.getItems()) {
                String typeLabel = getItemTypeLabel(item);
                sb.append(String.format("%-36s %s%s\n", item.getUuid(), typeLabel, item.getName()));
                
                String details = item.getDetails();
                if (details != null && !details.isEmpty()) {
                    sb.append("\t").append(details.trim().replace("\n", "\n\t")).append("\n");
                }
                
                sb.append(String.format("%61s %10.2f $ %10.2f\n", "$", item.getTax(), item.getCost()));
            }

            sb.append("                                                             -=-=-=-=-=- -=-=-=-=-=-\n");
            sb.append(String.format("%50s Subtotals $ %10.2f $ %10.2f\n", "", inv.getTotalTax(), inv.getSubTotal()));
            sb.append(String.format("%50s Grand Total             $ %10.2f\n\n", "", inv.getGrandTotal()));
        }
        return sb.toString();
    }

    private static String getItemTypeLabel(Item item) {
        if (item instanceof Equipment) {
            // Check if Equipment class has a method to distinguish Purchase vs Lease
            return "(Equipment) "; 
        }
        if (item instanceof Service) return "(Service) ";
        if (item instanceof License) return "(License) ";
        return "";
    }

    private static String truncate(String s, int len) {
        if (s == null) return "";
        return (s.length() <= len) ? s : s.substring(0, len);
    }
}
