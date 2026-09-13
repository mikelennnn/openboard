/*
 * Panel para elegir con que estilo de fuente reescribir el texto. No usa IA:
 * es conversion de caracteres Unicode, instantanea. Se abre al tocar el
 * boton del lapiz, a la izquierda de la barra.
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

public final class FontPanelView extends LinearLayout implements View.OnClickListener {

    private LinearLayout mStyleList;
    private TextView mStatusText;
    private Button mBackButton;

    private LatinIME mLatinIME;
    private String mOriginalText = "";
    private int mOriginalLength = 0;

    public FontPanelView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mStyleList = findViewById(R.id.font_panel_style_list);
        mStatusText = findViewById(R.id.font_panel_status);
        mBackButton = findViewById(R.id.font_panel_back_button);
        mBackButton.setOnClickListener(this);

        for (int i = 0; i < FontStyles.STYLE_KEYS_IN_ORDER.length; i++) {
            final String key = FontStyles.STYLE_KEYS_IN_ORDER[i];
            final String label = FontStyles.STYLE_LABELS_IN_ORDER[i];
            final Button button = new Button(getContext());
            button.setText(label);
            button.setTag(key);
            button.setOnClickListener(this);
            mStyleList.addView(button);
        }
    }

    /**
     * Se llama al tocar el boton del lapiz, con el texto que el usuario ya escribio.
     */
    public void open(final LatinIME latinIME, final String text, final int textLength) {
        mLatinIME = latinIME;
        mOriginalText = text;
        mOriginalLength = textLength;
        if (mStatusText != null) {
            mStatusText.setVisibility(TextUtils.isEmpty(text) ? VISIBLE : GONE);
        }
    }

    @Override
    public void onClick(final View view) {
        if (view == mBackButton) {
            if (mLatinIME != null) {
                mLatinIME.hideFontPanel();
            }
            return;
        }
        final Object tag = view.getTag();
        if (tag instanceof String) {
            applyStyle((String) tag);
        }
    }

    private void applyStyle(final String styleKey) {
        if (mLatinIME == null || TextUtils.isEmpty(mOriginalText)) {
            return;
        }
        final String converted = FontStyles.convert(mOriginalText, styleKey);
        final InputConnection ic = mLatinIME.getCurrentInputConnection();
        if (ic != null) {
            ic.deleteSurroundingText(mOriginalLength, 0);
            ic.commitText(converted, 1);
        }
        mLatinIME.hideFontPanel();
    }
}

