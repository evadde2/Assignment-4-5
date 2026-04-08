package com.cinco;

import java.io.*;
import java.util.*;

/*
 * A utility class that parses CSV files to extract individual contact details and assemble a 
 * collection of Person objects for the system.
 */
public class PersonReader {

	/*
	 * Reads a file line-by-line, splits data by commas to extract names and IDs, and handles 
	 * variable email counts to build a list of Person instances.
	 */
    public static List<Person> read(String filePath) throws Exception {
        List<Person> persons = new ArrayList<>();

        BufferedReader br = new BufferedReader(new FileReader(filePath));
        br.readLine(); 

        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",");

            String uuid = parts[0];
            String first = parts[1];
            String last = parts[2];
            String phone = parts[3]; 

            List<String> emails = new ArrayList<>();
            for (int i = 4; i < parts.length; i++) {
                emails.add(parts[i]);
            }

            persons.add(new Person(uuid, first, last, phone, emails));
        }

        br.close();
        return persons;
    }
}
