package uy.edu.utu.shopexpress.usuarios.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Registro de exportación sin datos identificatorios directos: no hay nombre,
 * apellido, calle ni documento. Los identificadores se reemplazan por seudónimos
 * HMAC (estables, permiten cruzar datasets sin revelar la identidad) y la fecha
 * de alta se generaliza a año-mes.
 */
public record UsuarioExportado(
        String idSeudonimo,
        String tipoDocumento,
        String documentoSeudonimo,
        String emailEnmascarado,
        List<String> ciudades,
        List<String> codigosPostales,
        int cantidadCompras,
        BigDecimal totalComprado,
        String mesAlta) {
}
