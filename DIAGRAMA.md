# Flujo de la opcion "Insertar heroe"

```
 Usuario
   |  escribe: Thor, guerrero, 150, 3
   v
+-----------------------------+
| VIEW  -  HeroeUI            |   solo Scanner y System.out
| insertar()                  |   lee los datos y se los pasa al controller
+-----------------------------+
   |  controller.insertar("Thor", "guerrero", 150, 3)
   v
+-----------------------------+
| CONTROLLER - HeroeController|   valida: nombre no vacio, tipo guerrero/mago,
| insertar(...)               |   vida > 0, nivel >= 1
|                             |   si algo esta mal devuelve el mensaje y no llama al DAO
+-----------------------------+
   |  dao.existeNombre("Thor")         -> si ya existe: "Ya existe un heroe llamado Thor."
   |  dao.insertar(new Heroe(...))     (Heroe es el POJO del paquete model)
   v
+-----------------------------+
| DAO  -  HeroeDAO            |   unica capa con JDBC
| insertar(Heroe)             |   1. ConexionDB.getConnection()  (lee config.txt)
|                             |   2. con.prepareStatement("INSERT ... VALUES (?, ?, ?, ?)")
|                             |   3. setString / setInt en cada ?
|                             |   4. executeUpdate()
|                             |   5. try-with-resources cierra PreparedStatement y Connection
+-----------------------------+
   |  INSERT INTO heroes ...
   v
+-----------------------------+
| BD  -  MySQL                |   rpg_grupal_db.heroes
|                             |   UNIQUE(nombre), CHECK(tipo), CHECK(nivel >= 1)
+-----------------------------+

 Vuelta:
 BD -> DAO: 1 fila afectada (si falla, SQLException -> el DAO lanza DAOException)
 DAO -> Controller: termina bien o lanza DAOException
 Controller -> View: "Heroe Thor guardado en la base de datos."  o  "Error: ..."
 View -> Usuario: imprime el mensaje
```
