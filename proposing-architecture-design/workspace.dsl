workspace {

    model {
        user = person "User" "Consumer who uses the online shopping system." {
            tags "user"
        }

        paymentSystem = softwareSystem "Third-party payment integration" "Using Stripe for managing payments." {
            tags "external"
        }

        onlineShoppingSystem = softwareSystem "Online shopping system" "A system that allows users to browse products, place orders, and manage their accounts." {

            webApplication = container "Web Application" "Delivers the static content and the Online shopping single page application." "Frontend app" {
                tags "frontend"
            }
            apiApplication = container "REST APIs" "Provides Online shopping functionality via a JSON/HTTPS API." "Scala Play REST API" {
                tags "backend"
            }

            psql = container "MySQL" {
                tags "db"
            }
        }

        # relationships between people and software systems
        user -> onlineShoppingSystem "Uses"
        onlineShoppingSystem -> paymentSystem "Integrates with"

        # relationships to/from containers
        user -> webApplication "browse webapp using" "HTTPS"
        webApplication -> apiApplication "consumes REST API" "JSON/HTTPS"
        apiApplication -> psql "reads from and writes to" "JDBC/SQL"
        apiApplication -> paymentSystem "integrates with"

    }

    views {
        systemContext onlineShoppingSystem "SystemContext" {
            include *
            autolayout lr
        }

        container onlineShoppingSystem "container-diagram" {
            include *
            autolayout lr
        }

        theme default

        styles {

            element "user" {
                shape person
                background #08427b
                color #ffffff
            }

            element "db" {
                shape cylinder
                background #eb2f1a
                color #FFFFFF
            }

            element "external" {
                background #878684
                color #FFFFFF
            }
        }
    }

}