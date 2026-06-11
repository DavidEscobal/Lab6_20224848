# LAB6_20224848 - Firebase Authentication y Cloud Firestore

Aplicacion Android desarrollada en Java para el Laboratorio 6 de IoT. La app permite autenticar usuarios y gestionar pronosticos del Mundial usando Firebase Authentication y Cloud Firestore.

## Funcionalidades principales

- Inicio de sesion con correo, Google y GitHub mediante FirebaseUI Auth.
- Registro automatico del usuario autenticado en Firestore.
- CRUD de pronosticos por usuario.
- Lista de pronosticos en tiempo real con `addSnapshotListener()`.
- Edicion y eliminacion controladas segun el estado del pronostico.
- Estados disponibles: `Pendiente`, `Acertado` y `Fallado`.
- Estadisticas en tiempo real: total, acertados, fallados y pendientes.
- Reglas de Firestore para que cada usuario solo acceda a sus propios datos.

## Tecnologias utilizadas

- Android Java
- Gradle
- View Binding
- Material Components
- Firebase Authentication
- FirebaseUI Auth
- Cloud Firestore
- Firestore Security Rules

## Estructura de datos en Firestore

```text
usuarios/{uid}
usuarios/{uid}/pronosticos/{pronosticoId}
```

Cada pronostico guarda:

- `seleccionA`
- `seleccionB`
- `fechaPartido`
- `golesA`
- `golesB`
- `estado`

## Configuracion de Firebase

La app Android esta configurada con el package:

```text
edu.pucp.lab6
```

Para ejecutar el proyecto en otra maquina:

1. Crear o usar un proyecto en Firebase Console.
2. Registrar una app Android con el package `edu.pucp.lab6`.
3. Agregar el SHA-1 del certificado debug o release que se vaya a usar.
4. Descargar `google-services.json`.
5. Colocar el archivo en:

   ```text
   app/google-services.json
   ```

6. Activar los proveedores de Authentication:
   - Email/Password
   - Google
   - GitHub

7. Publicar las reglas de Firestore:

   ```powershell
   firebase deploy --only firestore:rules --project <project-id>
   ```

## Reglas de Firestore

Las reglas del proyecto se encuentran en:

```text
firestore.rules
```

Permiten que un usuario autenticado lea y escriba solamente en su documento:

```text
usuarios/{uid}/pronosticos/{pronosticoId}
```

## Compilacion

Para generar el APK debug:

```powershell
.\gradlew.bat assembleDebug
```

El APK se genera en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Pruebas realizadas

- Login con Google.
- Validacion de SHA-1 para Google Sign-In.
- Registro de pronosticos en Firestore.
- Lectura en tiempo real de pronosticos.
- Despliegue de reglas de Firestore.
- Generacion de APK debug compatible con Android 11.
