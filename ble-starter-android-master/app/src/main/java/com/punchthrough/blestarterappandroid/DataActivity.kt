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
import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_data.btnBleData
//import kotlinx.android.synthetic.main.activity_data.btnDeleteFile
import kotlinx.android.synthetic.main.activity_data.btnOpenFile
import kotlinx.android.synthetic.main.activity_data.rvDatatable
import org.jetbrains.anko.startActivityForResult

import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader

class DataActivity : AppCompatActivity() {
    lateinit var adapter: DataAdapter
    /*lateinit var blelist: MutableList<Datalog>
    var openlist: MutableList<Datalog> = mutableListOf()
    var chartlist: MutableList<Datalog> = mutableListOf()*/

    lateinit var blelist: MutableList<DataFrame>
    var openlist: MutableList<DataFrame> = mutableListOf()
    var chartlist: MutableList<DataFrame> = mutableListOf()
    var fileError = false
    val BLEData = 1
    val FILEData = 2
    var dispData = 1
    var intnDataSaved = false
    var saveAndopen = false

    var saveHandle = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            resultData: ActivityResult ->
        val uri = resultData.data?.data
        if (resultData.resultCode == Activity.RESULT_OK){
            if (uri != null){
                WritetoFile(uri)
                intnDataSaved = true
                openFileAlert()
            }
        }
    }

    var readIntent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        resultData: ActivityResult ->
        if (resultData.resultCode == Activity.RESULT_OK){
            val uri = resultData.data?.data
            Log.d("Uri", "$uri")
            if (uri != null){
                openlist = openFileContent(uri)
                adapter.RemoveItems()
                adapter = DataAdapter(openlist)
                rvDatatable.adapter = adapter

                if (!fileError) {
                    supportFragmentManager.beginTransaction().apply {
                        var chartfragment = ChartFragment()
                        var bundle = Bundle()
                        bundle.putParcelableArrayList("ARRAY", ArrayList(openlist))
                        chartfragment.arguments = bundle
                        replace(R.id.chartFrame, chartfragment)
                        commit()
                    }
                    sendDataToBleActivity(openlist)
                    dispData = FILEData
                }
            }
        }
    }

    fun sendDataToBleActivity(arrayList: MutableList<DataFrame>)
    {
        val intent = Intent()
        var bundle = Bundle()
        bundle.putBoolean("SAVED_STATE", intnDataSaved)
        bundle.putParcelableArrayList("DATA_LIST", ArrayList(arrayList))
        intent.putExtras(bundle)
        //intent.putParcelableArrayListExtra("EXTRA_LIST", ArrayList(openlist))
        setResult(Activity.RESULT_OK, intent)
    }
    var deleteIntent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        resultData: ActivityResult ->
        if (resultData.resultCode == Activity.RESULT_OK){
            val uri = resultData.data?.data
            Log.d("Uri", "$uri")
            if (uri != null){
                DeleteFile(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data)
        supportActionBar!!.setBackgroundDrawable(resources.getDrawable(R.drawable.appbar_gradient, null))

        var bundle = intent.extras
        intnDataSaved = bundle?.getBoolean("SAVED_STATE") ?: true
        var list = bundle?.getParcelableArrayList<DataFrame>("DATA_LIST") ?: mutableListOf<DataFrame>()
        //var list: MutableList<DataFrame> = intent.getParcelableArrayListExtra<DataFrame>("DATA_LIST") as MutableList<DataFrame>
        blelist = list.toMutableList()
        Log.d("List Log","$list")
        adapter = DataAdapter(list)
        rvDatatable.adapter = adapter
        rvDatatable.layoutManager = LinearLayoutManager(this)

        btnBleData.setOnClickListener {
            list = blelist.toMutableList()
            DisplayBleData(list)
            sendDataToBleActivity(blelist)
        }
        btnOpenFile.setOnClickListener {
            if (intnDataSaved) {
                readFile()
            } else {
                saveBleAlert()
            }

        }
        //btnDeleteFile.setOnClickListener { deleteFile() }

        Log.d("FragTest","Fragment begin")
        supportFragmentManager.beginTransaction().apply {
            var chartfragment = ChartFragment()
            var bundle = Bundle()
            bundle.putParcelableArrayList("ARRAY", ArrayList(blelist))
            chartfragment.arguments = bundle
            replace(R.id.chartFrame, chartfragment)
            commit()
        }
        dispData = BLEData
    }

    private fun saveBleAlert(){
        val addDialog = AlertDialog.Builder(this).setTitle("Erase Alert!").
        setMessage("Save BLE data before opening existing file ?")
            .setPositiveButton("Yes"){ _, _ ->
                saveAndOpenExisting()
            }
            .setNegativeButton("No"){_, _ ->
                openFileAlert()
            }.create()
        addDialog.show()
    }
    private fun openFileAlert(){
        val openDialog = AlertDialog.Builder(this)
            .setMessage("View log ?")
            .setPositiveButton("Yes"){ _, _ ->
                readFile()
            }
            .setNegativeButton("No"){_, _ ->

            }.create()
        openDialog.show()
    }

    fun DisplayBleData(passlist: MutableList<DataFrame>) {
        Log.d("List Log","$passlist")
        adapter.RemoveItems()
        adapter = DataAdapter(passlist)
        rvDatatable.adapter = DataAdapter(passlist)

        supportFragmentManager.beginTransaction().apply {
            var chartfragment = ChartFragment()
            var bundle = Bundle()
            bundle.putParcelableArrayList("ARRAY", ArrayList(passlist))
            chartfragment.arguments = bundle
            replace(R.id.chartFrame, chartfragment)
            commit()
        }
    }

    fun saveAndOpenExisting(){
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "text/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        saveHandle.launch(intent)
    }
    fun readFile(){
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/*"
        }
        readIntent.launch(intent)
    }

    fun deleteFile(){
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/*"
        }
        deleteIntent.launch(intent)
    }

    fun openFileContent(uri: Uri): MutableList<DataFrame>
    {
        Log.d("Read List","Open File content")
        val inputStream = contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(inputStream))
        var currentline = reader.readLine()
        var tInt: Int = 0
        var dString: String = ""
        var tFloat: Float = 0f
        var hFloat: Float = 0f

        fileError = false
        Log.d("ReadList","$currentline")
        var blelist: MutableList<DataFrame> = mutableListOf()
        while(currentline != null){
            val linesplit = currentline.split(":"," ",",", "C","%").toTypedArray()
            //Log.d("Read", "size: ${linesplit.size}")
            //linesplit.forEach { Log.d("Read", "$it ${it::class.simpleName}") }
            try {
                /*tInt = linesplit[0].toInt()
                tFloat = linesplit[2].toFloat()
                hFloat = linesplit[5].toFloat()*/
                tInt = linesplit[0].toInt()
                dString = "${linesplit[2]}  ${linesplit[4]}:${linesplit[5]}"
                tFloat = linesplit[7].toFloat()
                hFloat = linesplit[10].toFloat()

                blelist.add( DataFrame(tInt, dString, tFloat, hFloat) )
                currentline = reader.readLine()
            } catch (e: Exception){
                Log.e("Exception", "${e.toString()}")
                Toast.makeText(this@DataActivity,"Data format error", Toast.LENGTH_SHORT).show()
                fileError = true
                break
            }
        }
        inputStream?.close()
        if (!fileError) {
            Log.d("Read List", "size: ${blelist.size}")
            for (i in 0 until blelist.size) {
                Log.d("Read List", "${blelist[i]}")
            }
        }
        else{
            blelist.clear()
            blelist.add(DataFrame(1,"", 0f, 0f))
        }
        return blelist
    }

    fun DeleteFile(uri: Uri){
        val filename = queryName(contentResolver, uri)
        DocumentsContract.deleteDocument(contentResolver, uri)
        Toast.makeText(this@DataActivity, "$filename file deleted", Toast.LENGTH_SHORT).show()
    }

    private fun WritetoFile(uri: Uri)
    {
        var datastring: String = ""
        var writeStream = contentResolver.openFileDescriptor(uri,"w")
        val filename = queryName(contentResolver, uri)
        var fileOutputStream = FileOutputStream(writeStream?.fileDescriptor)
        datastring = ""
        blelist.forEach {
            datastring += "${it.count}: ${it.datetime}, ${it.temperature}C, ${it.humidity}%\n"
        }
        fileOutputStream.write(datastring.toByteArray())
        fileOutputStream.close()
        Toast.makeText(this, "data save to $filename", Toast.LENGTH_SHORT).show()
    }

    private fun queryName(resolver: ContentResolver, uri: Uri): String {
        val returnCursor: Cursor = resolver.query(uri, null, null, null, null)!!
        val nameIndex: Int = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor.moveToFirst()
        val name: String = returnCursor.getString(nameIndex)
        returnCursor.close()
        return name
    }
}