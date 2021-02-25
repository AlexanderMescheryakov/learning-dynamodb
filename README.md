# Learning DynamoDB
## Resources
* [The DynamoDB Book](https://www.dynamodbbook.com/)
* [AWS re:Invent 2020: Data modeling with Amazon DynamoDB – Part 1](https://www.youtube.com/watch?v=fiP2e-g-r4g), [Part 2](https://www.youtube.com/watch?v=0uLF1tjI_BI)
* [AWS re:Invent 2020: Amazon DynamoDB advanced design patterns – Part 1](https://www.youtube.com/watch?v=MF9a1UNOAQo), [Part 2](https://www.youtube.com/watch?v=_KNrRdWD25M)
# Design
## Entities
![Entities table](/images/entities.png)
## Access patterns
![Access patterns table](/images/access-patterns.png)
## Singe table model
![Singe table model](/images/facets.png)
# Implementation
## DynamoDB CRUD API
There are four Java API kinds in the AWS SDK for DynamoDB:
* Low-level API v1
* High-level API v1 (DynamoDbMapper)
* Low-level API v2
* High-level API v2 (Enhanced Client)

Both low-level APIs correspond directly to DynamoDB HTTP API, but the v1 looks a bit more user friendly.
The high-level APIs has similar functionality allowing to use annotations to map entity fields with attribute names.
The main differences of the v2 APIs are the support of asynchronous calls and the optimization for AWS Lambda use cases having less initialization delays.
Generally the Enhanced Client could be the default choice. The main reason to still use low level APIs would be heterogeneous batch or transactional requests using different entity types in one call. But even in that case the high level table schema could ease the entity to attributes mapping.
## REST APIs implemented for the access patterns
The solution here provides a set of REST APIs implementing the previously defined access patterns as well the general CRUD operations.
The service implemented as a set of AWS Lambda written with Quarkus framework and building into native executables using GraalVM.
Repositories for Customer and Order entities implemented using AWS SDK v2 low-level API while the Product repostiory uses an Enhanced Client API.
![CRUD APIs](/images/crud.png)

