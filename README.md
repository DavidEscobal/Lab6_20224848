# LAB6_20224848 - Firebase Authentication & Cloud Firestore

Aplicacion Android Java para el **Sistema de Seguimiento del Mundial de Futbol** del Laboratorio 6.

## Metodos aplicados de las clases

- Firebase Authentication: `FirebaseAuth.getInstance()`, `getCurrentUser()`, `getUid()`, `AuthUI.signOut()`.
- FirebaseUI Auth: `createSignInIntentBuilder()`, `EmailBuilder`, `GoogleBuilder`, `GitHubBuilder`, `AuthMethodPickerLayout`.
- Cloud Firestore: `FirebaseFirestore.getInstance()`, `collection()`, `document()`, `add()`, `update()`, `delete()`, `addSnapshotListener()`, `toObject()`, `getId()`.
- NoSQL: datos organizados como coleccion/documento bajo `usuarios/{uid}/pronosticos/{pronosticoId}`.

## Configuracion Firebase

1. Crea un proyecto en Firebase Console.
2. Registra una app Android con package name:

   ```text
   edu.pucp.lab6
   ```

3. Descarga `google-services.json` y colocalo en:

   ```text
   app/google-services.json
   ```

4. Activa Authentication con:
   - Email/Password
   - Google
   - GitHub

5. Para GitHub, crea una OAuth App en GitHub y registra el Client ID/Secret en Firebase Authentication.
6. Crea Cloud Firestore y publica las reglas de `firestore.rules`.

## Funcionalidades

- Inicio de sesion personalizado con correo, Google y GitHub.
- Registro automatico de usuarios autenticados en `usuarios/{uid}`.
- CRUD de pronosticos en tiempo real.
- Edicion y eliminacion solo cuando el estado es `Pendiente`.
- Bloqueo automatico cuando el estado cambia a `Acertado` o `Fallado`.
- Estadisticas en tiempo real: total, acertados, fallados y pendientes.

## Pruebas recomendadas

- Crear cuenta por correo y validar el usuario en Firebase Auth.
- Iniciar sesion con Google y GitHub.
- Registrar un pronostico valido y verificarlo en Firestore.
- Intentar registrar selecciones iguales.
- Editar un pronostico pendiente.
- Cambiar estado a `Acertado` o `Fallado` y confirmar que queda bloqueado.
- Eliminar un pronostico pendiente con confirmacion.
- Abrir Estadisticas y confirmar actualizacion automatica.
- Probar con dos usuarios para confirmar separacion por `uid`.
