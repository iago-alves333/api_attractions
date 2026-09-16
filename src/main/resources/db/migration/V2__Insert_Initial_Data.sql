-- Inserção de Usuários Iniciais
-- A senha para todos é: senha123
INSERT INTO public.users (id, created_at, email, name, password, role) VALUES 
('11111111-1111-1111-1111-111111111111', NOW(), 'admin@titureco.com', 'Administrador', '$2a$10$eyjovCRHtOZPdHyiLnseAepV5AcXLU1a6zVIcxzlouR8XUis7yyBq', 'ADMIN'),
('22222222-2222-2222-2222-222222222222', NOW(), 'guia1@titureco.com', 'João Guia', '$2a$10$eyjovCRHtOZPdHyiLnseAepV5AcXLU1a6zVIcxzlouR8XUis7yyBq', 'GUIDE'),
('33333333-3333-3333-3333-333333333333', NOW(), 'guia2@titureco.com', 'Maria Guia', '$2a$10$eyjovCRHtOZPdHyiLnseAepV5AcXLU1a6zVIcxzlouR8XUis7yyBq', 'GUIDE'),
('44444444-4444-4444-4444-444444444444', NOW(), 'turista1@titureco.com', 'Pedro Turista', '$2a$10$eyjovCRHtOZPdHyiLnseAepV5AcXLU1a6zVIcxzlouR8XUis7yyBq', 'TOURIST'),
('55555555-5555-5555-5555-555555555555', NOW(), 'turista2@titureco.com', 'Ana Turista', '$2a$10$eyjovCRHtOZPdHyiLnseAepV5AcXLU1a6zVIcxzlouR8XUis7yyBq', 'TOURIST');

-- Inserção de Atrações Iniciais
INSERT INTO public.attractions (id, available_spots, description, location, price, title, version, guide_id, rating_average, review_count) VALUES 
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 20, 'Passeio histórico pelo centro da cidade', ST_SetSRID(ST_MakePoint(-34.8828, -7.1150), 4326), 50.00, 'Centro Histórico', 0, '22222222-2222-2222-2222-222222222222', 0.0, 0),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 15, 'Trilha com cachoeira no final', ST_SetSRID(ST_MakePoint(-34.8500, -7.1500), 4326), 80.00, 'Trilha da Cachoeira', 0, '22222222-2222-2222-2222-222222222222', 0.0, 0),
('cccccccc-cccc-cccc-cccc-cccccccccccc', 10, 'Passeio de barco pelas piscinas naturais', ST_SetSRID(ST_MakePoint(-34.8213, -7.1350), 4326), 120.00, 'Piscinas Naturais', 0, '33333333-3333-3333-3333-333333333333', 0.0, 0);

-- Inserção de Avaliações Iniciais (Reviews)
INSERT INTO public.reviews (id, comment, created_at, rating, attraction_id, tourist_id) VALUES 
('dddddddd-dddd-dddd-dddd-dddddddddddd', 'Excelente passeio, o guia João foi muito atencioso!', NOW(), 5, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '44444444-4444-4444-4444-444444444444'),
('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'Passeio legal, mas choveu no dia.', NOW(), 4, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '55555555-5555-5555-5555-555555555555');

-- Atualizando a média de notas da primeira atração
UPDATE public.attractions SET rating_average = 4.5, review_count = 2 WHERE id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';

-- Mais Atrações Reais (João Pessoa e Região)
INSERT INTO public.attractions (id, available_spots, description, location, price, title, version, guide_id, rating_average, review_count) VALUES 
('f0000000-f000-f000-f000-f00000000000', 50, 'Visita ao ponto mais oriental das Américas, onde o sol nasce primeiro no continente.', ST_SetSRID(ST_MakePoint(-34.7977, -7.1491), 4326), 0.00, 'Farol do Cabo Branco', 0, '22222222-2222-2222-2222-222222222222', 0.0, 0),
('f1111111-f111-f111-f111-f11111111111', 100, 'Famoso pôr do sol na Praia do Jacaré ao som do Bolero de Ravel tocado no saxofone.', ST_SetSRID(ST_MakePoint(-34.8569, -7.0366), 4326), 25.00, 'Pôr do Sol do Jacaré', 0, '33333333-3333-3333-3333-333333333333', 0.0, 0),
('f2222222-f222-f222-f222-f22222222222', 40, 'Passeio de catamarã até o banco de areia avermelhada que aparece na maré baixa.', ST_SetSRID(ST_MakePoint(-34.8197, -7.0258), 4326), 45.00, 'Ilha de Areia Vermelha', 0, '33333333-3333-3333-3333-333333333333', 0.0, 0),
('f3333333-f333-f333-f333-f33333333333', 200, 'O Parque Zoobotânico Arruda Câmara, conhecido como Bica, é um oásis verde no meio da cidade.', ST_SetSRID(ST_MakePoint(-34.8763, -7.1205), 4326), 2.00, 'Parque da Bica', 0, '22222222-2222-2222-2222-222222222222', 0.0, 0),
('f4444444-f444-f444-f444-f44444444444', 30, 'Passeio pelo litoral sul passando pelas famosas praias de Tambaba, Coqueirinho e Tabatinga.', ST_SetSRID(ST_MakePoint(-34.8000, -7.3000), 4326), 150.00, 'Praias do Litoral Sul', 0, '22222222-2222-2222-2222-222222222222', 0.0, 0);
