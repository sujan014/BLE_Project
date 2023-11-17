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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.dialog_setting.view.dgCancel
import kotlinx.android.synthetic.main.dialog_setting.view.hourPicker
import kotlinx.android.synthetic.main.dialog_setting.view.minutePicker
import kotlinx.android.synthetic.main.dialog_setting.view.setMinute
import java.lang.ClassCastException

class SettingDialogFragment: DialogFragment() {
    val min_hour = arrayOf(1,60)
    var timefactor = 1
    private var listener: OnFragmentInteractionListener?= null
    interface OnFragmentInteractionListener{
        fun onPicked(value: Int)
    }

    override fun onAttach(context: Context){
        super.onAttach(context)
        try{
            listener = context as OnFragmentInteractionListener
        } catch (e: ClassCastException){
            throw ClassCastException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var rootview: View = inflater.inflate(R.layout.dialog_setting, container, false)
        var sets = arrayOf("Minute", "Hour")
        rootview.hourPicker.apply{
            maxValue = 1
            minValue = 0
            value = 0
            displayedValues = sets
            setOnValueChangedListener{ _, _, newVal ->
                timefactor = min_hour[newVal]
                if (timefactor == 1){
                    rootview.minutePicker.apply {
                        maxValue = 60
                        minValue = 1
                    }
                } else {
                    rootview.minutePicker.apply {
                        maxValue = 24
                        minValue = 1
                    }
                }
            }
        }
        rootview.minutePicker.apply {
            maxValue = 60
            minValue = 1
        }
        rootview.dgCancel.setOnClickListener {
            dismiss()
        }
        rootview.setMinute.setOnClickListener {
            val min = rootview.minutePicker.value.toString().toInt() * timefactor
            listener?.onPicked(min)
            dismiss()
        }
        return rootview
    }
}