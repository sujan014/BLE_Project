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

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_write_input.btnCancel
import kotlinx.android.synthetic.main.activity_write_input.btnOk
import kotlinx.android.synthetic.main.activity_write_input.etInput
import kotlinx.android.synthetic.main.activity_write_input.tvOps

class writeInputActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write_input)

        btnOk.setOnClickListener {
            var activity2result = etInput.text.toString()
            Log.d("Set Code", "input = $activity2result")
            val intent = Intent()
            intent.putExtra("PassValue", activity2result)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }

        btnCancel.setOnClickListener { finish() }
    }
}