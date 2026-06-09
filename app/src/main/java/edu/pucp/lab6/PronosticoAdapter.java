package edu.pucp.lab6;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import edu.pucp.lab6.databinding.ItemPronosticoBinding;

public class PronosticoAdapter extends RecyclerView.Adapter<PronosticoAdapter.PronosticoViewHolder> {
    public interface PronosticoActions {
        void onEditar(PronosticoDto pronostico);

        void onEliminar(PronosticoDto pronostico);
    }

    private final List<PronosticoDto> pronosticos = new ArrayList<>();
    private final PronosticoActions actions;
    private final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);

    public PronosticoAdapter(PronosticoActions actions) {
        this.actions = actions;
    }

    public void submitList(List<PronosticoDto> nuevosPronosticos) {
        pronosticos.clear();
        pronosticos.addAll(nuevosPronosticos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PronosticoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPronosticoBinding binding = ItemPronosticoBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new PronosticoViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PronosticoViewHolder holder, int position) {
        holder.bind(pronosticos.get(position));
    }

    @Override
    public int getItemCount() {
        return pronosticos.size();
    }

    class PronosticoViewHolder extends RecyclerView.ViewHolder {
        private final ItemPronosticoBinding binding;

        PronosticoViewHolder(ItemPronosticoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PronosticoDto pronostico) {
            String fecha = pronostico.getFechaPartido() == null
                    ? "Sin fecha"
                    : dateFormat.format(pronostico.getFechaPartido());
            boolean editable = pronostico.estaPendiente();

            binding.textEquipos.setText(pronostico.getSeleccionA() + " vs " + pronostico.getSeleccionB());
            binding.textResultado.setText(pronostico.getGolesA() + " - " + pronostico.getGolesB());
            binding.textFecha.setText(fecha);
            binding.textEstado.setText(pronostico.getEstado());
            binding.buttonEditar.setEnabled(editable);
            binding.buttonEliminar.setEnabled(editable);
            binding.buttonEditar.setOnClickListener(view -> actions.onEditar(pronostico));
            binding.buttonEliminar.setOnClickListener(view -> actions.onEliminar(pronostico));
        }
    }
}
