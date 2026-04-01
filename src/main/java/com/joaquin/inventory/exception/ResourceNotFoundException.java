package com.joaquin.inventory.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " with id " + id + " not found");
    }

    public ResourceNotFoundException(String resource, String identifierName, String identifierValue) {
        super(resource + " with " + identifierName + " '" + identifierValue + "' not found");
    }
}
