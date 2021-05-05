/*
 * Copyright 2017 Adam Feinstein
 *
 * This file is part of MTG Familiar.
 *
 * MTG Familiar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MTG Familiar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam.helpers;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.view.ReliableColorPie;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GraphHelper {
    private final Context c;
    private final DeckStatsGenerator mStatGenerator;
    private final int mBackgroundColor;
    private final int mTextColor;
    private final int mTextSize;
    private final int mPieTextColor;

    /**
     * Creates a new GraphHelper
     *
     * @param mStatGenerator DeckStatsGenerator to get stats from
     * @param c              Context to get colors with
     */
    public GraphHelper(DeckStatsGenerator mStatGenerator, Context c) {
        this.mStatGenerator = mStatGenerator;
        this.c = c;
        mBackgroundColor = ContextCompat.getColor(c, FamiliarActivity.getResourceIdFromAttr(c, R.attr.color_background));
        mTextColor = ContextCompat.getColor(c, FamiliarActivity.getResourceIdFromAttr(c, R.attr.color_text));
        mPieTextColor = ContextCompat.getColor(c, R.color.glyph_foreground);
        mTextSize = 15;
    }

    /**
     * Fills all of the stat graphs at once, with the added benefit of adding an invisible legend
     * to equally balance the color graph with the type graph
     *
     * @param chartToFill    type PieChart to fill
     * @param pieToFill      color ReliableColorPie to fill
     * @param barChartToFill cmc BarChart to fill
     */
    public void fillStatGraphs(PieChart chartToFill, ReliableColorPie pieToFill, BarChart barChartToFill) {
        fillTypeGraph(chartToFill);
        fillCmcGraph(barChartToFill);
        fillColorGraph(pieToFill);
    }

    /**
     * Formats a given PieChart for displaying type statistics
     *
     * @param chartToFill PieChart to format
     */
    public void fillTypeGraph(PieChart chartToFill) {
        chartToFill.setData(createTypeData(mStatGenerator.getTypeStats()));
        chartToFill.setCenterText(c.getString(R.string.type_distribution));
        formatPieChart(chartToFill);
    }

    /**
     * Formats a given ReliableColorPie for displaying color statistics
     *
     * @param pieToFill ReliableColorPie to format  d4b8b2
     */
    public void fillColorGraph(ReliableColorPie pieToFill) {
        pieToFill.setData(createColorData(mStatGenerator.getColorStats()));
        pieToFill.setCenterText(c.getString(R.string.mana_value_distribution));
        formatPieChart(pieToFill);
    }

    /**
     * Helper function to format a pie chart in a consistent way
     *
     * @param pc The pie chart to format
     */
    private void formatPieChart(PieChart pc) {
        pc.setEntryLabelColor(mPieTextColor);
        pc.setEntryLabelTextSize(mTextSize);
        pc.getDescription().setEnabled(false);
        pc.getLegend().setEnabled(false);
        pc.setTransparentCircleColor(mBackgroundColor);
        pc.setHoleColor(mBackgroundColor);
        pc.setCenterTextColor(mTextColor);
        pc.setCenterTextSize(mTextSize);
        pc.setTouchEnabled(false);
    }

    /**
     * Formats a given BarChart for displaying cmc statistics
     *
     * @param chartToFill BarChart to format
     */
    public void fillCmcGraph(BarChart chartToFill) {
        BarData cmcData = createCmcData(mStatGenerator.getCmcStats());
        chartToFill.setData(cmcData);
        chartToFill.setTouchEnabled(false);
        chartToFill.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                if (value == 7) {
                    return "7+";
                } else {
                    return Integer.toString((int) value); //No decimals
                }
            }
        });
        chartToFill.getXAxis().setDrawGridLines(false);
        chartToFill.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE);
        chartToFill.getXAxis().setTextColor(mTextColor);
        chartToFill.getXAxis().setTextSize(mTextSize);
        chartToFill.getBarData().setValueTextColor(mTextColor);
        chartToFill.getBarData().setValueTextSize(mTextSize);
        chartToFill.getAxisLeft().setEnabled(false);
        chartToFill.getAxisRight().setEnabled(false);
        chartToFill.getDescription().setEnabled(false);

        Legend barLegend = chartToFill.getLegend();
        barLegend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        barLegend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        barLegend.setForm(Legend.LegendForm.NONE);
        barLegend.setTextSize(mTextSize);
        barLegend.setTextColor(mTextColor);
    }

    /**
     * Creates a PieData from the type Map supplied by the DeckStatsGenerator
     *
     * @param typeMap Map with type information
     * @return PieData containing type information
     */
    private PieData createTypeData(Map<String, Float> typeMap) {
        List<PieEntry> typeEntries = new ArrayList<>();
        for (String type : typeMap.keySet()) {
            if (typeMap.get(type) != 0) {
                typeEntries.add(new PieEntry(typeMap.get(type), type));
            }
        }
        PieDataSet typeSet = new PieDataSet(typeEntries, "");
        typeSet.setColors(new int[]{
                R.color.glyph_white,
                R.color.glyph_blue,
                R.color.glyph_black,
                R.color.glyph_red,
                R.color.glyph_green,
                R.color.glyph_grey,
                R.color.timeshifted_light}, c);
        PieData typeData = new PieData(typeSet);
        typeData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getPieLabel(float value, PieEntry pieEntry) {
                return "";
            }
        });
        return typeData;
    }

    /**
     * Creates a PieData from the color Map supplied by the DeckStatsGenerator
     *
     * @param colorMap Map with color information
     * @return PieData containing color information
     */
    private PieData createColorData(Map<String, Integer> colorMap) {
        List<PieEntry> colorEntries = new ArrayList<>();
        for (String color : colorMap.keySet()) {
            if (colorMap.get(color) != 0) {
                if (!color.isEmpty()) {
                    colorEntries.add(new PieEntry(colorMap.get(color), color + ": " + colorMap.get(color).toString()));
                } else {
                    colorEntries.add(new PieEntry(colorMap.get(color), "C")); // C for colorless
                }
            }
        }
        PieDataSet colorSet = new PieDataSet(colorEntries, c.getString(R.string.card_colors));
        colorSet.setColors(new int[]{
                R.color.glyph_white,
                R.color.icon_blue,
                R.color.icon_black,
                R.color.icon_red,
                R.color.icon_green,
                R.color.glyph_grey}, c);
        PieData colorData = new PieData(colorSet);
        colorData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getPieLabel(float value, PieEntry pieEntry) {
                return "";
            }
        });

        return colorData;
    }

    /**
     * Creates a formatted BarData from the cmc Map supplied by the DeckStatsGenerator
     *
     * @param cmcMap Map containing number of cards with each cmc
     * @return Formatted BarData containing the cmc information
     */
    private BarData createCmcData(Map<Integer, Integer> cmcMap) {
        List<BarEntry> cmcEntries = new ArrayList<>();
        for (Integer cmc : cmcMap.keySet()) {
            cmcEntries.add(new BarEntry(cmc, cmcMap.get(cmc)));
        }
        BarDataSet cmcSet = new BarDataSet(cmcEntries, c.getString(R.string.cmc_graph));
        cmcSet.setColors(new int[]{R.color.dark_grey}, c);
        BarData cmcData = new BarData(cmcSet);
        cmcData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getBarLabel(BarEntry barEntry) {
                if (barEntry.getY() == 0) {
                    return "";
                } else {
                    return Integer.toString((int) barEntry.getY()); //Don't show decimals
                }
            }
        });
        return cmcData;
    }
}
