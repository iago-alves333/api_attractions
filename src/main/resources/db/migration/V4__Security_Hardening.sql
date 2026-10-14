-- 1. Remove dados de demonstração da migration legada V2 em bancos que já a executaram
DELETE FROM reviews WHERE tourist_id IN (
    '11111111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222222',
    '33333333-3333-3333-3333-333333333333',
    '44444444-4444-4444-4444-444444444444',
    '55555555-5555-5555-5555-555555555555'
) OR attraction_id IN (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    'f0000000-f000-f000-f000-f00000000000',
    'f1111111-f111-f111-f111-f11111111111',
    'f2222222-f222-f222-f222-f22222222222',
    'f3333333-f333-f333-f333-f33333333333',
    'f4444444-f444-f444-f444-f44444444444'
);

DELETE FROM reservations WHERE tourist_id IN (
    '11111111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222222',
    '33333333-3333-3333-3333-333333333333',
    '44444444-4444-4444-4444-444444444444',
    '55555555-5555-5555-5555-555555555555'
) OR attraction_id IN (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    'f0000000-f000-f000-f000-f00000000000',
    'f1111111-f111-f111-f111-f11111111111',
    'f2222222-f222-f222-f222-f22222222222',
    'f3333333-f333-f333-f333-f33333333333',
    'f4444444-f444-f444-f444-f44444444444'
);

DELETE FROM attractions WHERE guide_id IN (
    '11111111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222222',
    '33333333-3333-3333-3333-333333333333',
    '44444444-4444-4444-4444-444444444444',
    '55555555-5555-5555-5555-555555555555'
);

DELETE FROM refresh_tokens WHERE user_id IN (
    '11111111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222222',
    '33333333-3333-3333-3333-333333333333',
    '44444444-4444-4444-4444-444444444444',
    '55555555-5555-5555-5555-555555555555'
);

DELETE FROM users WHERE id IN (
    '11111111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222222',
    '33333333-3333-3333-3333-333333333333',
    '44444444-4444-4444-4444-444444444444',
    '55555555-5555-5555-5555-555555555555'
);

-- 2. Limpa todos os refresh tokens legados em texto puro para forçar reautenticação segura
DELETE FROM refresh_tokens;

-- 3. Remove tabela de blacklist do banco (migrada para o Redis com TTL automático)
DROP TABLE IF EXISTS token_blacklist;
