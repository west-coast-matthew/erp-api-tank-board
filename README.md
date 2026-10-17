# ERP Tank Board API (Java)

## Description
As part of the ERP demo application, this project provides the API behind the tank board screens: the tank boards (facility maps) a user has, the shared templates available to them, and managing their cloned boards (cloning, saving settings, renaming and deleting).

See `CLAUDE.md` for the endpoints and the rules they follow.

## Building the application

This project relies on the `erp-core-java` Maven module, which needs to be built first from that project:

`mvn clean install`

Then build and test this project:

`mvn package`

## Starting the application
Copy `.env.example` to `.env`, start the databases from `erp-docker-deps`, then:

	mvn spring-boot:run

The API listens on port 5184.
