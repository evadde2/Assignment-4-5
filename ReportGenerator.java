package com.cinco;

import java.util.List;

public class ReportGenerator {

    public static String generateReports(List<Invoice> invoices) {
        StringBuilder sb = new StringBuilder();
        String border = "+-------------------------------------------------------------------------+";

        // --- REPORT 1: Invoices by Total ---
        SortedList<Invoice> report1 = new SortedList<>((a, b) -> {
            int res = Double.compare(b.getGrandTotal(), a.getGrandTotal());
            return (res != 0) ? res : a.getUuid().compareTo(b.getUuid());
        });
        for (Invoice inv : invoices) report1.add(inv);

        sb.append(border).append("\n| Invoices by Total                                                       |\n").append(border).append("\n");
        sb.append(String.format("%-36s %-25s %-10s\n", "Invoice", "Customer", "Total"));
        for (Invoice inv : report1) {
            sb.append(String.format("%-36s %-25s $ %10.2f\n", 
                inv.getUuid(), inv.getCustomer().getName(), inv.getGrandTotal()));
        }

        // --- REPORT 2: Invoices by Customer ---
        SortedList<Invoice> report2 = new SortedList<>((a, b) -> {
            int res = a.getCustomer().getName().compareToIgnoreCase(b.getCustomer().getName());
            return (res != 0) ? res : a.getUuid().compareTo(b.getUuid());
        });
        for (Invoice inv : invoices) report2.add(inv);

        sb.append("\n").append(border).append("\n| Invoices by Customer                                                    |\n").append(border).append("\n");
        sb.append(String.format("%-36s %-25s %-10s\n", "Invoice", "Customer", "Total"));
        for (Invoice inv : report2) {
            sb.append(String.format("%-36s %-25s $ %10.2f\n", 
                inv.getUuid(), inv.getCustomer().getName(), inv.getGrandTotal()));
        }

        // --- REPORT 3: Customer Totals ---
        SortedList<Company> report3 = new SortedList<>((a, b) -> {
            double totalA = sumInvoices(a, invoices);
            double totalB = sumInvoices(b, invoices);
            int res = Double.compare(totalA, totalB); 
            return (res != 0) ? res : a.getName().compareToIgnoreCase(b.getName());
        });
        
        // Only adding companies that appear on invoices to avoid breaking current DataLoader logic
        for (Invoice inv : invoices) {
            if (!listContainsCompany(report3, inv.getCustomer())) {
                report3.add(inv.getCustomer());
            }
        }

        sb.append("\n").append(border).append("\n| Customer Invoice Totals                                                 |\n").append(border).append("\n");
        sb.append(String.format("%-25s %-20s %-10s\n", "Customer", "Number of Invoices", "Total"));
        for (Company c : report3) {
            sb.append(String.format("%-25s %-20d $ %10.2f\n", 
                c.getName(), countInvoices(c, invoices), sumInvoices(c, invoices)));
        }

        return sb.toString();
    }

    private static double sumInvoices(Company c, List<Invoice> invs) {
        double s = 0;
        for (Invoice i : invs) if (i.getCustomer().getUuid().equals(c.getUuid())) s += i.getGrandTotal();
        return s;
    }

    private static int countInvoices(Company c, List<Invoice> invs) {
        int ct = 0;
        for (Invoice i : invs) if (i.getCustomer().getUuid().equals(c.getUuid())) ct++;
        return ct;
    }

    private static boolean listContainsCompany(SortedList<Company> list, Company c) {
        for (Company existing : list) {
            if (existing.getUuid().equals(c.getUuid())) return true;
        }
        return false;
    }
}
