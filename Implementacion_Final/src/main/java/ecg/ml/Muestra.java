package ecg.ml;

/**
 * Una muestra de entrenamiento: su vector de características y la clase real a la que pertenece.
 *
 * <p>Para los latidos ECG, {@code caracteristicas} son los 257 valores que calcula
 * {@link ecg.ml.Caracteristicas} y {@code etiqueta} es la clase AAMI como índice (0=N, 1=S, 2=V, 3=F).
 *
 * @param caracteristicas valores numéricos que describen la muestra
 * @param etiqueta        clase real, como índice entero (0, 1, 2, ...)
 */
public record Muestra(double[] caracteristicas, int etiqueta) {
}
