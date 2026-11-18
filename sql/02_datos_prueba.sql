UPDATE usuarios
   SET password_hash = '$2a$10$xOUzH6ZX7lBv1g8qzAcZG.Dsf26x82/Au3IVJn4fvxEK6KIm15iKq';

UPDATE productos SET imagen_url = 'img/laptop.jpg'          WHERE sku = 'ELEC-001';
UPDATE productos SET imagen_url = 'img/mouse.jpg'           WHERE sku = 'ELEC-002';
UPDATE productos SET imagen_url = 'img/audifonos.jpg'       WHERE sku = 'ELEC-003';
UPDATE productos SET imagen_url = 'img/celular.jpg'         WHERE sku = 'ELEC-005';
UPDATE productos SET imagen_url = 'img/playera-negra.jpg'   WHERE sku = 'ROPA-001';
UPDATE productos SET imagen_url = 'img/playera-bordada.jpg' WHERE sku = 'ROPA-002';
UPDATE productos SET imagen_url = 'img/tenis.jpg'           WHERE sku = 'CALZ-001';
UPDATE productos SET imagen_url = 'img/lampara.jpg'         WHERE sku = 'HOG-002';
UPDATE productos SET imagen_url = 'img/cafe.jpg'            WHERE sku = 'ALIM-001';
UPDATE productos SET imagen_url = 'img/mezcal.jpg'          WHERE sku = 'BEB-001';
UPDATE productos SET imagen_url = 'img/tarjeta-regalo.jpg'  WHERE sku IN ('DIG-500', 'DIG-1000');
