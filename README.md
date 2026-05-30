# E-commerce Monolith — TCC PUCPR

Implementação da versão **Monolítica** do experimento controlado de Engenharia de Software Experimental.

## Porta única

| Aplicação | Porta |
|---|---|
| `ecommerce-monolith` | 8080 |

## Stack
- Java 17 + Spring Boot 3.2
- H2 in-memory (banco único compartilhado por todos os contextos)
- JUnit 5 + Mockito + ArchUnit + PITest

## Estrutura de Pacotes

```
com.pucpr.tcc.ecommerce
├── product
│   ├── domain/          # Product, exceptions, ProductRepository (interface)
│   ├── application/     # ProductService
│   └── infrastructure/  # ProductController, JpaProductRepository, DTOs
├── customer
│   ├── domain/          # Customer, exceptions, CustomerRepository (interface)
│   ├── application/     # CustomerService
│   └── infrastructure/  # CustomerController, JpaCustomerRepository, DTOs
└── order
    ├── domain/          # Order, OrderItem, OrderStatus, exceptions, OrderRepository
    ├── application/     # OrderService (injeta CustomerService + ProductService via IoC)
    └── infrastructure/  # OrderController, JpaOrderRepository, DTOs
```

### Diferença chave vs Microsserviços

O `OrderService` injeta `CustomerService` e `ProductService` **diretamente na memória**
via IoC do Spring — sem nenhuma chamada HTTP. Esta é a variável experimental isolada pelo TCC.

## Como executar

```bash
mvn spring-boot:run
```

Acesse: `http://localhost:8080`
H2 Console: `http://localhost:8080/h2-console`

## Endpoints

### Produtos
| Método | URL | Descrição |
|---|---|---|
| POST | `/api/products` | Criar produto |
| GET | `/api/products` | Listar todos |
| GET | `/api/products/{id}` | Buscar por id |
| PATCH | `/api/products/{id}/stock/decrease` | Baixar estoque |
| GET | `/api/products/{id}/stock/check?quantity=N` | Verificar estoque |
| DELETE | `/api/products/{id}` | Remover produto |

### Clientes
| Método | URL | Descrição |
|---|---|---|
| POST | `/api/customers` | Criar cliente |
| GET | `/api/customers` | Listar todos |
| GET | `/api/customers/{id}` | Buscar por id |
| GET | `/api/customers/{id}/active` | Verificar se ativo |
| PATCH | `/api/customers/{id}/deactivate` | Desativar |
| PATCH | `/api/customers/{id}/activate` | Ativar |

### Pedidos
| Método | URL | Descrição |
|---|---|---|
| POST | `/api/orders/checkout` | Criar pedido (checkout completo) |
| GET | `/api/orders` | Listar todos |
| GET | `/api/orders/{id}` | Buscar por id |
| GET | `/api/orders/customer/{customerId}` | Pedidos por cliente |
| PATCH | `/api/orders/{id}/cancel` | Cancelar |
| PATCH | `/api/orders/{id}/pay` | Pagar |

## Testes

```bash
# Unitários + ArchUnit (varredura global única — RQ2)
mvn test

# Mutation score — todos os contextos em uma só execução (RQ3)
mvn pitest:mutationCoverage
# Relatório: target/pit-reports/index.html
```

## Regras de Negócio (alvos PITest)

- `Product.decreaseStock()` → `InsufficientStockException` se qty > stock
- `Product` constructor/`updatePrice()` → `InvalidProductException` se price ≤ 0
- `Customer.validateEmail()` → `InvalidEmailException` para formato inválido
- `Customer.validateIsActive()` → `InactiveCustomerException` se active = false
- `Order.calculateTotal()` → desconto 10% se subtotal > R$500
- `OrderService.checkout()` → bloqueia cliente inativo e produto sem estoque
