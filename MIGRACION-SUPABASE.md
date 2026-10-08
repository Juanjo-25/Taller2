# Migrar la base local a Supabase

Las tablas de la aplicación se guardan en el esquema **yate**, según
`application-supabase.properties`. Buscar solo en `public` no las muestra.

1. Detenga la aplicación para que la base H2 no cambie durante la exportación.
2. Configure `.env` con las claves de `.env.example`, usando los datos de
   **Connect → Session pooler** del proyecto Taller2. La contraseña es la de
   PostgreSQL, no una API key. No publique `.env`.
3. Desde la raíz del proyecto, ejecute:

   ```sh
   python3 scripts/supabase.py export
   python3 scripts/supabase.py apply
   python3 scripts/supabase.py run
   ```

`export` respalda `data/yate.mv.db` en `data/backups/`, abre H2 en modo lectura
y genera `data/migracion-supabase.sql`. El SQL contiene datos privados y está
excluido de Git junto con el directorio `data`.

`apply` crea las cinco tablas, conserva IDs y relaciones, importa los registros,
comprueba los conteos y ajusta las secuencias. Todo ocurre en una transacción.
Se detiene si alguna tabla destino ya tiene registros: no sobrescribe datos.
Si ya se ejecutó correctamente, no vuelva a importar; use únicamente `run`.

`run` carga `.env` y activa el perfil `supabase`. Ejecutar `./mvnw spring-boot:run`
sin ese perfil sigue usando la base H2 local. La copia H2 se conserva.

También puede ejecutar el contenido de `data/migracion-supabase.sql` directamente
en el SQL Editor de Supabase si no dispone de acceso JDBC desde su computador.

Para verificar en el SQL Editor:

```sql
SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_schema = 'yate' AND table_type = 'BASE TABLE'
ORDER BY table_name;

SELECT 'cliente' AS tabla, count(*) AS registros FROM yate.cliente
UNION ALL SELECT 'login', count(*) FROM yate.login
UNION ALL SELECT 'producto', count(*) FROM yate.producto
UNION ALL SELECT 'encabezado', count(*) FROM yate.encabezado
UNION ALL SELECT 'detalle', count(*) FROM yate.detalle;
```

En Table Editor seleccione el esquema `yate` para ver las tablas.

## Rol superadministrador

Para una base Supabase existente, ejecute `src/main/resources/db/super-admin.sql`
para ampliar los roles permitidos. El mismo archivo incluye la sentencia para
promover el administrador elegido por correo. Cierre sesión y vuelva a ingresar
después del cambio. No se promueven cuentas existentes automáticamente.

En una instalación vacía, la primera cuenta se crea como `SUPER_ADMIN`.
Este rol tiene acceso a toda la gestión y puede aprobar clientes, administradores
y otros superadministradores. Un `ADMIN` únicamente puede aprobar clientes.
