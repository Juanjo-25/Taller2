-- Ejecutar en Supabase antes de iniciar la aplicación con el nuevo rol.
BEGIN;
ALTER TABLE yate.login DROP CONSTRAINT IF EXISTS login_rol_check;
ALTER TABLE yate.login ADD CONSTRAINT login_rol_check
    CHECK (rol IN ('SUPER_ADMIN', 'ADMIN', 'CLIENTE'));
COMMIT;
-- Para promover una cuenta elegida, sustituir el correo y ejecutar:
-- UPDATE yate.login SET rol = 'SUPER_ADMIN'
-- WHERE email = 'correo-del-administrador' AND activo = true AND rol = 'ADMIN';
-- Cerrar sesión y volver a ingresar después del cambio.
