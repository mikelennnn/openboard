/*
 * Archivo agregado para la funcion del panel de IA y el boton CORREGIR del teclado.
 * Envia texto a Groq (API compatible con OpenAI) y devuelve la respuesta.
 * (El nombre del archivo/clase quedo como OpenRouterAI por historia, pero ahora
 * habla con la API de Groq, no con OpenRouter ni con Gemini.)
 */
package org.dslul.openboard.inputmethod.latin;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class OpenRouterAI {

    private static final String TAG = "OpenRouterAI";

    // TODO: si cambias de cuenta o de modelo, edita estas dos lineas.
    private static final String API_KEY = "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";
    private static final String MODEL = "openai/gpt-oss-20b";

    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";

    private static final int MAX_RETRIES_503 = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 2000L;

    public interface Callback {
        void onResult(String responseText);
        void onError(String errorMessage);
    }

    private OpenRouterAI() {
        // No instanciable.
    }

    /**
     * Manda el texto a la IA en un hilo aparte y devuelve el resultado en el hilo principal
     * a traves del Callback.
     */
    public static void send(final String prompt, final Callback callback) {
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String result = requestCompletionWithRetry(prompt);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onResult(result);
                        }
                    });
                } catch (final Exception e) {
                    Log.e(TAG, "Error llamando a Groq", e);
                    final String message = e.getMessage() != null ? e.getMessage() : "Error desconocido";
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(message);
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * Excepcion interna que guarda el codigo HTTP recibido, para poder decidir
     * si conviene reintentar (503) o no (401, 429, etc.).
     */
    private static final class HttpStatusException extends IOException {
        final int statusCode;

        HttpStatusException(final int statusCode, final String message) {
            super(message);
            this.statusCode = statusCode;
        }
    }

    /**
     * Igual que requestCompletion, pero si el servidor responde 503 (ocupado/caido
     * momentaneamente), espera un poco y reintenta, duplicando la espera cada vez
     * (2s, 4s, 8s). Si tras varios intentos sigue fallando, ahi si se rinde.
     */
    private static String requestCompletionWithRetry(final String prompt) throws IOException {
        long delay = INITIAL_RETRY_DELAY_MS;
        for (int attempt = 0; ; attempt++) {
            try {
                return requestCompletion(prompt);
            } catch (final HttpStatusException e) {
                final boolean isLastAttempt = attempt >= MAX_RETRIES_503;
                if (e.statusCode != 503 || isLastAttempt) {
                    throw e;
                }
                Log.w(TAG, "Error 503 de Groq, reintentando en " + delay + " ms"
                        + " (intento " + (attempt + 1) + " de " + MAX_RETRIES_503 + ")");
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
                delay *= 2;
            }
        }
    }

    private static String requestCompletion(final String prompt) throws IOException {
        final URL url = new URL(ENDPOINT);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
            connection.setDoOutput(true);
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(30000);

            final JSONObject body = new JSONObject();
            try {
                body.put("model", MODEL);
                final JSONArray messages = new JSONArray();
                final JSONObject message = new JSONObject();
                message.put("role", "user");
                message.put("content", prompt);
                messages.put(message);
                body.put("messages", messages);
            } catch (Exception e) {
                throw new IOException("No se pudo armar la peticion", e);
            }

            final OutputStream os = connection.getOutputStream();
            try {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            } finally {
                os.close();
            }

            final int status = connection.getResponseCode();
            final InputStream stream = (status >= 200 && status < 300)
                    ? connection.getInputStream() : connection.getErrorStream();
            final String rawResponse = readStream(stream);

            if (status < 200 || status >= 300) {
                throw new HttpStatusException(status,
                        "Groq respondio con codigo " + status + ": " + rawResponse);
            }

            final JSONObject json = new JSONObject(rawResponse);
            return json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Error inesperado hablando con Groq", e);
        } finally {
            connection.disconnect();
        }
    }

    private static String readStream(final InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        final BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } finally {
            reader.close();
        }
        return sb.toString();
    }
}
