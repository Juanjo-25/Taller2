import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Exporta H2 en modo lectura; el SQL generado requiere tablas destino vacías. */
public class MigrarH2 {
    static final Map<String, String> TABLES = new LinkedHashMap<>();
    static {
        TABLES.put("login", "id,email,contrasena,activo,rol");
        TABLES.put("cliente", "id,nombre,apellido,email,create_at,login_id");
        TABLES.put("producto", "id,nombre,descripcion,valor_unitario,stock,version");
        TABLES.put("encabezado", "id,cliente_id,fecha,total");
        TABLES.put("detalle", "id,encabezado_id,producto_id,cantidad,valor");
    }
    static String literal(Object value) {
        if (value == null) return "NULL";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        return "'" + value.toString().replace("'", "''") + "'";
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && (args[0].equals("apply") || args[0].equals("inspect") || args[0].equals("import-client"))) {
            String password = System.getenv("SUPABASE_DB_PASSWORD");
            if (password == null || password.isBlank()) throw new IllegalArgumentException("Falta SUPABASE_DB_PASSWORD en .env");
            String url = "jdbc:postgresql://" + env("SUPABASE_DB_HOST", "aws-0-us-east-2.pooler.supabase.com")
                + ":" + env("SUPABASE_DB_PORT", "5432") + "/" + env("SUPABASE_DB_NAME", "postgres") + "?sslmode=require&connectTimeout=15";
            try (Connection db = DriverManager.getConnection(url, env("SUPABASE_DB_USER", "postgres.mpdgevpqsdxwoexaunxu"), password);
                 Statement stmt = db.createStatement()) {
                if (args[0].equals("import-client")) importClient(db);
                if (args[0].equals("apply")) stmt.execute(Files.readString(Path.of("data/migracion-supabase.sql")));
                for (String table : TABLES.keySet()) {
                    try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM yate." + table)) {
                        rs.next(); System.out.println(table + ": " + rs.getLong(1) + " registros verificados");
                    }
                }
                if (args[0].equals("inspect")) {
                    try (Connection source = DriverManager.getConnection("jdbc:h2:file:./data/yate;ACCESS_MODE_DATA=r;IFEXISTS=TRUE", "sa", "");
                         Statement local = source.createStatement();
                         ResultSet rows = local.executeQuery("SELECT id,nombre,apellido,email,create_at FROM cliente")) {
                        int equal = 0, missing = 0, conflict = 0;
                        while (rows.next()) {
                            try (PreparedStatement query = db.prepareStatement("SELECT id,nombre,apellido,email,create_at FROM yate.cliente WHERE id=?")) {
                                query.setLong(1, rows.getLong(1));
                                try (ResultSet target = query.executeQuery()) {
                                    if (!target.next()) { missing++; continue; }
                                    boolean same = true;
                                    for (int i = 1; i <= 5; i++) same &= Objects.equals(rows.getObject(i), target.getObject(i));
                                    if (same) equal++; else conflict++;
                                }
                            }
                        }
                        System.out.println("Clientes locales: iguales=" + equal + ", ausentes=" + missing + ", conflictos=" + conflict);
                    }
                }
            } catch (SQLException ex) {
                System.err.println("Migración fallida. SQLState=" + ex.getSQLState() + ". Revise conexión, permisos o tablas destino no vacías.");
                System.exit(1);
            }
            return;
        }
        StringBuilder sql = new StringBuilder("BEGIN;\nSET standard_conforming_strings = on;\n");
        sql.append(Files.readString(Path.of("src/main/resources/db/supabase.sql")));
        sql.append("\nLOCK TABLE yate.login, yate.cliente, yate.producto, yate.encabezado, yate.detalle IN ACCESS EXCLUSIVE MODE;\n");
        for (String table : TABLES.keySet()) sql.append("DO $$ BEGIN IF EXISTS (SELECT 1 FROM yate.").append(table)
            .append(") THEN RAISE EXCEPTION 'La tabla destino ").append(table).append(" debe estar vacía'; END IF; END $$;\n");
        try (Connection db = DriverManager.getConnection("jdbc:h2:file:./data/yate;ACCESS_MODE_DATA=r;IFEXISTS=TRUE", "sa", "")) {
            for (var entry : TABLES.entrySet()) {
                String table = entry.getKey();
                long count = 0;
                try (ResultSet tables = db.getMetaData().getTables(null, "PUBLIC", table.toUpperCase(Locale.ROOT), new String[]{"TABLE"})) {
                    if (tables.next()) {
                        try (Statement stmt = db.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM " + table + " ORDER BY id")) {
                            Set<String> columns = new HashSet<>();
                            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) columns.add(rs.getMetaData().getColumnName(i).toLowerCase(Locale.ROOT));
                            while (rs.next()) {
                                List<String> values = new ArrayList<>();
                                for (String col : entry.getValue().split(",")) {
                                    if (!columns.contains(col) && !col.equals("login_id") && !col.equals("version")) throw new IllegalStateException("Falta columna " + table + "." + col);
                                    values.add(literal(columns.contains(col) ? rs.getObject(col) : col.equals("version") ? 0L : null));
                                }
                                sql.append("INSERT INTO yate.").append(table).append(" (").append(entry.getValue()).append(") VALUES (")
                                    .append(String.join(",", values)).append(");\n");
                                count++;
                            }
                        }
                    }
                }
                sql.append("DO $$ BEGIN IF (SELECT count(*) FROM yate.").append(table).append(") <> ").append(count)
                    .append(" THEN RAISE EXCEPTION 'Conteo incorrecto en ").append(table).append("'; END IF; END $$;\n");
                // ALTER SEQUENCE es transaccional y evita IDs repetidos después de importar.
                sql.append("DO $$ DECLARE siguiente bigint; BEGIN SELECT COALESCE(MAX(id),0)+1 INTO siguiente FROM yate.").append(table)
                    .append("; EXECUTE format('ALTER SEQUENCE %s RESTART WITH %s', pg_get_serial_sequence('yate.").append(table)
                    .append("','id'), siguiente); END $$;\n");
                System.out.println(table + ": " + count + " registros exportados");
            }
        }
        sql.append("COMMIT;\n");
        Files.writeString(Path.of("data/migracion-supabase.sql"), sql);
        System.out.println("SQL generado en data/migracion-supabase.sql (contiene datos privados).");
    }
    static void importClient(Connection db) throws Exception {
        try (Connection source = DriverManager.getConnection("jdbc:h2:file:./data/yate;ACCESS_MODE_DATA=r;IFEXISTS=TRUE", "sa", "");
             Statement local = source.createStatement();
             ResultSet rows = local.executeQuery("SELECT id,nombre,apellido,email,create_at FROM cliente")) {
            if (!rows.next()) throw new IllegalStateException("No hay cliente local para importar.");
            long oldId = rows.getLong(1);
            Object[] values = {rows.getObject(2), rows.getObject(3), rows.getObject(4), rows.getObject(5)};
            if (rows.next()) throw new IllegalStateException("Esta importación requiere exactamente un cliente local.");
            db.setAutoCommit(false);
            try (Statement stmt = db.createStatement()) {
                stmt.execute("LOCK TABLE yate.cliente IN ACCESS EXCLUSIVE MODE");
                Map<Long,List<Object>> before = clients(db);
                Long newId = null;
                for (var row : before.entrySet()) {
                    if (row.getValue().subList(0,4).equals(Arrays.asList(values)) && row.getValue().get(4) == null) {
                        newId = row.getKey(); break;
                    }
                }
                if (newId == null) {
                    stmt.execute("DO $$ DECLARE siguiente bigint; BEGIN SELECT COALESCE(MAX(id),0)+1 INTO siguiente FROM yate.cliente; EXECUTE format('ALTER SEQUENCE %s RESTART WITH %s', pg_get_serial_sequence('yate.cliente','id'), siguiente); END $$");
                    try (PreparedStatement insert = db.prepareStatement("INSERT INTO yate.cliente(nombre,apellido,email,create_at,login_id) VALUES(?,?,?,?,NULL) RETURNING id")) {
                        for (int i=0;i<4;i++) insert.setObject(i+1,values[i]);
                        try (ResultSet inserted=insert.executeQuery()) { inserted.next(); newId=inserted.getLong(1); }
                    }
                    Map<Long,List<Object>> after = clients(db);
                    if (after.size()!=before.size()+1 || !after.entrySet().containsAll(before.entrySet())) throw new IllegalStateException("Falló la verificación de conservación de clientes.");
                    if (!after.get(newId).subList(0,4).equals(Arrays.asList(values))) throw new IllegalStateException("Falló la verificación del cliente importado.");
                }
                db.commit();
                System.out.println("Cliente local ID " + oldId + " conservado en Supabase con ID " + newId + "; clientes anteriores intactos.");
                Path mapping=Path.of("data/cliente-id-supabase.txt");
                Files.writeString(mapping,"H2="+oldId+"\nSupabase="+newId+"\n");
            } catch (Exception ex) { db.rollback(); throw ex; }
            finally { db.setAutoCommit(true); }
        }
    }
    static Map<Long,List<Object>> clients(Connection db) throws SQLException {
        Map<Long,List<Object>> result=new LinkedHashMap<>();
        try (Statement stmt=db.createStatement(); ResultSet rows=stmt.executeQuery("SELECT id,nombre,apellido,email,create_at,login_id FROM yate.cliente ORDER BY id")) {
            while(rows.next()) {
                List<Object> values=new ArrayList<>();
                for(int i=2;i<=6;i++) values.add(rows.getObject(i));
                result.put(rows.getLong(1),values);
            }
        }
        return result;
    }
    static String env(String key, String fallback) { return System.getenv().getOrDefault(key, fallback); }
}
