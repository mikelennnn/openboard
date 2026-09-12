/*
 * Panel para elegir a que idioma traducir. Se abre al tocar el boton de traducir
 * (a la izquierda de la barra). Muestra una lista de idiomas; al tocar uno, traduce
 * el texto ya escrito y lo reemplaza automaticamente, sin pasos extra.
 */
package org.dslul.openboard.inputmethod.latin;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class TranslatePanelView extends LinearLayout implements View.OnClickListener {

    private static final String[] LANGUAGES = {
            "Ingles", "Frances", "Aleman", "Italiano", "Portugues",
            "Japones", "Chino", "Coreano", "Ruso", "Arabe", "Espanol"
    };

    private LinearLayout mLanguageList;
    private TextView mStatusText;
    private Button mBackButton;

    private LatinIME mLatinIME;
    private String mOriginalText = "";
    private int mOriginalLength = 0;

    public TranslatePanelView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mLanguageList = findViewById(R.id.translate_panel_language_list);
        mStatusText = findViewById(R.id.translate_panel_status);
        mBackButton = findViewById(R.id.translate_panel_back_button);
        mBackButton.setOnClickListener(this);

        for (final String language : LANGUAGES) {
            final Button button = new Button(getContext());
            button.setText(language);
            button.setTag(language);
            button.setOnClickListener(this);
            mLanguageList.addView(button);
        }
    }

    /**
     * Se llama al tocar el boton de traducir, con el texto que el usuario ya escribio.
     */
    public void open(final LatinIME latinIME, final String text, final int textLength) {
        mLatinIME = latinIME;
        mOriginalText = text;
        mOriginalLength = textLength;
        if (mStatusText != null) {
            mStatusText.setVisibility(GONE);
        }
    }

    @Override
    public void onClick(final View view) {
        if (view == mBackButton) {
            if (mLatinIME != null) {
                mLatinIME.hideTranslatePanel();
            }
            return;
        }
        final Object tag = view.getTag();
        if (tag instanceof String) {
            translateTo((String) tag);
        }
    }

    private void translateTo(final String language) {
        if (mLatinIME == null || TextUtils.isEmpty(mOriginalText)) {
            return;
        }
        if (mStatusText != null) {
            mStatusText.setVisibility(VISIBLE);
            mStatusText.setText("Traduciendo a " + language + "...");
        }

        final String prompt = "Traduce el siguiente texto al idioma " + language
                + ", detectando automaticamente en que idioma esta escrito originalmente. "
                + "Responde unicamente con la traduccion, sin explicaciones, notas ni "
                + "comillas adicionales:\n\n" + mOriginalText;

        OpenRouterAI.send(prompt, new OpenRouterAI.Callback() {
            @Override
            public void onResult(final String responseText) {
                if (!TextUtils.isEmpty(responseText)) {
                    final InputConnection ic = mLatinIME.getCurrentInputConnection();
                    if (ic != null) {
                        ic.deleteSurroundingText(mOriginalLength, 0);
                        ic.commitText(responseText, 1);
                    }
                }
                mLatinIME.hideTranslatePanel();
            }

            @Override
            public void onError(final String errorMessage) {
                if (mStatusText != null) {
                    mStatusText.setText("Error: " + errorMessage);
                }
            }
        });
    }
}
