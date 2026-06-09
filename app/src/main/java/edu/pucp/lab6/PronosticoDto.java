package edu.pucp.lab6;

import java.util.Date;

public class PronosticoDto {
    public static final String ESTADO_PENDIENTE = "Pendiente";
    public static final String ESTADO_ACERTADO = "Acertado";
    public static final String ESTADO_FALLADO = "Fallado";

    private String id;
    private String seleccionA;
    private String seleccionB;
    private Date fechaPartido;
    private int golesA;
    private int golesB;
    private String estado;

    public PronosticoDto() {
    }

    public PronosticoDto(String seleccionA, String seleccionB, Date fechaPartido,
                         int golesA, int golesB, String estado) {
        this.seleccionA = seleccionA;
        this.seleccionB = seleccionB;
        this.fechaPartido = fechaPartido;
        this.golesA = golesA;
        this.golesB = golesB;
        this.estado = estado;
    }

    public boolean estaPendiente() {
        return ESTADO_PENDIENTE.equals(estado);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSeleccionA() {
        return seleccionA;
    }

    public void setSeleccionA(String seleccionA) {
        this.seleccionA = seleccionA;
    }

    public String getSeleccionB() {
        return seleccionB;
    }

    public void setSeleccionB(String seleccionB) {
        this.seleccionB = seleccionB;
    }

    public Date getFechaPartido() {
        return fechaPartido;
    }

    public void setFechaPartido(Date fechaPartido) {
        this.fechaPartido = fechaPartido;
    }

    public int getGolesA() {
        return golesA;
    }

    public void setGolesA(int golesA) {
        this.golesA = golesA;
    }

    public int getGolesB() {
        return golesB;
    }

    public void setGolesB(int golesB) {
        this.golesB = golesB;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
