package com.gelakinetic.mtgfam.fragments;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.DeckStatsGenerator;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeckStatsFragment extends FamiliarFragment {
    private DeckStatsGenerator mStatGenerator;
    private final List<MtgCard> mDeckToStat;
    private final Map<String, Integer> mColorChartColors;
    private PieChart mTypeChart;
    private PieChart mColorChart;
    private BarChart mCmcChart;

    public DeckStatsFragment(List<MtgCard> mDeckToStat) {
        this.mDeckToStat = mDeckToStat;
        mColorChartColors = new HashMap<>();
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View myFragmentView =
                inflater.inflate(R.layout.stat_frag, container, false);
        assert myFragmentView != null;
        mStatGenerator = new DeckStatsGenerator(mDeckToStat);

        mColorChartColors.put("W", getContext().getResources().getColor(R.color.glyph_white));
        mColorChartColors.put("U", getContext().getResources().getColor(R.color.icon_blue));
        mColorChartColors.put("B", getContext().getResources().getColor(R.color.bpblack));
        mColorChartColors.put("R", getContext().getResources().getColor(R.color.icon_red));
        mColorChartColors.put("G", getContext().getResources().getColor(R.color.icon_green));
        mColorChartColors.put("", getContext().getResources().getColor(R.color.light_grey));
        mTypeChart = (PieChart) myFragmentView.findViewById(R.id.type_chart);
        mColorChart = (PieChart) myFragmentView.findViewById(R.id.color_chart);
        mCmcChart = (BarChart) myFragmentView.findViewById(R.id.cmc_graph);
        //Type graph
        List<PieEntry> typeEntries = new ArrayList<>();
        Map<String, Float> typeMap = mStatGenerator.getTypeStats();
        for (String type : typeMap.keySet()) {
            if (typeMap.get(type) != 0) {
                typeEntries.add(new PieEntry(typeMap.get(type), type));
            }
        }
        PieDataSet typeSet = new PieDataSet(typeEntries, "Card Types");
        typeSet.setColors(new int[] {R.color.bpblack, R.color.bpBlue, R.color.bpDarker_red, R.color.glyph_green, R.color.bpLight_gray, R.color.mythic_light}, getContext());
        PieData typeData = new PieData(typeSet);
        typeData.setValueFormatter(new PercentFormatter());
        mTypeChart.setData(typeData);
        mTypeChart.getDescription().setEnabled(false);
        mTypeChart.getLegend().setWordWrapEnabled(true);
        mTypeChart.invalidate();
        //Color graph
        List<PieEntry> colorEntries = new ArrayList<>();
        Map<String, Float> colorMap = mStatGenerator.getColorStats();
        PieDataSet colorSet = new PieDataSet(colorEntries, "Card Colors");
        for (String color : colorMap.keySet()) {
            if (colorMap.get(color) != 0) {
                if (!color.isEmpty()) {
                    colorEntries.add(new PieEntry(colorMap.get(color), color));
                    colorSet.addColor(mColorChartColors.get(color));
                } else {
                    colorEntries.add(new PieEntry(colorMap.get(color), "Colorless"));
                    colorSet.addColor(mColorChartColors.get(color));
                }
            }
        }
        colorSet.notifyDataSetChanged();
        PieData colorData = new PieData(colorSet);
        colorData.setValueFormatter(new PercentFormatter());
        mColorChart.setData(colorData);
        mColorChart.getDescription().setEnabled(false);
        mColorChart.getLegend().setWordWrapEnabled(true);
        mColorChart.invalidate();
        //Cmc graph
        List<BarEntry> cmcEntries = new ArrayList<>();
        Map<Integer, Integer> cmcMap = mStatGenerator.getCmcStats();
        for (Integer cmc : cmcMap.keySet()) {
            cmcEntries.add(new BarEntry(cmc, cmcMap.get(cmc)));
        }
        BarDataSet cmcSet = new BarDataSet(cmcEntries, "CMC Graph");
        cmcSet.setColors(new int[] {R.color.dark_grey}, getContext());
        BarData cmcData = new BarData(cmcSet);
        cmcData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getBarLabel(BarEntry barEntry) {
                return Integer.toString((int) barEntry.getY());
            }
        });
        mCmcChart.setData(cmcData);
        mCmcChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                if (value == 7) {
                    return "7+";
                } else {
                    return Integer.toString((int) value);
                }
            }
        });
        mCmcChart.getXAxis().setDrawGridLines(false);
        mCmcChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE);

        mCmcChart.getAxisLeft().setEnabled(false);
        mCmcChart.getAxisRight().setEnabled(false);
        mCmcChart.getDescription().setEnabled(false);
        mCmcChart.getLegend().setEnabled(false);
        mCmcChart.invalidate();
        return myFragmentView;
    }
}
