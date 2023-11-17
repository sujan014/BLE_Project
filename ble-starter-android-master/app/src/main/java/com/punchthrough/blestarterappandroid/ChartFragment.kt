/*
 * Copyright 2021 Punch Through Design LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.punchthrough.blestarterappandroid

import android.graphics.Color
import android.graphics.DashPathEffect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.android.synthetic.main.fragment_chart.logchart
import org.bouncycastle.asn1.x500.style.RFC4519Style.l


//import kotlinx.android.synthetic.main.fragment_chart.tvScanRate

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ChartFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ChartFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var meas_param: Int = 1
    var markTime = StampTime(0, 0, 0, 0 ,0 ,0)
    var startTime = StampTime(0, 0, 0, 0 ,0 ,0)
    var this_list = ArrayList<DataFrame>()
    var dataPoints = 30
    val dayofmonth: IntArray = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }*/
        //var args: Bundle? = arguments
        arguments?.let { args ->
            if (args.containsKey("MEAS_EXTRA")){
                meas_param = args.getInt("MEAS_EXTRA")
            }
            if (args.containsKey("TIME_EXTRA")){
                markTime = args.getParcelable<StampTime>("TIME_EXTRA")!!
                Log.d("TimeStamp", "${markTime.year}-${markTime.month}-${markTime.day}  ${markTime.hour}:${markTime.min}:${markTime.sec}")
            }
            if (args.containsKey("ARRAY")) {
                Log.d("FragTest", "Contains Bundle")
                this_list = args.getParcelableArrayList<DataFrame>("ARRAY") as ArrayList<DataFrame>
                /*this_list.forEach {
                    Log.d("FragTest", "${it.count} : ${it.temperature}, ${it.humidity}")
                }*/
            }
        }
        startTime = markTime
        Log.d("FragTest","Chart OnCreate / List size: ${this_list.size}")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("FragTest","Chart OnCreateView")
        var view: View = inflater.inflate(R.layout.fragment_chart, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("FragTest","Chart onViewCreated")
        setChart_Parameters()
    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ChartFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ChartFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
    private fun setChart_Parameters()
    {
        val numMap: HashMap<Int, String> = HashMap()
        var i = 1
        this_list.forEach{
            numMap[i] = it.datetime.toString()
            i++
        }

        logchart.apply{
            //setBackgroundColor(Color.WHITE)
            description.isEnabled=false
            setTouchEnabled(true)
            //setOnChartValueSelectedListener(this)
            setDrawGridBackground(false)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(false)     // x and y zoom separately
            //setBackgroundColor(Color.rgb(0, 0, 230))
        }
        var l1 = LegendEntry("Temperature", Legend.LegendForm.SQUARE, 10f, 5f, null, Color.RED).apply {
        }

        var l2 = LegendEntry("Humidity", Legend.LegendForm.SQUARE, 10f, 5f, null, Color.GREEN)
        var legend: Legend = logchart.legend.apply {
            form = Legend.LegendForm.SQUARE
            textSize = 20f
            //textColor = Color.YELLOW
            verticalAlignment = Legend.LegendVerticalAlignment.TOP
            horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            orientation = Legend.LegendOrientation.VERTICAL
            //setCustom(LegendEntry)
            setCustom(arrayOf<LegendEntry>(l1, l2))
            setDrawInside(true)
            isEnabled = true
        }

        var xAxis: XAxis = logchart.xAxis.apply {
            textColor = Color.WHITE
        }
        logchart.axisRight.isEnabled = false
        var yAxis: YAxis = logchart.axisLeft.apply {
            enableGridDashedLine(10f, 10f, 0f)
            textColor = Color.WHITE
            axisMaximum = 150f
            axisMinimum = -50f
        }
        var llAXis = LimitLine(9f, "Index 10").apply {
            setLineWidth(4f)
            enableDashedLine(10f, 10f, 0f)
            setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM)
            setTextSize(10f)
        }
        val ll1 = LimitLine(150f, "Upper Limit").apply {
            lineWidth = 4f
            enableDashedLine(10f, 10f, 0f)
            labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
            textSize = 10f
        }
        val ll2 = LimitLine(0f, "Lower Limit").apply{
            lineWidth = 4f
            enableDashedLine(10f, 10f, 0f)
            labelPosition = LimitLine.LimitLabelPosition.RIGHT_BOTTOM
            textSize = 10f
        }
        var value1 = ArrayList<Entry>().apply {
            this_list.forEach {
                add(Entry(it.count.toFloat(), it.temperature))
            }
        }
        var value2 = ArrayList<Entry>().apply {
            this_list.forEach {
                add(Entry(it.count.toFloat(), it.humidity))
            }
        }

        val set1: LineDataSet
        val set2: LineDataSet
        if (logchart.data != null && logchart.data.dataSetCount > 0
        ) {
            set1 = logchart.data.getDataSetByIndex(0) as LineDataSet
            set2 = logchart.data.getDataSetByIndex(1) as LineDataSet
            set1.values = value1
            set2.values = value2
            logchart.data.notifyDataChanged()
            logchart.notifyDataSetChanged()
        } else {
            set1 = LineDataSet(value1, "Temperature")
            set1.setDrawIcons(false)
            set1.color = Color.RED
            /*set1.enableDashedLine(10f, 5f, 0f)
            set1.enableDashedHighlightLine(10f, 5f, 0f)
            set1.setCircleColor(Color.RED)
            set1.circleRadius = 3f
            set1.setDrawFilled(true)      // blue color below chart line
            */
            set1.valueTextColor = Color.WHITE
            set1.setDrawCircles(false)
            set1.setDrawCircleHole(false)
            set1.lineWidth = 2f
            set1.valueTextSize = 9f
            set1.mode = LineDataSet.Mode.CUBIC_BEZIER
            set1.cubicIntensity = 0.2f
            set1.formLineWidth = 1f
            set1.formLineDashEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
            set1.formSize = 10f
            /*if (Utils.getSDKInt() >= 18) {
                val drawable: Drawable? = ContextCompat.getDrawable(this, R.drawable.fade_blue)
                set1.fillDrawable = drawable
            } else {
                set1.fillColor = Color.DKGRAY
            }*/

            set2 = LineDataSet(value2, "Humidity")
            set2.setDrawIcons(false)
            set2.color = Color.GREEN
            /*set2.enableDashedLine(10f, 5f, 0f)
            set2.enableDashedHighlightLine(10f, 5f, 0f)
            set2.setCircleColor(Color.BLUE)
            set2.circleRadius = 3f
            set2.setDrawFilled(true)          // blue color below chart line
            */
            set2.valueTextColor = Color.WHITE
            set2.lineWidth = 2f
            set2.setDrawCircles(false)
            set2.setDrawCircleHole(false)
            set2.valueTextSize = 9f
            set1.mode = LineDataSet.Mode.CUBIC_BEZIER
            set1.cubicIntensity = 0.2f
            set2.formLineWidth = 1f
            set2.formLineDashEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
            set2.formSize = 15f

            val dataSets = LineData(set1, set2)
            val xAxis: XAxis = logchart.xAxis
            xAxis.valueFormatter = object : ValueFormatter(){
                override fun getFormattedValue(value: Float): String? {
                    var t = numMap[value.toInt()]
                    return t
                }
            }
            xAxis.granularity = 1f
            xAxis.labelRotationAngle = 45f
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            logchart.data = dataSets
        }
    }
}