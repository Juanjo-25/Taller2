# Conectar Yate a Supabase

## 1. Crear el proyecto

1. Abra https://supabase.com/dashboard y cree un proyecto.
2. Guarde la contraseña de la base de datos que eligió al crearlo.
3. Cuando esté listo, abra **Connect** y seleccione **Session pooler**.
4. Copie el host, el puerto, el nombre de la base de datos y el usuario exactamente como aparecen. El usuario del pooler suele incluir la referencia del proyecto.

Utilizamos PostgreSQL por JDBC y Spring Data JPA. No se necesitan claves `anon`, `publishable` ni `service_role` para esta conexión.

## 2. Configurar e iniciar (macOS / zsh)

En una terminal, dentro de la carpeta del proyecto:

```zsh
export SPRING_PROFILES_ACTIVE=supabase
export SUPABASE_DB_HOST='aws-0-us-east-2.pooler.supabase.com'
export SUPABASE_DB_PORT='5432'
export SUPABASE_DB_NAME='postgres'
export SUPABASE_DB_USER='postgres.mpdgevpqsdxwoexaunxu'
read -s 'SUPABASE_DB_PASSWORD?Contraseña de la base de datos: '
export SUPABASE_DB_PASSWORD
./mvnw spring-boot:run
```

El host y el usuario de este proyecto ya están configurados como valores predeterminados del perfil `supabase`. La contraseña se solicita sin mostrarla y no se escribe en el historial del comando. Estas variables pertenecen a la terminal actual; si usa el IDE, configure las mismas variables en su configuración de ejecución. Spring Boot no carga un archivo `.env` automáticamente.

## 3. Verificar

1. Abra http://localhost:8080/login/ingresar.
2. Cree el administrador inicial si esta base de datos está vacía.
3. En Supabase, abra **Table Editor** y seleccione el esquema **yate**.
4. Verifique las tablas `login` y `cliente` y los registros creados desde la aplicación.

La aplicación crea el esquema `yate` y Hibernate crea o actualiza sus tablas. El esquema debe mantenerse fuera de los esquemas expuestos por la Data API: las consultas se realizan desde el servidor Java. Las contraseñas de usuarios se guardan como hashes BCrypt en `yate.login`, distintos de la contraseña de conexión a PostgreSQL.

El perfil `supabase` usa SSL y un máximo de cinco conexiones. El login, la aprobación de cuentas y los permisos siguen a cargo de Spring Security; no se utiliza Supabase Auth.

## 4. Datos locales y pruebas

Los datos que ya existen en H2 no se transfieren automáticamente. Supabase empieza con sus propios registros; no se borra la base local.

Para volver a H2 en esta terminal:

```zsh
unset SPRING_PROFILES_ACTIVE
unset SUPABASE_DB_HOST SUPABASE_DB_PORT SUPABASE_DB_NAME SUPABASE_DB_USER SUPABASE_DB_PASSWORD
./mvnw spring-boot:run
```

Las pruebas siguen usando bases H2 temporales y no modifican Supabase. Ejecútelas sin activar el perfil remoto:

```zsh
SPRING_PROFILES_ACTIVE= ./mvnw test
```

Documentación oficial: https://supabase.com/docs/guides/getting-started/quickstarts/spring-boot
