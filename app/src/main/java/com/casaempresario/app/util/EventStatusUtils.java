package com.casaempresario.app.util;

import com.casaempresario.app.database.Evento;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EventStatusUtils {
    private static final long DURACAO_PADRAO_MS = 3 * 60 * 60 * 1000L;
    private static final long MARGEM_FIM_MS = 60 * 1000L;

    public static Date parseDate(String dataEvento) {
        if (dataEvento == null || dataEvento.trim().isEmpty()) return null;
        String value = dataEvento.trim();

        Date parsed = parseIsoLocal(value);
        if (parsed != null) return parsed;

        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm",
                "dd/MM/yyyy 'às' HH:mm",
                "yyyy-MM-dd HH:mm:ss"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.getDefault());
                formatter.setLenient(false);
                return formatter.parse(value);
            } catch (Exception ignored) { }
        }
        return null;
    }

    private static Date parseIsoLocal(String value) {
        try {
            if (!value.contains("T")) return null;
            String[] parts = value.split("T");
            if (parts.length < 2) return null;

            String[] dateParts = parts[0].split("-");
            String[] timeParts = parts[1].split(":");
            if (dateParts.length < 3 || timeParts.length < 2) return null;

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, Integer.parseInt(dateParts[0]));
            cal.set(Calendar.MONTH, Integer.parseInt(dateParts[1]) - 1);
            cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateParts[2]));
            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
            cal.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
            cal.set(Calendar.SECOND, timeParts.length >= 3 ? Integer.parseInt(timeParts[2]) : 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTime();
        } catch (Exception e) {
            return null;
        }
    }

    public static long getEventTimeMillis(Evento evento) {
        Date date = evento != null ? parseDate(evento.dataEvento) : null;
        return date != null ? date.getTime() : Long.MAX_VALUE;
    }

    public static long getEventEndTimeMillis(Evento evento) {
        Date inicio = evento != null ? parseDate(evento.dataEvento) : null;
        Date fim = evento != null ? parseDate(evento.dataFimEvento) : null;

        if (inicio == null) {
            return Long.MAX_VALUE;
        }

        if (fim == null || !fim.after(inicio)) {
            return inicio.getTime() + DURACAO_PADRAO_MS;
        }

        return fim.getTime();
    }

    public static String calcularStatusAutomatico(Evento evento) {
        if (evento == null) return "AGENDADO";
        if ("CANCELADO".equalsIgnoreCase(evento.status)) return "CANCELADO";

        Date inicioDate = parseDate(evento.dataEvento);
        if (inicioDate == null) {
            return evento.status != null && !evento.status.trim().isEmpty() ? evento.status : "AGENDADO";
        }

        long agora = System.currentTimeMillis();
        long inicio = inicioDate.getTime();
        long fim = getEventEndTimeMillis(evento);

        if (agora < inicio) return "AGENDADO";
        if (agora < fim + MARGEM_FIM_MS) return "EM_ANDAMENTO";
        return "CONCLUIDO";
    }
}
