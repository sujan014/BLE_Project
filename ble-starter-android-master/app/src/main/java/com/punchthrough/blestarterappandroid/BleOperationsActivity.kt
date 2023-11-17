/*
 * Copyright 2019 Punch Through Design LLC
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

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.punchthrough.blestarterappandroid.ble.ConnectionEventListener
import com.punchthrough.blestarterappandroid.ble.ConnectionManager
import com.punchthrough.blestarterappandroid.ble.isIndicatable
import com.punchthrough.blestarterappandroid.ble.isNotifiable
import com.punchthrough.blestarterappandroid.ble.isReadable
import com.punchthrough.blestarterappandroid.ble.isWritable
import com.punchthrough.blestarterappandroid.ble.isWritableWithoutResponse
import com.punchthrough.blestarterappandroid.ble.toHexString
//import kotlinx.android.synthetic.main.activity_ble_operations.bottomNavigationView
import kotlinx.android.synthetic.main.activity_ble_operations.btnInit
import kotlinx.android.synthetic.main.activity_ble_operations.btnNewPdf
import kotlinx.android.synthetic.main.activity_ble_operations.btnPacket

import kotlinx.android.synthetic.main.activity_ble_operations.btnSetting
import kotlinx.android.synthetic.main.activity_ble_operations.btnStart
import kotlinx.android.synthetic.main.activity_ble_operations.btnStop

import kotlinx.android.synthetic.main.activity_ble_operations.btnViewData
import kotlinx.android.synthetic.main.activity_ble_operations.btnsaveData
import kotlinx.android.synthetic.main.activity_ble_operations.fragFrame
import kotlinx.android.synthetic.main.activity_ble_operations.pdfProgressbar
import kotlinx.android.synthetic.main.activity_ble_operations.progressbar2
import kotlinx.android.synthetic.main.activity_ble_operations.tvProgValue
import kotlinx.android.synthetic.main.activity_ble_operations.tvStatus
import kotlinx.android.synthetic.main.activity_ble_operations.tv_avghumid
import kotlinx.android.synthetic.main.activity_ble_operations.tv_avgtemp
import kotlinx.android.synthetic.main.activity_ble_operations.tv_maxhumid
import kotlinx.android.synthetic.main.activity_ble_operations.tv_maxtemp
import kotlinx.android.synthetic.main.activity_ble_operations.tv_measStart
import kotlinx.android.synthetic.main.activity_ble_operations.tv_measStop
import kotlinx.android.synthetic.main.activity_ble_operations.tv_minhumid
import kotlinx.android.synthetic.main.activity_ble_operations.tv_mintemp
//import kotlinx.android.synthetic.main.activity_ble_operations.characteristics_recycler_view
//import kotlinx.android.synthetic.main.activity_ble_operations.btnTest
//import kotlinx.android.synthetic.main.activity_ble_operations.log_scroll_view
//import kotlinx.android.synthetic.main.activity_ble_operations.log_text_view
//import kotlinx.android.synthetic.main.activity_ble_operations.mtu_field
//import kotlinx.android.synthetic.main.activity_ble_operations.request_mtu_button
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.selector
import org.jetbrains.anko.yesButton
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID

private const val WRITE_REQUEST_CODE: Int = 43

class BleOperationsActivity : AppCompatActivity(), SettingDialogFragment.OnFragmentInteractionListener{
    var chartfragment = ChartFragment()
    var logfragment = LogFragment()
    var chartlist: MutableList<Datalog> = mutableListOf()
    var framelist: MutableList<DataFrame> = mutableListOf()
    var dataSaved: Boolean = false

    var saveHandle = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        resultData: ActivityResult ->
        val uri = resultData.data?.data
        if (resultData.resultCode == Activity.RESULT_OK){
            if (uri != null){
                WritetoFile(uri)
                dataSaved = true
            }
        }
    }
    val activityForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            var intent = result.data
            //framelist = intent?.getParcelableArrayListExtra<DataFrame>("EXTRA_LIST") as MutableList<DataFrame>*/
            var bundle = intent?.extras
            dataSaved = bundle?.getBoolean("SAVED_STATE") ?: false
            framelist = bundle?.getParcelableArrayList<DataFrame>("DATA_LIST") ?: mutableListOf<DataFrame>()
            Log.d("BLEActivity", "New list obtained")
            set_statistics()
            showChartFragment()
        }
    }

    var newpdfhandle = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        resultData: ActivityResult ->
        val uri = resultData.data?.data
        if (resultData.resultCode == Activity.RESULT_OK){
            if (uri != null){
                var bitmap = getBitmapFromView(fragFrame)
                if (bitmap != null) {
                    //saveBitmap(bitmap)
                    var startdate = framelist[0].datetime
                    var enddate = framelist[framelist.size-1].datetime
                    val currdatetime = LocalDateTime.now()
                    val year = currdatetime.year
                    val mon = currdatetime.monthValue
                    val day = currdatetime.dayOfMonth
                    val hr = currdatetime.hour
                    val min = currdatetime.minute
                    val sec = currdatetime.second
                    val curformat = "${year.toString()}/${mon.toString()}/${day.toString()} ${hr.toString()}:${min.toString()}:${sec.toString()}"
                    var testinfo = TestInfo(device.name.toString(), startdate, enddate, curformat)
                    val myRunnable = Runnable {
                        Handler(Looper.getMainLooper()).post {
                            pdfProgressbar.visibility = View.VISIBLE
                        }
                        CreateNewPdf.newPdf(this@BleOperationsActivity, uri, framelist, testinfo, bitmap)
                        Handler(Looper.getMainLooper()).post {
                            pdfProgressbar.visibility = View.GONE
                        }
                    }
                    val thread = Thread(myRunnable).start()
                    /*Thread {
                        // Run whatever background code you want here.
                        Log.d("PdfThread", "Writing PDF")
                        Handler(Looper.getMainLooper()).post {

                        }
                        Log.d("PdfThread", "Exiting PDF")
                    }.start()*/
                }
            }
        }
    }
    private fun getBitmapFromView(view: View): Bitmap{
        var bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        var canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }
    fun saveBitmap(bmp: Bitmap)
    {
        val dest = this.filesDir.toString() + "/capture.bmp"
        Log.d("saveBitmap", "$dest")
        try{
            var out: FileOutputStream = FileOutputStream(dest)
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
            Toast.makeText(this, "capture saved to $dest", Toast.LENGTH_SHORT).show()
        } catch (e: java.lang.Exception){
            Toast.makeText(this, "capture save error", Toast.LENGTH_SHORT).show()
        }
    }
    private lateinit var device: BluetoothDevice
    private val dateFormatter = SimpleDateFormat("MMM d, HH:mm:ss", Locale.US)
    private val characteristics by lazy {
        ConnectionManager.servicesOnDevice(device)?.flatMap { service ->
            service.characteristics ?: listOf()
        } ?: listOf()
    }
    private val characteristicProperties by lazy {
        characteristics.map { characteristic ->
            characteristic to mutableListOf<CharacteristicProperty>().apply {
                if (characteristic.isNotifiable()) add(CharacteristicProperty.Notifiable)
                if (characteristic.isIndicatable()) add(CharacteristicProperty.Indicatable)
                if (characteristic.isReadable()) add(CharacteristicProperty.Readable)
                if (characteristic.isWritable()) add(CharacteristicProperty.Writable)
                if (characteristic.isWritableWithoutResponse()) {
                    add(CharacteristicProperty.WritableWithoutResponse)
                }
            }.toList()
        }.toMap()
    }
    private val characteristicAdapter: CharacteristicAdapter by lazy {
        CharacteristicAdapter(characteristics) { characteristic ->
            showCharacteristicOptions(characteristic)
        }
    }
    private var notifyingCharacteristics = mutableListOf<UUID>()
    var debugText: String = ""

    var measTime:Int = 1

    var startTime: StampTime = StampTime(0, 0, 0, 0 ,0 ,0)
    var stopTime: StampTime = StampTime(0, 0, 0, 0 ,0 ,0)
    val dayofmonth: IntArray = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

    private fun logServices(){
        var groupMatch = false
        var list = ConnectionManager.servicesOnDevice(device)
        list?.forEach { services ->
            Log.d("Gattproperty"," ${services.uuid}")
            services.characteristics.forEach {
                Log.d("Gattproperty"," \t\t ${it.uuid}")
                if (it.isNotifiable()) {
                    ConnectionManager.enableNotifications(device, it)
                    groupMatch = true
                    Log.d("Gattproperty"," \t\t ${it.uuid} opened")
                }
                if ((it.isWritable() || it.isWritableWithoutResponse()) && groupMatch) {
                    writecharacteristics = it
                    Log.d("Gattproperty"," \t\t ${it.uuid} writable found")
                }
            }
        }
        Log.d("Devicename", "${device.name}")
    }
    private fun showProgress(set: Boolean) {
        runOnUiThread {
            when (set) {
                true -> {
                    progressbar2.visibility = View.VISIBLE
                    tvProgValue.visibility = View.VISIBLE
                }
                false -> {
                    if (progressbar2 != null)
                        progressbar2.visibility = View.GONE
                    if (tvProgValue != null)
                        tvProgValue.visibility = View.GONE
                }
            }
        }
    }
    private fun setProgressValue(value: Int){
        runOnUiThread() {
            progressbar2.progress = value
            tvProgValue.text = "$value%"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        ConnectionManager.registerListener(connectionEventListener)
        super.onCreate(savedInstanceState)
        supportActionBar!!.setBackgroundDrawable(resources.getDrawable(R.drawable.appbar_gradient, null))

        Log.d("LifecycleView", "on create")

        showProgress(false)
        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            ?: error("Missing BluetoothDevice from MainActivity!")

        setContentView(R.layout.activity_ble_operations)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
            title = getString(R.string.ble_playground)
        }
        logServices()
        //setupRecyclerView()
        /*request_mtu_button.setOnClickListener {
            if (mtu_field.text.isNotEmpty() && mtu_field.text.isNotBlank()) {
                mtu_field.text.toString().toIntOrNull()?.let { mtu ->
                    log("Requesting for MTU value of $mtu")
                    ConnectionManager.requestMtu(device, mtu)
                } ?: log("Invalid MTU value: ${mtu_field.text}")
            } else {
                log("Please specify a numeric value for desired ATT MTU (23-517)")
            }
            hideKeyboard()
        }*/

        /*btnTest.setOnClickListener {
            log_text_view.text = ""
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                // do something after 2000ms
                log_text_view.text = debugText
            }, 2000)
        }*/
        showChartFragment()
        /*bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.miMain -> {
                    showChartFragment()
                }
                R.id.miLog -> {
                    showLogFragment()
                }
            }
            true
        }*/
        btnsaveData.setOnClickListener {
            createFile()
        }
        btnViewData.setOnClickListener{
            var intent = Intent(this, DataActivity::class.java)
            var bundle: Bundle = Bundle()
            bundle.putBoolean("SAVED_STATE", dataSaved)
            bundle.putParcelableArrayList("DATA_LIST", ArrayList(framelist))
            intent.putExtras(bundle)
            //intent.putParcelableArrayListExtra("DATA_LIST", ArrayList(framelist))
            //startActivity(intent)
            activityForResult.launch(intent)
        }
        btnNewPdf.setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = "text/.pdf"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            newpdfhandle.launch(intent)
        }
        btnInit.setOnClickListener {
            setProgressValue(0)
            WriteChannel(1)
            dataSaved = false }
        btnStart.setOnClickListener {
            setProgressValue(0)
            WriteChannel(3)
            dataSaved = false }
        btnStop.setOnClickListener {
            setProgressValue(0)
            WriteChannel(4)
            dataSaved = false }
        btnPacket.setOnClickListener {
            setProgressValue(0)
            debugText = ""
            WriteChannel(5)
            dataSaved = false
        }
        btnSetting.setOnClickListener {
            setProgressValue(0)
            //settingOperation()
            settingDialog()
            dataSaved = false
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("LifecycleView", "on start")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("LifecycleView", "on restart")
    }
    override fun onResume() {
        super.onResume()
        Log.d("LifecycleView", "on resume")
    }
    override fun onPause() {
        super.onPause()
        Log.d("LifecycleView", "on pause")
    }

    override fun onStop() {
        super.onStop()
        Log.d("LifecycleView", "on stop")
    }

    private fun showChartFragment(){
        supportFragmentManager.beginTransaction().apply {
            chartfragment = ChartFragment()
            var bundle = Bundle()
            bundle.putInt("MEAS_EXTRA", measTime)
            bundle.putParcelable("TIME_EXTRA", startTime)
            bundle.putParcelableArrayList("ARRAY", ArrayList(framelist))    //bundle.putParcelableArrayList("ARRAY", ArrayList(chartlist))
            chartfragment.arguments = bundle
            replace(R.id.fragFrame, chartfragment)
            commit()
        }
    }

    private fun showLogFragment()
    {
        supportFragmentManager.beginTransaction().apply {
            var bundle = Bundle()
            bundle.putString("LOG_EXTRA", debugText)
            logfragment.arguments = bundle
            replace(R.id.fragFrame, logfragment)
            commit()
        }
    }
    fun settingDialog()
    {
        var pickerdialog = SettingDialogFragment()
        pickerdialog.show(supportFragmentManager, "Select measurement time")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPicked(time: Int) {
        Log.d("AlertDialog", "Picked : $time")
        val lower = time.toUByte()
        val upper = (time shr 8).toUByte()
        measTime = time
        Log.d("AlertDialog", "$time = $upper $lower SELECTED")
        settingPacket[2] = upper.toByte()
        settingPacket[3] = lower.toByte()
        val tstring = settingPacket.toHexString()
        Log.d("AlertDialog", "{$tstring}")
        WriteChannel(2)
    }

    /*
    @RequiresApi(Build.VERSION_CODES.O)
    fun settingOperation(){
        val pDialogView = LayoutInflater.from(this).inflate(R.layout.value_picker, null)
        pDialogView.apply{
            numberPicker.also{
                it.minValue = 1
                it.maxValue = 1440
            }
        }
        val pBuilder = AlertDialog.Builder(this)
            .setView(pDialogView)
            .setTitle("Measurement cycle")
            .setPositiveButton("OK"){ _, _ ->
                val selected = pDialogView.numberPicker.value
                val lower = selected.toUByte()
                val upper = (selected shr 8).toUByte()
                measTime = selected
                Log.d("AlertDialog", "$selected = $upper $lower SELECTED")
                settingPacket[2] = upper.toByte()
                settingPacket[3] = lower.toByte()
                val tstring = settingPacket.toHexString()
                Log.d("AlertDialog", "{$tstring}")
                WriteChannel(2)
            }
            .setNegativeButton("CANCEL"){ _, _ ->
            }.create()
        pBuilder.setCanceledOnTouchOutside(false)
        pBuilder.show()
        pBuilder.setOnDismissListener{
            Log.d("AlertDialog", "Dialog closed")
        }
    }*/

    fun createFile()
    {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "text/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        saveHandle.launch(intent)
    }

    override fun onDestroy() {
        ConnectionManager.unregisterListener(connectionEventListener)
        ConnectionManager.teardownConnection(device)
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /*private fun setupRecyclerView() {
        characteristics_recycler_view.apply {
            adapter = characteristicAdapter
            layoutManager = LinearLayoutManager(
                this@BleOperationsActivity,
                RecyclerView.VERTICAL,
                false
            )
            isNestedScrollingEnabled = false
        }

        val animator = characteristics_recycler_view.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }*/

    @SuppressLint("SetTextI18n")
    private fun log(message: String) {
        debugText = debugText + "\n" + message
        /*runOnUiThread {
            val currentLogText = if (log_text_view.text.isEmpty()) {
                "Beginning of log."
            } else {
                log_text_view.text
            }
            //log_text_view.text = "$currentLogText\n$formattedMessage"
            log_text_view.text = "$currentLogText\n$message"
            log_scroll_view.post { log_scroll_view.fullScroll(View.FOCUS_DOWN) }
            debugText = log_text_view.text.toString()
        }*/
    }

    lateinit var writecharacteristics: BluetoothGattCharacteristic

    private fun showCharacteristicOptions(characteristic: BluetoothGattCharacteristic)
    {
        Log.d("Debug","${characteristicProperties[characteristic]}; $characteristic")
        writecharacteristics = characteristic
        characteristicProperties[characteristic]?.let { properties ->
            selector("Select an action to perform", properties.map { it.action }) { _, i ->
                Log.d("Debug","${properties[i]}")
                when (properties[i]) {
                    CharacteristicProperty.Readable -> {
                        log("Reading from ${characteristic.uuid}")
                        ConnectionManager.readCharacteristic(device, characteristic)
                    }
                    CharacteristicProperty.Writable, CharacteristicProperty.WritableWithoutResponse -> {
                        //showWritePayloadDialog(characteristic)
                    }
                    CharacteristicProperty.Notifiable, CharacteristicProperty.Indicatable -> {
                        if (notifyingCharacteristics.contains(characteristic.uuid)) {
                            log("Disabling notifications on ${characteristic.uuid}")
                            ConnectionManager.disableNotifications(device, characteristic)
                        } else {
                            log("Enabling notifications on ${characteristic.uuid}")
                            ConnectionManager.enableNotifications(device, characteristic)
                        }
                    }
                }
            }
        }
    }

    fun ReadChannel(){

    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun WriteChannel(selectVal: Int){

        opCode = selectVal
        val arraytosend = when (selectVal){
            1 -> initPacket
            2 -> settingPacket
            3 -> startPacket
            4 -> stopPacket
            5 -> readPacket
            else -> byteArrayOf(selectVal.toByte())
        }
        newpacketRead = true
        data_state = data_header
        index = 0
        list.clear()
        framelist.clear()

        if (arraytosend.size > 1) {
            if (selectVal == 3 || selectVal == 4) {
                val currdatetime = LocalDateTime.now()
                val year = currdatetime.year - 2000
                val mon = currdatetime.monthValue
                val day = currdatetime.dayOfMonth
                val hr = currdatetime.hour
                val min = currdatetime.minute
                val sec = currdatetime.second
                arraytosend[2] = year.toByte()
                arraytosend[3] = mon.toByte()
                arraytosend[4] = day.toByte()
                arraytosend[5] = hr.toByte()
                arraytosend[6] = min.toByte()
                arraytosend[7] = sec.toByte()
            }
            //log("Writing to ${characteristic.uuid}: ${bytes.toHexString()}")
            log("TX ::\n${arraytosend.toHexString()}")
            /*when(selectVal){
                1 -> writeStatusMsg("Status: Init packet sent", "#ffd6d6","#0f94ff")
                2 -> writeStatusMsg("Status: Setting packet sent", "#ffd6d6","#0f94ff")
                3 -> writeStatusMsg("Status: Start request", "#ffd6d6","#0f94ff")
                4 -> writeStatusMsg("Status: Stop request", "#ffd6d6","#0f94ff")
                5 -> writeStatusMsg("Status: Data request", "#ffd6d6","#0f94ff")
            }*/
            ConnectionManager.writeCharacteristic(
                device,
                writecharacteristics,
                arraytosend
            )
        }
    }
    private fun writeStatusMsg(message: String, backcolor: String, textcolor: String){
        runOnUiThread {
            Toast.makeText(this@BleOperationsActivity, message, Toast.LENGTH_SHORT).show()
            /*tvStatus.text = message
            tvStatus.setBackgroundColor(Color.parseColor(backcolor))
            tvStatus.setTextColor(Color.parseColor(textcolor))*/
        }
    }

    var initPacket = byteArrayOf(0x3E, 0x30, 0x30, 0x30, 0x0D, 0x0A)
    var settingPacket = byteArrayOf(0x3E, 0x31, 0x00, 0x01, 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x30, 0x30, 0x0D, 0x0A)
    var startPacket = byteArrayOf(0x3E, 0x32, 0x21, 0x05, 0x03, 0x04, 0x13, 0x05, 0x30, 0x30, 0x0D, 0x0A)
    var stopPacket = byteArrayOf(0x3E, 0x33, 0x21, 0x05, 0x03, 0x04, 0x13, 0x05, 0x30, 0x30, 0x0D, 0x0A)
    var readPacket = byteArrayOf(0x3E, 0x34, 0x30, 0x30, 0x0D, 0x0A)

    var opCode = 0
    var newpacketRead: Boolean = false

    var data_header = 1
    var data_packet = 2
    var data_state = data_header

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("InflateParams")
    private fun showWritePayloadDialog(characteristic: BluetoothGattCharacteristic) {
        val hexField = layoutInflater.inflate(R.layout.edittext_hex_payload, null) as EditText

        alert {
            customView = hexField
            isCancelable = false
            yesButton {
                with(hexField.text.toString()) {
                    if (isNotBlank() && isNotEmpty()) {
                        //val bytes = hexToBytes()
                        val bytes = toInt()
                        opCode = bytes
                        val arraytosend = when (bytes){
                            1 -> initPacket
                            2 -> settingPacket
                            3 -> startPacket
                            4 -> stopPacket
                            5 -> readPacket
                            else -> byteArrayOf(bytes.toByte())
                        }
                        newpacketRead = true
                        data_state = data_header
                        index = 0
                        list.clear()

                        if (arraytosend.size > 1) {
                            if (bytes == 3 || bytes == 4) {
                                val currdatetime = LocalDateTime.now()
                                val year = currdatetime.year - 2000
                                val mon = currdatetime.monthValue
                                val day = currdatetime.dayOfMonth
                                val hr = currdatetime.hour
                                val min = currdatetime.minute
                                val sec = currdatetime.second
                                arraytosend[2] = year.toByte()
                                arraytosend[3] = mon.toByte()
                                arraytosend[4] = day.toByte()
                                arraytosend[5] = hr.toByte()
                                arraytosend[6] = min.toByte()
                                arraytosend[7] = sec.toByte()
                            }
                            //log("Writing to ${characteristic.uuid}: ${bytes.toHexString()}")
                            log("TX ::\n${arraytosend.toHexString()}")
                            ConnectionManager.writeCharacteristic(
                                device,
                                characteristic,
                                arraytosend
                            )
                        }
                    } else {
                        log("Please enter a hex payload to write to ${characteristic.uuid}")
                    }
                }
            }
            noButton {}
        }.show()
        hexField.showKeyboard()
    }

    var holdArray : ByteArray? = null
    fun ProcessPacket(rxbyte: ByteArray)
    {
        if (opCode != 5) {
            log("Notification RX ::\n${rxbyte.toHexString()}")
            if (opCode == 1){   checkPacket(opCode, rxbyte)/*writeStatusMsg("Status: Init packet sent")*/}
            if (opCode == 2){   writeStatusMsg("Status: Setting successful", "#0f94ff", "#ffffff")}
            if (opCode == 3){   writeStatusMsg("Status: Start successful", "#0f94ff", "#ffffff")}
            if (opCode == 4){   writeStatusMsg("Status: Stop successful", "#0f94ff", "#ffffff")}
            timer(2000, 1000).start()
        } else {
            if (data_state == data_header){
                //Log.d("Packet","header: ${rxbyte.toHexString()}")
                log("Notification RX ::\n${rxbyte.toHexString()}")
                //writeStatusMsg("Data header rx", "#ffd6d6", "#0f94ff")
                formattedData = ""
                data_state = data_packet
                newpacketRead = true

                startTime.year = 2000 + rxbyte[2].toInt()
                startTime.month = rxbyte[3].toInt()
                startTime.day = rxbyte[4].toInt()
                startTime.hour = rxbyte[5].toInt()
                startTime.min = rxbyte[6].toInt()
                startTime.sec = rxbyte[7].toInt()
                stopTime.year = 2000 + rxbyte[8].toInt()
                stopTime.month = rxbyte[9].toInt()
                stopTime.day = rxbyte[10].toInt()
                stopTime.hour = rxbyte[11].toInt()
                stopTime.min = rxbyte[12].toInt()
                stopTime.sec = rxbyte[13].toInt()

                Log.d("TimeStamp", "Start-> ${startTime.year}-${startTime.month}-${startTime.day}  ${startTime.hour}/${startTime.min}/${startTime.sec}")
                Log.d("TimeStamp", "Stop -> ${stopTime.year}-${stopTime.month}-${stopTime.day}  ${stopTime.hour}/${stopTime.min}/${stopTime.sec}")
            } else {
                if (newpacketRead == true) {
                    holdArray = rxbyte
                    //Log.d("Packet", "New")
                    //log("Notification RX ::\n${rxbyte.toHexString()}")
                    if (holdArray!!.size == 254) {
                        newpacketRead = true
                        log("Notification RX ::\n${holdArray?.toHexString()}")
                        log(TabulateData(holdArray!!))
                    } else {
                        newpacketRead = false
                    }
                } else {
                    holdArray = holdArray?.plus(rxbyte)
                    Log.d("Packet", "frag")
                    if (holdArray!!.size == 254) {
                        newpacketRead = true
                        log("Notification RX ::\n${holdArray?.toHexString()}")
                        log("\n" + TabulateData(holdArray!!))
                    }
                }
            }
        }
    }

    private fun checkPacket(opcode: Int, rxbyte: ByteArray){
        if (opcode == 1){
            val byte1 = rxbyte[1].toUByte()
            val byte2 = rxbyte[2].toUByte()
            if (byte1 == 0x30.toUByte() && byte2 == 0x31.toUByte()){
                writeStatusMsg("Status: Init started", "#ffd6d6", "#00f511")
                //progbar_continue(true)
            } else if (byte1 == 0x30.toUByte() && byte2 == 0x32.toUByte()){
                writeStatusMsg("Status: Init completed", "#0f94ff", "#ffffff")
                //progbar_continue(false)
            }
        }
    }

    fun progbar_continue(state: Boolean){
        runOnUiThread {
            if (state == true) {
            } else {
            }
        }
    }

    var formattedData: String = ""
    var list = mutableListOf<Datalog>()
    var index = 0
    private fun TabulateData(Array: ByteArray): String{
        var pktcount = 0
        var temp = 0
        var tempe = 0f
        var humid = 0f
        var i = 4
        var ut1: UByte = 0u
        var ut2: UByte = 0u
        var hum: UByte = 0u
        var dispstr:String = ""

        while(pktcount < 62){
            ut1 = Array[i].toUByte()
            ut2 = Array[i + 1].toUByte()
            hum = Array[i + 2].toUByte()
            if (!(ut1 == 255.toUByte() && ut2 == 255.toUByte()  && hum == 255.toUByte())) {
                temp = (( ut1.toUInt() shl 8 ) + ut2.toUInt()).toInt()
                tempe = temp.toFloat()/10
                humid = hum.toFloat()
                list.add(Datalog(++index, tempe, humid))
                dispstr = dispstr + (index).toString() + ": " + tempe + "C, " + humid + "%\n"
            }
            i += 4
            pktcount++
        }

        showProgress(true)
        setProgressValue(Array[3].toInt()*100/Array[2].toInt())
        if (Array[2] == Array[3]) {
            chartlist = list
            populateFrameList()
            set_statistics()
            Log.d("ListDebug", "length-${chartlist.size}")
            writeStatusMsg("Data packet completed", "#0f94ff", "#ffffff")
            showChartFragment()
        }
        return dispstr
    }

    fun populateFrameList(){
        var ttime = startTime
        ttime.min -= measTime
        var datetime: String = ""
        framelist.clear()
        for (i in 0 until list.size){
            ttime.min += measTime
            if (ttime.min >= 60) {
                ttime.min %= 60
                ttime.hour++;
                if (ttime.hour >= 24) {
                    ttime.hour = 0;
                    ttime.day++
                    if (ttime.day > dayofmonth[startTime.month - 1]) {
                        ttime.day = 1;
                        ttime.month++
                        if (ttime.month > 12) {
                            ttime.month = 1
                            ttime.year++
                        }
                    }
                }
            }
            datetime = ttime.year.toString() + "/" + ttime.month.toString() + "/" + ttime.day.toString() + "  " + ttime.hour.toString() + ":" + ttime.min.toString()
            framelist.add( DataFrame( list[i].count, datetime, list[i].temperature, list[i].humidity ) )
        }
    }
    private fun timer(millisInFuture:Long,countDownInterval:Long): CountDownTimer {
        return object: CountDownTimer(millisInFuture,countDownInterval){
            override fun onTick(millisUntilFinished: Long){
                val timeRemaining = "${millisUntilFinished/1000}"
                Log.d("Timers","tick: $timeRemaining")
            }

            override fun onFinish() {
                Log.d("Timers","tick: End")
                writeStatusMsg("", "#0f94ff", "#ffffff")
            }
        }
    }

    fun roundTo(valueToRound: Float): Float{
        var floatf = 0f
        floatf = ((valueToRound*10f).toInt()).toFloat()/10f
        return floatf
    }
    private fun set_statistics()
    {
        if (framelist.isNullOrEmpty()) {
            Toast.makeText(this@BleOperationsActivity, "packet is empty", Toast.LENGTH_SHORT).show()
            tv_mintemp.text = "Min: "
            tv_maxtemp.text = "Max: "
            tv_avgtemp.text = "Avg: "
            tv_minhumid.text = "Min: "
            tv_maxhumid.text = "Max: "
            tv_avghumid.text = "Avg: "
            tv_measStart.text = "Start: "
            tv_measStop.text = "Stop: "
        } else{
            try {
                val mintemp = framelist.minBy { it.temperature }?.temperature
                val maxtemp = framelist.maxBy { it.temperature }?.temperature
                val avgtemp = (framelist.map { it.temperature }.average() * 10f).toInt().toFloat() / 10f
                val minhumid = framelist.minBy { it.humidity }?.humidity
                val maxhumid = framelist.maxBy { it.humidity }?.humidity
                val avghumid = (framelist.map { it.humidity }.average() * 10f).toInt().toFloat() / 10f
                Log.d("WHADDA", "$mintemp")
                Log.d("WHADDA", "$maxtemp")
                Log.d("WHADDA", "$avgtemp")
                tv_mintemp.text = "Min: $mintemp C"
                tv_maxtemp.text = "Max: $maxtemp C"
                tv_avgtemp.text = "Avg: $avgtemp C"
                tv_minhumid.text = "Min: $minhumid %"
                tv_maxhumid.text = "Max: $maxhumid %"
                tv_avghumid.text = "Avg: $avghumid %"
                tv_measStart.text = "Start: ${framelist[0].datetime.toString()}"
                tv_measStop.text = "Stop: ${framelist[framelist.size-1].datetime.toString()}"
            } catch (e: Exception){
                Log.d("ExcpetionHandle", "${e}")
            }
        }
    }
    /*fun WritetoFile(uri: Uri)
    {
        var writeStream = contentResolver.openFileDescriptor(uri,"w")
        val filename = queryName(contentResolver, uri)
        var fileOutputStream = FileOutputStream(writeStream?.fileDescriptor)
        fileOutputStream.write(formattedData.toByteArray())
        fileOutputStream.close()
        Toast.makeText(this, "data save to $filename", Toast.LENGTH_SHORT).show()
        Log.i("Log", "$formattedData")
    }*/
    fun WritetoFile(uri: Uri)
    {
        var writeStream = contentResolver.openFileDescriptor(uri,"w")
        val filename = queryName(contentResolver, uri)
        var fileOutputStream = FileOutputStream(writeStream?.fileDescriptor)
        formattedData = ""
        framelist.forEach {
            formattedData += "${it.count}: ${it.datetime}, ${it.temperature}C, ${it.humidity}%\n"
        }
        fileOutputStream.write(formattedData.toByteArray())
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

    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {
            onDisconnect = {
                runOnUiThread {
                    alert {
                        title = "Disconnected"
                        message = "Disconnected from device."
                        positiveButton("OK") { onBackPressed() }
                    }.show()
                }
            }

            onCharacteristicRead = { _, characteristic ->
                log("Read from ${characteristic.uuid}: ${characteristic.value.toHexString()}\n ${String(characteristic.value)}")
            }

            onCharacteristicWrite = { _, characteristic ->
                //log("Wrote to ${characteristic.uuid}")
            }

            onMtuChanged = { _, mtu ->
                log("MTU updated to $mtu")
            }

            onCharacteristicChanged = { _, characteristic ->
                //log("Value changed on ${characteristic.uuid}: ${characteristic.value.toHexString()} \n ${String(characteristic.value)}")
                ProcessPacket(characteristic.value)
            }

            onNotificationsEnabled = { _, characteristic ->
                log("Enabled notifications on ${characteristic.uuid}")
                notifyingCharacteristics.add(characteristic.uuid)
            }

            onNotificationsDisabled = { _, characteristic ->
                log("Disabled notifications on ${characteristic.uuid}")
                notifyingCharacteristics.remove(characteristic.uuid)
            }
        }
    }

    private enum class CharacteristicProperty {
        Readable,
        Writable,
        WritableWithoutResponse,
        Notifiable,
        Indicatable;

        val action
            get() = when (this) {
                Readable -> "Read"
                Writable -> "Write"
                WritableWithoutResponse -> "Write Without Response"
                Notifiable -> "Toggle Notifications"
                Indicatable -> "Toggle Indications"
            }
    }

    private fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }

    private fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun EditText.showKeyboard() {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        requestFocus()
        inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun String.hexToBytes() =
        this.chunked(2).map { it.toUpperCase(Locale.US).toInt(16).toByte() }.toByteArray()
}
