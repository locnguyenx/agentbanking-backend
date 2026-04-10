-- Fix agent tier values to match the enum (PREMIUM -> PREMIER, BASIC -> MICRO)
UPDATE agent SET tier = 'PREMIER' WHERE tier = 'PREMIUM';
UPDATE agent SET tier = 'MICRO' WHERE tier = 'BASIC';
