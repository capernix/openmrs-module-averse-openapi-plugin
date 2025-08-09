# Test Configuration Examples for Maven Plugin Parameter Testing

This directory contains example POM configurations to test different parameter passing scenarios.

## test-pom-basic.xml
Basic configuration with simple parameters.

## test-pom-advanced.xml  
Advanced configuration with all parameters including lists.

## test-pom-queue-module.xml
Realistic configuration for Queue module OpenAPI generation.

## Usage

To test these configurations:

```bash
# Test basic configuration
mvn -f test-pom-basic.xml param-test:hello

# Test advanced configuration  
mvn -f test-pom-advanced.xml param-test:hello

# Test Queue module simulation
mvn -f test-pom-queue-module.xml param-test:hello
```
