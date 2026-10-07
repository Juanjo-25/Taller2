"""Ejecutar desde la raíz: python3 scripts/supabase.py export|apply|run."""
from pathlib import Path
import os
import shutil
import subprocess
import sys
from datetime import datetime

root = Path(__file__).resolve().parents[1]
os.chdir(root)
env = os.environ.copy()
if (root / '.env').exists():
    for line in (root / '.env').read_text().splitlines():
        line = line.strip()
        if not line or line.startswith('#'):
            continue
        if line.startswith('export '):
            line = line[7:]
        if '=' not in line:
            raise SystemExit('Formato inválido en .env; use CLAVE=valor.')
        key, value = line.split('=', 1)
        env[key.strip()] = value.strip().strip('\"\'')
mode = sys.argv[1] if len(sys.argv) == 2 else ''
if mode == 'run':
    if not env.get('SUPABASE_DB_PASSWORD'):
        raise SystemExit('Falta SUPABASE_DB_PASSWORD en .env.')
    env['SPRING_PROFILES_ACTIVE'] = 'supabase'
    raise SystemExit(subprocess.call(['./mvnw', 'spring-boot:run'], env=env))
if mode not in ('export', 'apply', 'inspect', 'import-client'):
    raise SystemExit('Uso: python3 scripts/supabase.py export|apply|run')
if mode == 'export':
    opened = subprocess.run(['lsof', str(root / 'data/yate.mv.db')], capture_output=True)
    if opened.returncode == 0:
        raise SystemExit('Detenga la aplicación: la base H2 está abierta en otro proceso.')
    backup = root / 'data' / 'backups' / datetime.now().strftime('%Y%m%d-%H%M%S')
    backup.mkdir(parents=True)
    shutil.copy2(root / 'data/yate.mv.db', backup / 'yate.mv.db')
    (backup / 'yate.mv.db').chmod(0o600)
jars = []
for dependency in ('com/h2database/h2', 'org/postgresql/postgresql'):
    candidates = sorted((Path.home() / '.m2/repository' / dependency).glob('*/*.jar'))
    if not candidates:
        raise SystemExit('Faltan dependencias JDBC. Ejecute ./mvnw dependency:go-offline')
    jars.append(str(candidates[-1]))
command = ['java', '--class-path', os.pathsep.join(jars), 'scripts/MigrarH2.java']
if mode in ('apply', 'inspect', 'import-client'):
    if not env.get('SUPABASE_DB_PASSWORD'):
        raise SystemExit('Falta SUPABASE_DB_PASSWORD en .env.')
    command.append(mode)
result = subprocess.call(command, env=env)
if mode == 'export' and result == 0:
    (root / 'data/migracion-supabase.sql').chmod(0o600)
raise SystemExit(result)
