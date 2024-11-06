# DBDAA: Dual Blockchain and Decentralized Identifiers Assisted Anonymous Authentication for Building IoT

This repository contains the code implementation for the paper titled "**DBDAA: Dual Blockchain and Decentralized Identifiers Assisted Anonymous Authentication for Building IoT**." This project utilizes a dual blockchain architecture, decentralized identifiers (DIDs), and cryptographic methods to enable anonymous authentication in IoT environments.

---

## Main Structure

- **Authentication**: Contains Java implementations for cryptographic operations required for the roles in the DBDAA model.
  
- **Proverif**: Includes Proverif scripts used for formal verification of the DBDAA authentication protocols.
  
- **HyperledgerFabric**: Holds the code for the blockchain implementation using Hyperledger Fabric, which supports the dual blockchain aspect of DBDAA.
---

## Authentication Module

This folder contains a Java Spring Boot application that provides cryptographic operations and authentication services for the DBDAA model. This module supports various roles in the DBDAA system, enabling functions required for IoT environments.


### Prerequisites

- Java 11 or higher
- Maven (for project build and dependency management)
- Spring Boot framework


### Folder Structure

- `src/main/java`: Contains Java source code for the cryptographic operations and Spring Boot controllers for the DBDAA model.
- `src/main/resources`: Contains configuration files, including `application.properties` for setting up environment variables and parameters.
- `pom.xml`: Maven configuration file for managing dependencies.


### Installation and Setup

1. **Clone the Repository**:
   ```
   git clone https://github.com/PeterZ121/DBDAA.git
   cd DBDAA/Authentication
   ```

2. **Build the Project**:
   Use Maven to compile and package the application:
   ```
   mvn clean install
   ```

3. **Configure Application Properties**:
   Open `src/main/resources/application.yml` to set any required configuration settings. These may include server ports, database settings (if any), and other environment variables relevant to the authentication processes.


### Running the Application

Start the Spring Boot application using Maven:

```
mvn spring-boot:run
```

The application will start on the default port (9002). You can change the port in the `application.yml` file if needed.


### Testing

You can test the endpoints using a tool like Postman or cURL.

For further details, refer to the source code within each Java class.

---

## Proverif Module

The **Proverif** module contains scripts written in the Proverif language, designed to formally verify the authentication protocols in the DBDAA.

Codes and Testing Protocol:
- `deviceAuth.pv`: This script verifies the device authentication protocol in the DBDAA model.
- `userAnoAuth.pv`: This script verifies the user anonymous authentication protocol in the DBDAA model.

Obtaining Results: To obtain results for these tests, simply copy and paste the respective code into the ProVerif tool available at [Proverif's official website](https://proverif.inria.fr/) and run them separately. We appreciate your interest and collaboration!

---


## Acknowledgements
We gratefully thank the authors from reedsolo for open-sourcing their code.

We would like to thank the reviewers for their careful reading and comments on our manuscript, in order to facilitate the better presentation of our paper.

The project is funded in part by the National Natural Science Foundation of China (Grant No.62071111) and atural Science Foundation of Xinjiang Uygur Autonomous Region (Grant No.2023D01A63).
