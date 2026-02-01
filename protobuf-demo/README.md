# Scala Protobuf Producer/Consumer Demo

This project demonstrates two important aspects of protobuf schema evolution:

## 1. Renaming a Field
Renaming a field in the `.proto` file (changing the field name but keeping the same field number)
is not a breaking change.

## 2. Using Optional Fields
Adding or using `optional` fields in protobuf is wire-compatible; older code can read messages
without the new field, and newer code can handle messages where the field is absent. However, the presence or absence
of the field can change application semantics.

## Video Demonstration

*Note: The following video demonstration is presented in **Urdu**.*

[Watch the demo on YouTube](https://youtu.be/G_0aAKUuIY4)

## Building the Project
The project uses sbt to compile Scala code and generate protobuf classes.

- To compile everything:
  ```bash
  sbt pack
  ```

## Running the Producer and Consumer

### Producer
```bash
./producer/target/pack/bin/producer
```

### Consumer
```bash
./producer/target/pack/bin/producer
```

## How Protobuf is Used
- The protobuf schema is defined in `producer/src/main/protobuf/user.proto` and `consumer/src/main/protobuf/user.proto`.
- sbt is configured to generate Scala classes from `.proto` files during the build process.
- Both Producer and Consumer use these generated classes for message serialization and deserialization.
