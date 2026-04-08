package com.cinco;
/*
 * A utility class that parses CSV files to instantiate Company objects by mapping raw text to 
 * structured data and resolving contact person dependencies.
 */

import java.io.*;
import java.util.*;

public class CompanyReader {

    /**
     * Reads companies from a CSV file.
     * @param filePath Path to Companies.csv
     * @param personsMap Map of Person UUID -> Person (for contact lookup)
     * @return List of Company objects
     * @throws Exception if file reading fails
     */
	
    public static List<Company> read(String filePath, Map<String, Person> personsMap) throws Exception {
        List<Company> companies = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine(); // skip header

            while ((line = br.readLine()) != null) {
                String[] p = line.split(",", -1);

                String uuid = p[0];
                String contactUuid = p[1];
                String name = p[2];
                String street = p[3];
                String city = p[4];
                String state = p[5];
                String zip = p[6];

                Person contact = personsMap.get(contactUuid);
                if (contact == null) {
                    throw new IllegalArgumentException("Contact UUID not found: " + contactUuid);
                }

                Address address = new Address(street, city, state, zip);

                companies.add(new Company(uuid, contact, name, address));
            }
        }
        return companies;
    }
}
