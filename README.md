# CMSLike Desarrollo 1

Aplicación Spring Boot MVC con Thymeleaf y PostgreSQL. El sitio público y el panel usan el mismo servidor. El código sigue la estructura `cibertec.pe` con paquetes `controller`, `model`, `repository` y `service`.

## Ejecutar con Docker

Crea un archivo local `.env` con `DB_PASSWORD` y `CMS_MANAGER_CLAVE`. La contraseña inicial debe tener al menos 12 caracteres y un máximo de 72 bytes. Puedes indicar `CMS_MANAGER_USUARIO` y `CMS_PORT`, que toman los valores `manager` y `8080` por defecto.

```bash
docker compose up --build -d
```

El sitio está en `http://localhost:8080/`. Inicia sesión en `http://localhost:8080/admin/DashBoardAdminCMSPanel`. Las credenciales de arranque solo crean el primer usuario en una base de datos nueva. Conserva las contraseñas en variables de entorno y activa `CMS_COOKIE_SEGURA=true` al servir la aplicación por HTTPS.

## Flujo editorial

- El Manager registra usuarios, administra sus roles y estado, y configura la apariencia del sitio.
- El Manager y el Content Creator Manager asignan permisos de edición por página y de creación por prefijo. Por ejemplo, el permiso `/noticias` permite proponer `/noticias/evento`.
- El editor añade bloques desde el panel izquierdo. Puedes ordenar, duplicar y quitar bloques, y configurar filas de hasta cuatro imágenes. Las imágenes se referencian por URL.
- Guarda un borrador, abre su historial y envíalo a revisión. El revisor puede inspeccionar la misma composición que se publicará, simular móvil y aprobarla o devolverla con observaciones.
- La página aparece en la navegación pública tras la primera aprobación. La ruta `/` apunta a `/home` cuando existe una versión publicada de esa página.

## Base de datos y pruebas

Los repositorios JPA usan JPQL o consultas derivadas. Las migraciones Flyway en `src/main/resources/db/migration` crean las tablas de aplicación en `public` y los eventos editoriales en `audit`. Una publicación y sus eventos se guardan en la misma transacción.

Para apuntar a otro servidor PostgreSQL, configura `DB_URL`, `DB_USER` y `DB_PASSWORD`. Para cambiar de motor, agrega el driver y soporte Flyway correspondientes, verifica los nombres de esquema y adapta las migraciones que el motor requiera. Pasa la suite de integración en ese motor antes de usarlo.

Importa el `pom.xml` en STS como proyecto Maven existente. Con Java 21 ejecuta:

```bash
./mvnw clean verify
```

Surefire ejecuta las pruebas unitarias y Failsafe las de integración. La integración usa H2 en memoria por defecto y puede usar un PostgreSQL vacío y descartable con `CMS_TEST_DB_URL`, `CMS_TEST_DB_USER` y `CMS_TEST_DB_PASSWORD`. GitHub Actions verifica ambos motores.

El servicio PostgreSQL de Compose es para desarrollo local. Para producción, ejecuta las migraciones con un rol separado y limita el rol de aplicación a las operaciones requeridas. Para `audit.evento` concede lectura e inserción y acceso a su secuencia, con la modificación y el borrado reservados al administrador de base de datos. Flyway admite sus propias credenciales mediante `SPRING_FLYWAY_USER` y `SPRING_FLYWAY_PASSWORD`.
