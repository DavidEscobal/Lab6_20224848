package edu.pucp.lab6;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.pucp.lab6.databinding.ActivityMainBinding;
import edu.pucp.lab6.databinding.DialogPronosticoBinding;

public class MainActivity extends AppCompatActivity {
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    private ActivityMainBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;
    private ListenerRegistration pronosticosListener;
    private PronosticoAdapter adapter;
    private final SimpleDateFormat firestoreDateFormat =
            new SimpleDateFormat(DATE_PATTERN, Locale.US);

    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            this::onSignInResult
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!firebaseEstaConfigurado()) {
            mostrarFirebaseSinConfigurar();
            return;
        }

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.setLanguageCode("es-419");
        db = FirebaseFirestore.getInstance();

        adapter = new PronosticoAdapter(new PronosticoAdapter.PronosticoActions() {
            @Override
            public void onEditar(PronosticoDto pronostico) {
                mostrarDialogoPronostico(pronostico);
            }

            @Override
            public void onEliminar(PronosticoDto pronostico) {
                confirmarEliminacion(pronostico);
            }
        });

        binding.recyclerPronosticos.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerPronosticos.setAdapter(adapter);
        binding.buttonNuevoPronostico.setOnClickListener(view -> mostrarDialogoPronostico(null));
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_pronosticos) {
                mostrarPronosticos();
                return true;
            } else if (item.getItemId() == R.id.navigation_estadisticas) {
                mostrarEstadisticas();
                return true;
            } else if (item.getItemId() == R.id.navigation_logout) {
                cerrarSesion();
                return false;
            }
            return false;
        });

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            iniciarLogin();
        } else {
            configurarSesionActiva(user);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (pronosticosListener != null) {
            pronosticosListener.remove();
            pronosticosListener = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (firebaseAuth == null || db == null) {
            return;
        }
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null && pronosticosListener == null) {
            escucharPronosticos();
        }
    }

    private boolean firebaseEstaConfigurado() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp app = FirebaseApp.initializeApp(this);
                return app != null;
            }
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private void mostrarFirebaseSinConfigurar() {
        binding.textUsuario.setText(R.string.firebase_no_configurado_titulo);
        binding.layoutPronosticos.setVisibility(View.VISIBLE);
        binding.layoutEstadisticas.setVisibility(View.GONE);
        binding.buttonNuevoPronostico.setEnabled(false);
        binding.textEstadoVacio.setVisibility(View.VISIBLE);
        binding.textEstadoVacio.setText(R.string.firebase_no_configurado_mensaje);
        binding.bottomNavigation.setEnabled(false);
        Toast.makeText(this, R.string.firebase_no_configurado_titulo, Toast.LENGTH_LONG).show();
    }

    private void iniciarLogin() {
        boolean googleConfigurado = googleSignInEstaConfigurado();
        int layoutLogin = googleConfigurado
                ? R.layout.auth_method_picker
                : R.layout.auth_method_picker_sin_google;

        AuthMethodPickerLayout.Builder layoutBuilder = new AuthMethodPickerLayout.Builder(layoutLogin)
                .setEmailButtonId(R.id.buttonEmail)
                .setGithubButtonId(R.id.buttonGithub);

        List<AuthUI.IdpConfig> providers = new ArrayList<>();
        providers.add(new AuthUI.IdpConfig.EmailBuilder().build());
        if (googleConfigurado) {
            layoutBuilder.setGoogleButtonId(R.id.buttonGoogle);
            providers.add(new AuthUI.IdpConfig.GoogleBuilder().build());
        } else {
            Toast.makeText(this, R.string.google_no_configurado, Toast.LENGTH_LONG).show();
        }
        providers.add(new AuthUI.IdpConfig.GitHubBuilder().build());

        AuthMethodPickerLayout customLayout = layoutBuilder.build();

        Intent intent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setAuthMethodPickerLayout(customLayout)
                .setLogo(R.drawable.ic_world_cup)
                .setTheme(R.style.Theme_Lab6)
                .setIsSmartLockEnabled(false)
                .build();
        signInLauncher.launch(intent);
    }

    private boolean googleSignInEstaConfigurado() {
        int resourceId = getResources().getIdentifier(
                "default_web_client_id",
                "string",
                getPackageName()
        );
        if (resourceId == 0) {
            return false;
        }
        try {
            String webClientId = getString(resourceId).trim();
            return !webClientId.isEmpty()
                    && !"CHANGE-ME".equals(webClientId)
                    && !"default_web_client_id".equals(webClientId);
        } catch (Resources.NotFoundException e) {
            return false;
        }
    }

    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        if (result.getResultCode() == RESULT_OK) {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                configurarSesionActiva(user);
            }
        } else {
            Toast.makeText(this, R.string.login_cancelado, Toast.LENGTH_SHORT).show();
        }
    }

    private void configurarSesionActiva(@NonNull FirebaseUser user) {
        String correo = user.getEmail() == null ? user.getUid() : user.getEmail();
        binding.textUsuario.setText(correo);
        guardarUsuarioSiEsNecesario(user);
        mostrarPronosticos();
        escucharPronosticos();
    }

    private void guardarUsuarioSiEsNecesario(@NonNull FirebaseUser user) {
        Map<String, Object> usuario = new HashMap<>();
        usuario.put("uid", user.getUid());
        usuario.put("correo", user.getEmail());
        usuario.put("nombre", user.getDisplayName());
        db.collection("usuarios")
                .document(user.getUid())
                .set(usuario, SetOptions.merge());
    }

    private CollectionReference pronosticosRef() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("No hay usuario autenticado");
        }
        return db.collection("usuarios")
                .document(user.getUid())
                .collection("pronosticos");
    }

    private void escucharPronosticos() {
        if (pronosticosListener != null) {
            pronosticosListener.remove();
        }
        pronosticosListener = pronosticosRef()
                .orderBy("fechaPartido", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, R.string.error_cargar_pronosticos, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshot == null) {
                        return;
                    }
                    List<PronosticoDto> pronosticos = new java.util.ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        PronosticoDto pronostico = doc.toObject(PronosticoDto.class);
                        pronostico.setId(doc.getId());
                        pronosticos.add(pronostico);
                    }
                    adapter.submitList(pronosticos);
                    actualizarEstadisticas(pronosticos);
                    binding.textEstadoVacio.setVisibility(pronosticos.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void mostrarPronosticos() {
        binding.layoutPronosticos.setVisibility(View.VISIBLE);
        binding.layoutEstadisticas.setVisibility(View.GONE);
    }

    private void mostrarEstadisticas() {
        binding.layoutPronosticos.setVisibility(View.GONE);
        binding.layoutEstadisticas.setVisibility(View.VISIBLE);
    }

    private void mostrarDialogoPronostico(@Nullable PronosticoDto pronostico) {
        if (pronostico != null && !pronostico.estaPendiente()) {
            Toast.makeText(this, R.string.pronostico_bloqueado, Toast.LENGTH_SHORT).show();
            return;
        }

        DialogPronosticoBinding dialogBinding = DialogPronosticoBinding.inflate(LayoutInflater.from(this));
        configurarSpinnerEstado(dialogBinding, pronostico);
        configurarSelectorFecha(dialogBinding);

        if (pronostico != null) {
            dialogBinding.editSeleccionA.setText(pronostico.getSeleccionA());
            dialogBinding.editSeleccionB.setText(pronostico.getSeleccionB());
            dialogBinding.editGolesA.setText(String.valueOf(pronostico.getGolesA()));
            dialogBinding.editGolesB.setText(String.valueOf(pronostico.getGolesB()));
            if (pronostico.getFechaPartido() != null) {
                dialogBinding.editFecha.setText(firestoreDateFormat.format(pronostico.getFechaPartido()));
                dialogBinding.editFecha.setTag(pronostico.getFechaPartido());
            }
        }

        int titulo = pronostico == null ? R.string.registrar_pronostico : R.string.editar_pronostico;
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.guardar, null)
                .create();
        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> guardarPronostico(dialog, dialogBinding, pronostico)));
        dialog.show();
    }

    private void configurarSpinnerEstado(DialogPronosticoBinding dialogBinding, @Nullable PronosticoDto pronostico) {
        String[] estados = {
                PronosticoDto.ESTADO_PENDIENTE,
                PronosticoDto.ESTADO_ACERTADO,
                PronosticoDto.ESTADO_FALLADO
        };
        ArrayAdapter<String> adapterEstados = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, estados);
        adapterEstados.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dialogBinding.spinnerEstado.setAdapter(adapterEstados);
        dialogBinding.spinnerEstado.setEnabled(pronostico != null);
        if (pronostico != null) {
            int index = Arrays.asList(estados).indexOf(pronostico.getEstado());
            dialogBinding.spinnerEstado.setSelection(Math.max(index, 0));
        }
    }

    private void configurarSelectorFecha(DialogPronosticoBinding dialogBinding) {
        dialogBinding.editFecha.setOnClickListener(view -> {
            Date fechaActual = obtenerFechaDialogo(dialogBinding);
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            calendar.setTime(fechaActual);
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (picker, year, month, dayOfMonth) -> {
                        java.util.Calendar selected = java.util.Calendar.getInstance();
                        selected.set(year, month, dayOfMonth, 0, 0, 0);
                        selected.set(java.util.Calendar.MILLISECOND, 0);
                        Date fecha = selected.getTime();
                        dialogBinding.editFecha.setText(firestoreDateFormat.format(fecha));
                        dialogBinding.editFecha.setTag(fecha);
                    },
                    calendar.get(java.util.Calendar.YEAR),
                    calendar.get(java.util.Calendar.MONTH),
                    calendar.get(java.util.Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
    }

    private Date obtenerFechaDialogo(DialogPronosticoBinding dialogBinding) {
        Object tag = dialogBinding.editFecha.getTag();
        if (tag instanceof Date) {
            return (Date) tag;
        }
        try {
            String fechaTexto = dialogBinding.editFecha.getText().toString();
            if (!fechaTexto.trim().isEmpty()) {
                return firestoreDateFormat.parse(fechaTexto);
            }
        } catch (ParseException ignored) {
        }
        return new Date();
    }

    private void guardarPronostico(AlertDialog dialog,
                                   DialogPronosticoBinding dialogBinding,
                                   @Nullable PronosticoDto pronosticoExistente) {
        String seleccionA = dialogBinding.editSeleccionA.getText().toString().trim();
        String seleccionB = dialogBinding.editSeleccionB.getText().toString().trim();
        String golesATexto = dialogBinding.editGolesA.getText().toString().trim();
        String golesBTexto = dialogBinding.editGolesB.getText().toString().trim();
        Object fechaTag = dialogBinding.editFecha.getTag();

        if (seleccionA.isEmpty() || seleccionB.isEmpty() || golesATexto.isEmpty()
                || golesBTexto.isEmpty() || !(fechaTag instanceof Date)) {
            Toast.makeText(this, R.string.validacion_campos, Toast.LENGTH_SHORT).show();
            return;
        }
        if (seleccionA.equalsIgnoreCase(seleccionB)) {
            Toast.makeText(this, R.string.validacion_selecciones, Toast.LENGTH_SHORT).show();
            return;
        }

        int golesA;
        int golesB;
        try {
            golesA = Integer.parseInt(golesATexto);
            golesB = Integer.parseInt(golesBTexto);
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.validacion_goles, Toast.LENGTH_SHORT).show();
            return;
        }
        if (golesA < 0 || golesB < 0) {
            Toast.makeText(this, R.string.validacion_goles, Toast.LENGTH_SHORT).show();
            return;
        }

        String estado = pronosticoExistente == null
                ? PronosticoDto.ESTADO_PENDIENTE
                : dialogBinding.spinnerEstado.getSelectedItem().toString();
        PronosticoDto pronostico = new PronosticoDto(
                seleccionA,
                seleccionB,
                (Date) fechaTag,
                golesA,
                golesB,
                estado
        );

        if (pronosticoExistente == null) {
            pronosticosRef()
                    .add(pronostico)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, R.string.pronostico_registrado, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, R.string.error_guardar, Toast.LENGTH_SHORT).show());
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("seleccionA", pronostico.getSeleccionA());
            data.put("seleccionB", pronostico.getSeleccionB());
            data.put("fechaPartido", pronostico.getFechaPartido());
            data.put("golesA", pronostico.getGolesA());
            data.put("golesB", pronostico.getGolesB());
            data.put("estado", pronostico.getEstado());

            pronosticosRef()
                    .document(pronosticoExistente.getId())
                    .update(data)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, R.string.pronostico_actualizado, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, R.string.error_guardar, Toast.LENGTH_SHORT).show());
        }
    }

    private void confirmarEliminacion(PronosticoDto pronostico) {
        if (!pronostico.estaPendiente()) {
            Toast.makeText(this, R.string.pronostico_bloqueado, Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.eliminar_pronostico)
                .setMessage(R.string.confirmar_eliminacion)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.eliminar, (dialog, which) -> pronosticosRef()
                        .document(pronostico.getId())
                        .delete()
                        .addOnSuccessListener(unused -> Toast.makeText(this, R.string.pronostico_eliminado, Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, R.string.error_eliminar, Toast.LENGTH_SHORT).show()))
                .show();
    }

    private void actualizarEstadisticas(List<PronosticoDto> pronosticos) {
        int total = pronosticos.size();
        int pendientes = 0;
        int acertados = 0;
        int fallados = 0;

        for (PronosticoDto pronostico : pronosticos) {
            if (PronosticoDto.ESTADO_ACERTADO.equals(pronostico.getEstado())) {
                acertados++;
            } else if (PronosticoDto.ESTADO_FALLADO.equals(pronostico.getEstado())) {
                fallados++;
            } else {
                pendientes++;
            }
        }

        binding.textTotal.setText(String.valueOf(total));
        binding.textAcertados.setText(String.valueOf(acertados));
        binding.textFallados.setText(String.valueOf(fallados));
        binding.textPendientes.setText(String.valueOf(pendientes));
        binding.progressAcertados.setMax(Math.max(total, 1));
        binding.progressFallados.setMax(Math.max(total, 1));
        binding.progressPendientes.setMax(Math.max(total, 1));
        binding.progressAcertados.setProgress(acertados);
        binding.progressFallados.setProgress(fallados);
        binding.progressPendientes.setProgress(pendientes);
    }

    private void cerrarSesion() {
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(task -> {
                    if (pronosticosListener != null) {
                        pronosticosListener.remove();
                        pronosticosListener = null;
                    }
                    adapter.submitList(new java.util.ArrayList<>());
                    iniciarLogin();
                    Toast.makeText(this, R.string.logout_exitoso, Toast.LENGTH_SHORT).show();
                });
    }
}
