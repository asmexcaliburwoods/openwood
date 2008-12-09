@cls
@rem SUITABLE SETTING FOR 25000 USERS --> set MAXMEM=165000000
set MAXMEM=165000000
rem set JAVAOPTIONS=-version
set JAVAOPTIONS=-server -Xms%MAXMEM% -Xmx%MAXMEM% -DXincgc -showversion
@rem -Xms<size>        set initial Java heap size
@rem -Xmx<size>        set maximum Java heap size
@call t TestMemory
