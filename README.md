# Nüber — Ride-Booking System

**Java concurrency simulation and conceptual AWS cloud architecture**

Nüber is a university project exploring two aspects of a ride-hailing platform: designing a cloud-based architecture and implementing a concurrent booking simulation. These were **separate coursework stages**: the AWS design was conceptual and **was not deployed or used by the Java program**.

## Project components

### 1. AWS architecture design (conceptual)

The [architecture report](architecture/nuber-aws-architecture.pdf) proposes an AWS-based ride-hailing platform with:

- API access and protection using Route 53, CloudFront, WAF and API Gateway.
- Authentication and role management using Amazon Cognito.
- Containerized backend microservices using ECS Fargate.
- Data storage using Aurora PostgreSQL, DynamoDB, Redis and S3 for different access patterns.
- Asynchronous workloads and analytics using EventBridge, SQS, Kinesis and related services.
- A proposed multi-region deployment strategy with considerations for privacy, security, scalability, monitoring and costs.
- External integrations such as Stripe and Google Maps.

This is a **design exercise**, not an implementation or proof of production experience with those services.

### 2. Java concurrent booking simulation

The Java implementation models passengers requesting rides in multiple regions, drivers being allocated to bookings, and completed journeys returning results.

Core implementation features:

- A shared, thread-safe `LinkedBlockingQueue<Driver>` of available drivers.
- Region-specific `Semaphore` limits on simultaneous bookings.
- `ExecutorService` workers to process bookings asynchronously.
- `Future<BookingResult>` values for retrieving completed results.
- `AtomicInteger` counters for booking identifiers and bookings awaiting drivers.
- A shutdown mechanism to stop accepting new bookings while processing previously accepted work.

### Java classes

| Class | Responsibility |
| --- | --- |
| `NuberDispatch` | Registers regions, manages the available-driver queue and routes bookings. |
| `NuberRegion` | Schedules bookings and limits region concurrency. |
| `Booking` | Allocates a driver, simulates a trip and returns the result. |
| `Driver` / `Passenger` | Model trip participants and simulated travel delays. |
| `BookingResult` | Holds completed booking information. |
| `Simulation` / `AssignmentDriver` | Drive sample simulations and provide an entry point. |

## Running the simulation

Requires a **Java Development Kit (JDK)**. No external dependencies or build framework are included.

From the repository root, in a terminal:

```bash
mkdir -p out
javac -d out src/nuber/students/*.java
java -cp out nuber.students.AssignmentDriver
```

`AssignmentDriver.java` includes sample simulation settings. The checked-in version runs a simulation with one driver, 50 passengers and randomized delays of up to 3,000 ms; completion may therefore take some time. Adjust the enabled simulation there to try other workloads.


## Background and authorship

This project was created for university coursework. The course instructor provided the Java project scaffold, class structure and assignment requirements. **I implemented the Java simulation functionality within that scaffold and independently prepared the AWS architecture design for an earlier part of the course.** The Part 2 implementation follows the instructor-provided architecture; it does not implement the Part 1 AWS proposal.

Existing instructor attribution in source comments has been preserved. This repository is a portfolio presentation of coursework rather than a production ride-hailing service.

## Limitations and future improvements

- The project is a local simulation, with no live API, frontend, database or deployed AWS infrastructure.
- Formal automated tests are not included in the submitted files.
- Interruption, cancellation and shutdown edge cases would benefit from additional tests and refinement.
- The AWS proposal requires further validation of operational complexity, multi-region data residency and cost assumptions before real deployment.

