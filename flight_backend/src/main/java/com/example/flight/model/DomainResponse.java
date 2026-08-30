package com.example.flight.model;

public class DomainResponse {

    private String name;
    private Domain value;

    public DomainResponse() {
    }

    public DomainResponse(String name, Domain value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Domain getValue() {
        return value;
    }

    public void setValue(Domain value) {
        this.value = value;
    }
}
