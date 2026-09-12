/*
 * Panel de chat con la IA. Se muestra en vez del teclado cuando se toca el boton "IA".
 * Guarda el chat en el celular; si pasan 24 horas sin usarlo, borra lo visible pero
 * conserva un resumen corto para que la IA siga teniendo algo de contexto.
 */
package org.dslul.openboard.inputmethod.latin;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class AiPanelView extends LinearLayout implements View.OnClickListener {

    private static final String PREFS_NAME = "ai_panel_prefs";
    private static final String KEY_CHAT_JSON = "chat_json";
    private static final String KEY_LAST_TIME = "chat_last_time";
    private static final String KEY_MEMORY = "chat_memory";
    private static final long EXPIRY_MILLIS = 24L * 60 * 60 * 1000; // 24 horas

    private LinearLayout mChatContainer;
    private ScrollView mScrollView;
    private EditText mInputField;
    private Button mAskButton;
    private Button mInsertButton;
    private Button mBackButton;

    private LatinIME mLatinIME;
    private final List<String[]> mMessages = new ArrayList<>(); // {rol, texto}
    private String mLastAnswer = "";

    public AiPanelView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mChatContainer = findViewById(R.id.ai_panel_chat_container);
        mScrollView = findViewById(R.id.ai_panel_scroll);
        mInputField = findViewById(R.id.ai_panel_input);
        mAskButton = findViewById(R.id.ai_panel_ask_button);
        mInsertButton = findViewById(R.id.ai_panel_insert_button);
        mBackButton = findViewById(R.id.ai_panel_back_button);
        mAskButton.setOnClickListener(this);
        mInsertButton.setOnClickListener(this);
        mBackButton.setOnClickListener(this);
    }

    /**
     * Se llama cada vez que se abre el panel (al tocar el boton "IA").
     */
    public void open(final LatinIME latinIME) {
        mLatinIME = latinIME;
        loadChatFromStorage();
        renderMessages();
    }

    @Override
    public void onClick(final View view) {
        if (view == mAskButton) {
            askQuestion();
        } else if (view == mInsertButton) {
            insertLastAnswer();
        } else if (view == mBackButton) {
            if (mLatinIME != null) {
                mLatinIME.hideAiPanel();
            }
        }
    }

    private void askQuestion() {
        if (mInputField == null || mLatinIME == null) {
            return;
        }
        final String question = mInputField.getText().toString().trim();
        if (TextUtils.isEmpty(question)) {
            return;
        }
        mInputField.setText("");
        addMessage("user", question);
        renderMessages();
        mAskButton.setEnabled(false);

        final String prompt = buildPromptWithHistory();

        OpenRouterAI.send(prompt, new OpenRouterAI.Callback() {
            @Override
            public void onResult(final String responseText) {
                mAskButton.setEnabled(true);
                final String answer = TextUtils.isEmpty(responseText)
                        ? "(sin respuesta)" : responseText;
                addMessage("assistant", answer);
                mLastAnswer = answer;
                renderMessages();
                saveChatToStorage();
            }

            @Override
            public void onError(final String errorMessage) {
                mAskButton.setEnabled(true);
                addMessage("assistant", "Error: " + errorMessage);
                mLastAnswer = "";
                renderMessages();
                saveChatToStorage();
            }
        });
    }

    private void insertLastAnswer() {
        if (mLatinIME == null || TextUtils.isEmpty(mLastAnswer)) {
            return;
        }
        final InputConnection ic = mLatinIME.getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(mLastAnswer, 1);
        }
        mLatinIME.hideAiPanel();
    }

    private void addMessage(final String role, final String text) {
        mMessages.add(new String[]{role, text});
    }

    private String buildPromptWithHistory() {
        final StringBuilder sb = new StringBuilder();
        final String memory = loadMemory();
        if (!TextUtils.isEmpty(memory)) {
            sb.append("Contexto de conversaciones anteriores (usalo si es util, no lo repitas): ")
                    .append(memory).append("\n\n");
        }
        for (final String[] message : mMessages) {
            if ("user".equals(message[0])) {
                sb.append("Usuario: ").append(message[1]).append("\n");
            } else {
                sb.append("Asistente: ").append(message[1]).append("\n");
            }
        }
        return sb.toString();
    }

    private void renderMessages() {
        if (mChatContainer == null) {
            return;
        }
        mChatContainer.removeAllViews();
        for (final String[] message : mMessages) {
            final TextView bubble = new TextView(getContext());
            final boolean isUser = "user".equals(message[0]);
            bubble.setText((isUser ? "Tu: " : "IA: ") + message[1]);
            bubble.setPadding(8, 6, 8, 6);
            mChatContainer.addView(bubble);
        }
        if (mScrollView != null) {
            mScrollView.post(new Runnable() {
                @Override
                public void run() {
                    mScrollView.fullScroll(View.FOCUS_DOWN);
                }
            });
        }
    }

    private void loadChatFromStorage() {
        mMessages.clear();
        mLastAnswer = "";
        if (mLatinIME == null) {
            return;
        }
        final SharedPreferences prefs =
                mLatinIME.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        final long lastTime = prefs.getLong(KEY_LAST_TIME, 0L);
        final long now = System.currentTimeMillis();
        if (now - lastTime > EXPIRY_MILLIS) {
            // Pasaron mas de 24 horas: se borra el chat visible, la memoria se conserva.
            return;
        }
        final String json = prefs.getString(KEY_CHAT_JSON, null);
        if (TextUtils.isEmpty(json)) {
            return;
        }
        try {
            final JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                final JSONObject obj = array.getJSONObject(i);
                final String role = obj.optString("role", "user");
                final String text = obj.optString("text", "");
                mMessages.add(new String[]{role, text});
                if ("assistant".equals(role)) {
                    mLastAnswer = text;
                }
            }
        } catch (Exception e) {
            mMessages.clear();
        }
    }

    private void saveChatToStorage() {
        if (mLatinIME == null) {
            return;
        }
        final JSONArray array = new JSONArray();
        try {
            for (final String[] message : mMessages) {
                final JSONObject obj = new JSONObject();
                obj.put("role", message[0]);
                obj.put("text", message[1]);
                array.put(obj);
            }
        } catch (Exception e) {
            // Si algo falla armando el JSON, seguimos con lo que se pudo armar.
        }
        mLatinIME.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CHAT_JSON, array.toString())
                .putLong(KEY_LAST_TIME, System.currentTimeMillis())
                .apply();
        saveMemory();
    }

    private void saveMemory() {
        if (mLatinIME == null) {
            return;
        }
        // Guardamos un resumen corto de los ultimos intercambios para que la IA
        // siga recordando algo aunque el chat visible se borre a las 24 horas.
        final StringBuilder sb = new StringBuilder();
        final int start = Math.max(0, mMessages.size() - 6);
        for (int i = start; i < mMessages.size(); i++) {
            final String[] message = mMessages.get(i);
            sb.append("user".equals(message[0]) ? "Usuario dijo: " : "IA respondio: ")
                    .append(message[1]).append(". ");
        }
        String memory = sb.toString();
        if (memory.length() > 1500) {
            memory = memory.substring(memory.length() - 1500);
        }
        mLatinIME.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_MEMORY, memory)
                .apply();
    }

    private String loadMemory() {
        if (mLatinIME == null) {
            return "";
        }
        return mLatinIME.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_MEMORY, "");
    }
}
