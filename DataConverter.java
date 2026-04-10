package com.cinco;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DataConverter {

    public static void main(String[] args) {

        try {
            // Initialize Loader and Load Persons/Companies
            DataLoader loader = new DataLoader();
            loader.loadPersons("data/Persons.csv");
            loader.loadCompanies("data/Companies.csv");

            // Read ItemTemplates from CSV
            List<ItemTemplate> templates = ItemReader.read("data/Items.csv");

            // Transform Templates into Polymorphic Item Subclasses
           
            List<Item> items = new ArrayList<>();
            for (ItemTemplate t : templates) {
                String type = t.getTypeCode();

                if (type.equals("E")) {
                    
                    items.add(new Equipment(t.getUuid(), t.getName(), t.getBaseRate()));
                } else if (type.equals("S")) {
                    
                    items.add(new Service(t.getUuid(), t.getName(), t.getBaseRate(), (Person)null, 0.0));
                } else if (type.equals("L")) {
                    
                    items.add(new License(t.getUuid(), t.getName(), t.getExtraFee(), t.getBaseRate(), (java.time.LocalDate)null,(java.time.LocalDate) null));
                }
            }

            // 4. Create output directory if it doesn't exist
            new File("data").mkdirs();

            // Run XML Serialization 
            XmlSerializer xmlSerializer = new XmlSerializer();
            xmlSerializer.serializePersons(loader.getPersons(), "data/Persons.xml");
            xmlSerializer.serializeCompanies(loader.getCompanies(), "data/Companies.xml");
            xmlSerializer.serializeItems(items, "data/Items.xml");

            // Run JSON Serialization 
            MyJsonSerializer jsonSerializer = new MyJsonSerializer();
            jsonSerializer.serializePersons(loader.getPersons(), "data/Persons.json");
            jsonSerializer.serializeCompanies(loader.getCompanies(), "data/Companies.json");
            jsonSerializer.serializeItems(items, "data/Items.json");

            System.out.println("Success: All files generated in data/ folder.");

        } catch (Exception e) {
            System.err.println("Conversion failed:");
            e.printStackTrace();
        }
    }
}
