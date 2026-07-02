package upeu.edu.pe.dto;

public class HistorialEntry {
    public String tipo;
    public String fecha;
    public String titulo;
    public String descripcion;
    public Object data;

    public HistorialEntry() {}

    public HistorialEntry(String tipo, String fecha, String titulo, String descripcion, Object data) {
        this.tipo = tipo;
        this.fecha = fecha;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.data = data;
    }
}
