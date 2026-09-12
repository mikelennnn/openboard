/*
 * Archivo agregado para la funcion de boton "IA" del teclado.
 * Envia el texto ya escrito por el usuario a OpenRouter y devuelve la respuesta.
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
    private static final String MODEL = "openrouter/free";

    private static final String ENDPOINT = "https://openrouter.ai/api/v1/chat/completions";

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
                    final String result = requestCompletion(prompt);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onResult(result);
                        }
                    });
                } catch (final Exception e) {
                    Log.e(TAG, "Error llamando a OpenRouter", e);
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
                throw new IOException("OpenRouter respondio con codigo " + status + ": " + rawResponse);
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
            throw new IOException("Error inesperado hablando con OpenRouter", e);
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
