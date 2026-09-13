/*
 * Panel de simbolos organizados por categoria. A diferencia de los paneles de
 * traducir y fuentes, este se comporta como el selector de emojis: se queda
 * abierto y cada toque inserta el simbolo directamente, sin cerrar el panel.
 */
package org.dslul.openboard.inputmethod.latin;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;

public final class SymbolPanelView extends LinearLayout implements View.OnClickListener {

    private static final int SYMBOLS_PER_ROW = 8;

    private LinearLayout mCategoryTabs;
    private LinearLayout mGrid;
    private Button mBackButton;

    private LatinIME mLatinIME;
    private int mCurrentCategory = 0;

    public SymbolPanelView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mCategoryTabs = findViewById(R.id.symbol_panel_category_tabs);
        mGrid = findViewById(R.id.symbol_panel_grid);
        mBackButton = findViewById(R.id.symbol_panel_back_button);
        mBackButton.setOnClickListener(this);

        for (int i = 0; i < SymbolData.CATEGORY_NAMES.length; i++) {
            final Button tab = new Button(getContext());
            tab.setText(SymbolData.CATEGORY_NAMES[i]);
            tab.setTag(i);
            tab.setAllCaps(false);
            tab.setOnClickListener(this);
            mCategoryTabs.addView(tab);
        }
    }

    /**
     * Se llama al tocar el boton de simbolos. No necesita texto previo: es
     * una herramienta de insercion, como el teclado de emojis.
     */
    public void open(final LatinIME latinIME) {
        mLatinIME = latinIME;
        showCategory(mCurrentCategory);
    }

    @Override
    public void onClick(final View view) {
        if (view == mBackButton) {
            if (mLatinIME != null) {
                mLatinIME.hideSymbolPanel();
            }
            return;
        }
        final Object tag = view.getTag();
        if (tag instanceof Integer) {
            showCategory((Integer) tag);
            return;
        }
        if (tag instanceof String) {
            insertSymbol((String) tag);
        }
    }

    private void showCategory(final int categoryIndex) {
        mCurrentCategory = categoryIndex;
        mGrid.removeAllViews();

        final List<String> symbols = splitToGraphemes(SymbolData.CATEGORY_SYMBOLS[categoryIndex]);

        LinearLayout currentRow = null;
        for (int i = 0; i < symbols.size(); i++) {
            if (i % SYMBOLS_PER_ROW == 0) {
                currentRow = new LinearLayout(getContext());
                currentRow.setOrientation(HORIZONTAL);
                mGrid.addView(currentRow);
            }
            final String symbol = symbols.get(i);
            final TextView cell = new TextView(getContext());
            cell.setText(symbol);
            cell.setTextSize(20f);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(12, 16, 12, 16);
            cell.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            cell.setBackgroundResource(android.R.drawable.list_selector_background);
            cell.setClickable(true);
            cell.setTag(symbol);
            cell.setOnClickListener(this);
            currentRow.addView(cell);
        }
    }

    private void insertSymbol(final String symbol) {
        if (mLatinIME == null) {
            return;
        }
        final InputConnection ic = mLatinIME.getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(symbol, 1);
        }
        // El panel se queda abierto para seguir insertando simbolos.
    }

    /**
     * Separa un bloque de texto en "simbolos" respetando pares subrogados y
     * marcas combinantes/selectores de variacion, para que no se corten a
     * la mitad de un simbolo compuesto.
     */
    private static List<String> splitToGraphemes(final String text) {
        final List<String> result = new ArrayList<>();
        final BreakIterator iterator = BreakIterator.getCharacterInstance();
        iterator.setText(text);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE;
                start = end, end = iterator.next()) {
            final String piece = text.substring(start, end).trim();
            if (!piece.isEmpty()) {
                result.add(piece);
            }
        }
        return result;
    }
}

