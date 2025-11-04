package com.devsuperior.dscommerce.services.exceptions;

// Extendemos a RuntimeException, pois ele não exige o try catch
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
