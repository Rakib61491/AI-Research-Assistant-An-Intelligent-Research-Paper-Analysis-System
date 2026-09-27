package com.example.airesearchassistant.model;

/**
 * Person — plain POJO for C1 TableView demo.
 * No JavaFX imports (Hard Rule 2). Works with
 * PropertyValueFactory via standard JavaBean getters.
 */
public class Person {

    private final String firstName;
    private final String lastName;
    private final int    age;

    public Person(String firstName, String lastName, int age) {
        this.firstName = firstName;
        this.lastName  = lastName;
        this.age       = age;
    }

    public String getFirstName() { return firstName; }
    public String getLastName()  { return lastName;  }
    public int    getAge()       { return age;        }
}
