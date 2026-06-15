# E-commerce TCC — Sistema de Gestão de Pedidos

Backend completo com Spring Boot 3.2, H2, Spring Security e JWT.

## Stack

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 17 | Linguagem |
| Spring Boot | 3.2.0 | Framework base |
| Spring Security | 6.x | Autenticação e autorização |
| JJWT | 0.11.5 | Geração e validação de JWT |
| Spring Data JPA | 3.2 | ORM / Repositórios |
| H2 Database | Runtime | Banco em memória |
| Spring Validation | 3.2 | Validação de DTOs |

## Como executar

```bash
# Clone ou extraia o projeto
cd ecommerce-tcc

# Executa na porta 8080
mvn spring-boot:run

# Executa os testes
mvn test
```

### Acesso ao H2 Console
```
URL:      http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:ecommercedb
User:     sa
Password: (vazio)
```

### Usuários pré-cadastrados (data.sql)

| E-mail | Senha | Role |
|---|---|---|
| `admin@tcc.com` | `admin123` | ADMIN |
| `joao@email.com` | `cliente123` | CUSTOMER |

---

## Endpoints da API

### 🔓 Autenticação (público)

| Método | URL | Body | Descrição |
|---|---|---|---|
| POST | `/api/auth/register` | `{name, email, password}` | Cadastra usuário e retorna JWT |
| POST | `/api/auth/login` | `{email, password}` | Autentica e retorna JWT |

**Exemplo de registro:**
```json
POST /api/auth/register
{
  "name": "Maria Silva",
  "email": "maria@email.com",
  "password": "minhasenha"
}
```
**Resposta:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "userId": 3,
  "name": "Maria Silva",
  "email": "maria@email.com",
  "role": "CUSTOMER"
}
```

> Para todas as chamadas autenticadas, envie o header:
> `Authorization: Bearer <token>`

---

### 📦 Produtos

| Método | URL | Auth | Descrição |
|---|---|---|---|
| GET | `/api/products` | Público | Lista todos os produtos |
| GET | `/api/products/{id}` | Público | Busca produto por ID |
| GET | `/api/products/search?name=X` | Público | Busca por nome |
| POST | `/api/products` | ADMIN | Cadastra novo produto |
| PUT | `/api/products/{id}` | ADMIN | Atualiza produto |
| DELETE | `/api/products/{id}` | ADMIN | Remove produto |

**Body para criação/atualização:**
```json
{
  "name": "Notebook Dell",
  "description": "Intel i7, 16GB RAM",
  "price": 3499.90,
  "stockQuantity": 15
}
```

---

### 🛒 Pedidos

| Método | URL | Auth | Descrição |
|---|---|---|---|
| POST | `/api/orders` | Autenticado | Cria pedido (desconta estoque) |
| GET | `/api/orders/my-orders` | Autenticado | Lista pedidos do usuário logado |
| GET | `/api/orders/{id}` | Autenticado | Busca pedido por ID |
| GET | `/api/orders` | ADMIN | Lista todos os pedidos |
| PATCH | `/api/orders/{id}/status` | ADMIN | Atualiza status |
| PATCH | `/api/orders/{id}/cancel` | Autenticado | Cancela pedido |

**Body para criar pedido:**
```json
POST /api/orders
{
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 3, "quantity": 1 }
  ]
}
```

**Body para atualizar status:**
```json
PATCH /api/orders/1/status
{
  "newStatus": "CONFIRMADO"
}
```

### Fluxo de Status

```
PENDENTE ──► CONFIRMADO ──► ENVIADO ──► ENTREGUE
    │               │
    └───────────────┴──► CANCELADO (terminal)
```

Tentativas de transição fora do fluxo retornam `422 Unprocessable Entity`.

---

### 📊 Relatórios (somente ADMIN)

| Método | URL | Parâmetros | Descrição |
|---|---|---|---|
| GET | `/api/reports/sales` | `startDate`, `endDate` | Relatório de vendas por período |

**Exemplo:**
```
GET /api/reports/sales?startDate=2024-01-01&endDate=2024-12-31
Authorization: Bearer <admin_token>
```

**Resposta:**
```json
{
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "totalOrders": 42,
  "deliveredOrders": 35,
  "canceledOrders": 3,
  "totalRevenue": 87540.50,
  "orders": [ ... ]
}
```

---

### 👤 Usuários

| Método | URL | Auth | Descrição |
|---|---|---|---|
| GET | `/api/users/me` | Autenticado | Perfil do usuário logado |
| GET | `/api/users` | ADMIN | Lista todos os usuários |
| GET | `/api/users/{id}` | ADMIN | Busca usuário por ID |
| DELETE | `/api/users/{id}` | ADMIN | Remove usuário |

---

## Tratamento de Erros

Todas as respostas de erro seguem o formato padronizado:

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Produto não encontrado com id: 99"
}
```

Erros de validação de DTO retornam os campos com problema:

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "message": "Erro de validação nos campos enviados.",
  "errors": {
    "price": "O preço deve ser maior que zero.",
    "name": "O nome do produto é obrigatório."
  }
}
```

| Exceção | Status HTTP |
|---|---|
| `ProductNotFoundException` | 404 |
| `OrderNotFoundException` | 404 |
| `InsufficientStockException` | 422 |
| `InvalidStatusTransitionException` | 422 |
| `BadCredentialsException` | 401 |
| `AccessDeniedException` | 403 |
| Erros de validação (`@Valid`) | 400 |

---

## Arquitetura

```
Controller → Service → Repository → Entity
               ↓
             DTOs (nunca entidades expostas na API)
```

### Pacotes

```
com.pucpr.tcc.monolith
├── product/       Cadastro e estoque de produtos
├── order/         Pedidos, itens e fluxo de status
├── user/          Cadastro, autenticação e perfil
├── report/        Relatórios de vendas por período
├── security/      JWT (JwtService, Filter, SecurityConfig)
└── exception/     GlobalExceptionHandler centralizado
```

### Segurança JWT

1. Cliente faz `POST /api/auth/login` → recebe token Bearer
2. Inclui `Authorization: Bearer <token>` em cada requisição
3. `JwtAuthenticationFilter` intercepta, valida o token e injeta a autenticação no `SecurityContext`
4. Controllers recebem o usuário via `@AuthenticationPrincipal User currentUser`
