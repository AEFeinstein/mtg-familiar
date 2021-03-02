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
import com.gelakinetic.mtgfam.helpers.view.ReliableColorPie;
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
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DeckStatsFragment extends FamiliarFragment {
    private DeckStatsGenerator mStatGenerator;
    private final List<MtgCard> mDeckToStat;
    /* UI Elements */
    private PieChart mTypeChart;
    private PieChart mColorChart;
    private BarChart mCmcChart;

    public DeckStatsFragment(List<MtgCard> mDeckToStat) {
        this.mDeckToStat = mDeckToStat;
    }

    /**
     * Create the view, pull out UI elements, and set up the listener for the "add cards" button.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in
     *                           the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should
     *                           be attached to. The fragment should not add the view itself, but
     *                           this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous
     *                           saved state as given here.
     * @return The view to be displayed.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View myFragmentView =
                inflater.inflate(R.layout.stat_frag, container, false);
        assert myFragmentView != null;
        mStatGenerator = new DeckStatsGenerator(mDeckToStat);
        mTypeChart = myFragmentView.findViewById(R.id.type_chart);
        mColorChart = (ReliableColorPie) myFragmentView.findViewById(R.id.color_chart);
        mCmcChart = myFragmentView.findViewById(R.id.cmc_graph);
        //Type graph
        PieData typeData = createTypeData(mStatGenerator.getTypeStats());
        mTypeChart.setData(typeData);
        //mTypeChart.setDrawEntryLabels(false);
        mTypeChart.getDescription().setEnabled(false);
        mTypeChart.getLegend().setEnabled(false);
        //mTypeChart.getLegend().setWordWrapEnabled(true);
        //mTypeChart.getLegend().setMaxSizePercent((float) 0.1);
        mTypeChart.setCenterText("Type Distribution");
        mTypeChart.setTouchEnabled(false);
        mTypeChart.invalidate(); //refresh
        //Color graph
        PieData colorData = createColorData(mStatGenerator.getColorStats());
        mColorChart.setData(colorData);
        mColorChart.setDrawEntryLabels(false);
        mColorChart.setTouchEnabled(false);
        mColorChart.getDescription().setEnabled(false);
        mColorChart.getLegend().setEnabled(false);
        mColorChart.setCenterText("Color Distribution");
        mColorChart.invalidate(); //refresh
        //Cmc graph
        BarData cmcData = createCmcData(mStatGenerator.getCmcStats());
        mCmcChart.setData(cmcData);
        mCmcChart.setTouchEnabled(false);
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
        mCmcChart.invalidate(); //refresh
        return myFragmentView;
    }

    /**
     * Creates a PieData from the type Map supplied by the DeckStatsGenerator
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
        typeSet.setColors(new int[] {R.color.bpblack, R.color.bpBlue, R.color.bpDarker_red, R.color.glyph_green, R.color.bpLight_gray, R.color.mythic_light}, getContext());
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
     * @param colorMap Map with color information
     * @return PieData containing color information
     */
    private PieData createColorData(Map<String, Float> colorMap) {
        List<PieEntry> colorEntries = new ArrayList<>();
        for (String color : colorMap.keySet()) {
            if (colorMap.get(color) != 0) {
                if (!color.isEmpty()) {
                    colorEntries.add(new PieEntry(colorMap.get(color), color));
                } else {
                    colorEntries.add(new PieEntry(colorMap.get(color), "Colorless"));
                }
            }
        }
        PieDataSet colorSet = new PieDataSet(colorEntries, "Card Colors");
        colorSet.setColors(new int[] {R.color.glyph_white, R.color.icon_blue, R.color.bpblack, R.color.icon_red, R.color.icon_green, R.color.light_grey}, getContext());
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
     * @param cmcMap Map containing number of cards with each cmc
     * @return Formatted BarData containing the cmc information
     */
    private BarData createCmcData(Map<Integer, Integer> cmcMap) {
        List<BarEntry> cmcEntries = new ArrayList<>();
        for (Integer cmc : cmcMap.keySet()) {
            cmcEntries.add(new BarEntry(cmc, cmcMap.get(cmc)));
        }
        BarDataSet cmcSet = new BarDataSet(cmcEntries, "CMC Graph");
        cmcSet.setColors(new int[] {R.color.dark_grey}, getContext());
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
