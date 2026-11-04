package uy.edu.utu.shopexpress.usuarios.service;

import uy.edu.utu.shopexpress.usuarios.dto.UsuarioExportado;

import java.util.List;
import java.util.Objects;

public final class ExportadorCsv {

    private static final String ENCABEZADO = "id_seudonimo,tipo_documento,documento_seudonimo,email_enmascarado,"
            + "ciudades,codigos_postales,cantidad_compras,total_comprado,mes_alta";

    private ExportadorCsv() {
    }

    public static String aCsv(List<UsuarioExportado> registros) {
        StringBuilder csv = new StringBuilder(ENCABEZADO).append('\n');
        for (UsuarioExportado r : registros) {
            csv.append(String.join(",",
                    campo(r.idSeudonimo()),
                    campo(r.tipoDocumento()),
                    campo(r.documentoSeudonimo()),
                    campo(r.emailEnmascarado()),
                    campo(String.join("|", r.ciudades())),
                    campo(String.join("|", r.codigosPostales())),
                    String.valueOf(r.cantidadCompras()),
                    campo(Objects.toString(r.totalComprado(), "")),
                    campo(r.mesAlta())))
                    .append('\n');
        }
        return csv.toString();
    }

    /** Escapa según RFC 4180: comillas dobles si hay coma, comillas o salto de línea. */
    private static String campo(String valor) {
        if (valor == null) {
            return "";
        }
        if (valor.contains(",") || valor.contains("\"") || valor.contains("\n")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }
}
