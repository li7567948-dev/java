from pathlib import Path
import subprocess,tempfile,itertools,re,shutil
import sys
results=Path.cwd()/'results'; results.mkdir(exist_ok=True); build=str(Path(sys.argv[1] if len(sys.argv)>1 else 'bin').resolve()); passed=[]
def run(cwd,args='',data=''):
 r=subprocess.run(['java','-cp',build,'controller.Main']+([args] if args else []),input=data.encode(),stdout=subprocess.PIPE,stderr=subprocess.STDOUT,cwd=cwd,timeout=20)
 assert r.returncode==0,r.stdout; return r.stdout.decode('utf-8')
def check(name,fn):
 fn();passed.append(name);print('PASS: '+name)
def flags():
 for role,d,a in itertools.product(('root','user'),(False,True),(False,True)):
  with tempfile.TemporaryDirectory() as tmp:
   p=Path(tmp); login='demo'; config=f'login={login}\npassword=lab4\ngroup={role}\ndebug={str(d).lower()}\nautotests={str(a).lower()}\n';(p/'config.properties').write_text(config)
   out=run(p,data='demo\nlab4\n'+('11\n12\n' if role=='user' else '')+'0\n');log=(p/'logs/application.log').read_text()
   assert 'Добро пожаловать — demo' in out and ('11 - Отладка' in out)==(role=='root')
   assert ('PASS:' in out)==a and ('[TEST]' in log)==a and ('[DEBUG]' in log)==d
   if a:assert out.index('ИТОГО: 15 успешно, 0 ошибок')<out.index('===== ПУНКТ')
   if role=='user':assert out.count('Доступ запрещён')==2
   assert 'Старт программы, логин из настроек: demo' in log and 'Завершение программы' in log and 'lab4' not in log
   name=f'{role}_debug-{d}_autotests-{a}';(results/(name+'.txt')).write_text(out);(results/(name+'.log')).write_text(log);passed.append(name);print('PASS: '+name)
flags()
with tempfile.TemporaryDirectory() as tmp:
 p=Path(tmp);(p/'config.properties').write_text('login=root\npassword=lab4\ngroup=root\ndebug=true\nautotests=false\n')
 def root_ops():
  vehicle='1\nА 123 БВ\n4,5\n1.8\n1.5\n1500\n20\n1\n';truck='2\nБ 456 ВГ\n8\n2\n3\n9000\n15\n1\n'
  data='root\nlab4\n1\n2\n'+vehicle+'3\n1\n20\n1\n4\n6\n2\n'+vehicle+truck+'7\n6\n'+truck+'8\n7\n5\n4\n10\n6\nabc\n10\n999\n11\n12\n0\n'
  out=run(p,data=data);(results/'root_crud.txt').write_text(out);log=(p/'logs/application.log').read_text();
  for token in ('Добавлены ID: [6, 7]','Объект изменён','Объект удалён','База данных сохранена','Количество объектов: 6','Б 456 ВГ','ID не найден: 999','ИТОГО: 15 успешно, 0 ошибок','проверено: 3; разрешено: 2; запрещено: 1'):assert token in out,token
  assert 'Ошибка:' in out and 'Отладка: false' in out
  assert 'Запись БД: изменён ID=6' in log and 'Запись БД: удалён ID=7' in log
  db=p/'data/vehicles.txt';before=db.read_bytes();(results/'database-example.txt').write_bytes(before);beforelog=(p/'logs/application.log').read_bytes();out=run(p,data='root\nlab4\n4\n0\n');assert 'Количество объектов: 6' in out and 'Б 456 ВГ' in out and db.read_bytes()==before;assert (p/'logs/application.log').read_bytes().startswith(beforelog);(results/'restart.txt').write_text(out);(results/'application-example.log').write_bytes((p/'logs/application.log').read_bytes())
 check('CRUD, manual tests, simulation, invalid menu/ID, persistence, append',root_ops)
 def bad_login():
  out=run(p,data='root\nwrong\n');assert 'Неверный логин или пароль' in out and '===== ПУНКТ' not in out;(results/'failed_login.txt').write_text(out)
 check('wrong password',bad_login)
 def missing():
  out=run(p,args='missing.properties');assert 'Ошибка:' in out and '===== ПУНКТ' not in out;(results/'missing_config.txt').write_text(out)
 check('missing configuration',missing)
 def corrupt():
  db=p/'data/vehicles.txt';db.write_bytes(b'VEHICLES_V1\t5\ncorrupt\n');before=db.read_bytes();out=run(p,data='root\nlab4\n');assert 'Ошибка БД' in out and db.read_bytes()==before and '===== ПУНКТ' not in out;(results/'corrupt_database.txt').write_text(out)
 check('corrupt database remains untouched',corrupt)
print(f'TOTAL: {len(passed)} passed, 0 failed')
