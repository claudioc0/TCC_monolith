-- ============================================================
-- Dados iniciais para desenvolvimento (H2 in-memory)
-- Carregado automaticamente pelo Spring Boot ao iniciar.
-- ============================================================

-- Os usuários agora são criados dinamicamente na classe DatabaseSeeder.java

-- Produtos
INSERT INTO products (name, description, price, stock_quantity, created_at, updated_at)
VALUES ('Notebook Dell Inspiron',
        'Processador Intel i7, 16GB RAM, SSD 512GB',
        3499.90, 15, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO products (name, description, price, stock_quantity, created_at, updated_at)
VALUES ('Mouse Gamer Logitech G502',
        'Mouse óptico 25600 DPI, RGB, 11 botões programáveis',
        349.90, 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO products (name, description, price, stock_quantity, created_at, updated_at)
VALUES ('Teclado Mecânico HyperX',
        'Switch Red, ABNT2, Anti-Ghosting, USB',
        489.90, 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO products (name, description, price, stock_quantity, created_at, updated_at)
VALUES ('Monitor LG 27" IPS',
        '2K QHD, 144Hz, 1ms, HDMI/DisplayPort',
        1899.90, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO products (name, description, price, stock_quantity, created_at, updated_at)
VALUES ('Headset Sony WH-1000XM5',
        'Cancelamento de ruído ativo, 30h bateria, Bluetooth 5.2',
        1499.90, 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
