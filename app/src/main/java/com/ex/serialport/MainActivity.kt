package com.ex.serialport

import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.EditText
import android.widget.Toast
import android_serialport_api.BAUDRATE
import android_serialport_api.SerialPortFinder
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.ex.serialport.adapter.LogListAdapter
import com.ex.serialport.adapter.SpAdapter
import com.ex.serialport.databinding.ActivityMainBinding
import com.ashlikun.serialport.SerialHelper
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

@RequiresApi(Build.VERSION_CODES.KITKAT)
class MainActivity : AppCompatActivity() {
    private val serialPortFinder by lazy {
        SerialPortFinder()
    }
    val simpleDateFormat = SimpleDateFormat("HH:mm:ss:SSS")
    val currentTime
        get() = simpleDateFormat.format(Calendar.getInstance().time)
    private val serialHelper: SerialHelper by lazy {
        SerialHelper("dev/ttyS1", 115200) { byteArray ->
            runOnUiThread {
                if (binding.radioGroup.getCheckedRadioButtonId() == R.id.radioButton1) {
                    logListAdapter.addData(currentTime + " Rx:<==" + String(byteArray, StandardCharsets.UTF_8))
                    if (logListAdapter.data != null && logListAdapter.data.size > 0) {
                        binding.recyclerView.smoothScrollToPosition(logListAdapter.data.size)
                    }
                } else {
                    logListAdapter.addData(currentTime + " Rx:<==" + ByteUtil.ByteArrToHex(byteArray))
                    if (logListAdapter.data != null && logListAdapter.data.size > 0) {
                        binding.recyclerView.smoothScrollToPosition(logListAdapter.data.size)
                    }
                }
            }
        }
    }
    private var logListAdapter = LogListAdapter(null)
    val botes = BAUDRATE.values().map { it.baudrate }
    val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    protected override fun onDestroy() {
        super.onDestroy()
        serialHelper.close()
    }

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        logListAdapter = LogListAdapter(null)
        binding.recyclerView.setLayoutManager(LinearLayoutManager(this))
        binding.recyclerView.setAdapter(logListAdapter)
        binding.recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))



        initAdapter()
        binding.btnOpen.setOnClickListener { v: View? ->
            if (serialHelper.open()) {
                binding.btnOpen.isEnabled = false
            } else {
                Toast.makeText(this@MainActivity, getString(R.string.tips_cannot_be_opened), Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnSend.setOnClickListener(View.OnClickListener {
            val sDateFormat = SimpleDateFormat("hh:mm:ss.SSS")
            if (binding.radioGroup.getCheckedRadioButtonId() == R.id.radioButton1) {
                if (binding.edInput.getText().toString().isNotEmpty()) {
                    if (serialHelper.isOpen) {
                        serialHelper.sendTxt(binding.edInput.getText().toString())
                        logListAdapter.addData(sDateFormat.format(Date()) + " Tx:==>" + binding.edInput.getText().toString())
                        if (logListAdapter.data != null && logListAdapter.data.size > 0) {
                            binding.recyclerView.smoothScrollToPosition(logListAdapter.data.size)
                        }
                    } else {
                        Toast.makeText(baseContext, R.string.tips_serial_port_not_open, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(baseContext, R.string.tips_please_enter_a_data, Toast.LENGTH_SHORT).show()
                }
            } else {
                if (binding.edInput.text.toString().isNotEmpty()) {
                    if (serialHelper.isOpen) {
                        try {
                            binding.edInput.text.toString().toLong(16)
                        } catch (e: NumberFormatException) {
                            Toast.makeText(getBaseContext(), R.string.tips_formatting_hex_error, Toast.LENGTH_SHORT).show()
                            return@OnClickListener
                        }
                        serialHelper.sendHex(binding.edInput.text.toString())
                        logListAdapter.addData(sDateFormat.format(Date()) + " Tx:==>" + binding.edInput.text.toString())
                        if (logListAdapter.data != null && logListAdapter.data.size > 0) {
                            binding.recyclerView.smoothScrollToPosition(logListAdapter.data.size)
                        }
                    } else {
                        Toast.makeText(getBaseContext(), R.string.tips_serial_port_not_open, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(getBaseContext(), R.string.tips_please_enter_a_data, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun initAdapter() {
        val ports: List<String> = serialPortFinder.allDevicesPath
        val databits = arrayOf("8", "7", "6", "5")
        val paritys = arrayOf("NONE", "ODD", "EVEN", "SPACE", "MARK")
        val stopbits = arrayOf("1", "2")
        val flowcons = arrayOf("NONE", "RTS/CTS", "XON/XOFF")
        val spAdapter = SpAdapter(this)
        spAdapter.setDatasLk(ports.toTypedArray())
        binding.spSerial.adapter = spAdapter
        binding.spSerial.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                serialHelper.close()
                serialHelper.port = ports[position]
                binding.btnOpen.isEnabled = true
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        val spAdapter2 = SpAdapter(this)
        spAdapter2.setDatasLk((botes.map { it.toString() } + listOf("CUSTOM")).toTypedArray())
        binding.spBaudrate.adapter = spAdapter2
        binding.spBaudrate.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                if (position == botes.size - 1) {
                    showInputDialog()
                    return
                }
                findViewById<View>(R.id.tv_custom_baudrate).visibility = View.GONE
                serialHelper.close()
                serialHelper.baudRate = botes[position]
                binding.btnOpen.isEnabled = true
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        val spAdapter3 = SpAdapter(this)
        spAdapter3.setDatasLk(databits)
        binding.spDatabits.adapter = spAdapter3
        binding.spDatabits.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                serialHelper.close()
                serialHelper.dataBits = databits[position].toInt()
                binding.btnOpen.isEnabled = true
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        val spAdapter4 = SpAdapter(this)
        spAdapter4.setDatasLk(paritys)
        binding.spParity.adapter = spAdapter4
        binding.spParity.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                serialHelper.close()
                serialHelper.parity = position
                binding.btnOpen.isEnabled = true
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        val spAdapter5 = SpAdapter(this)
        spAdapter5.setDatasLk(stopbits)
        binding.spStopbits.adapter = spAdapter5
        binding.spStopbits.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                serialHelper.close()
                serialHelper.stopBits = stopbits[position].toInt()
                binding.btnOpen.isEnabled = true
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        val spAdapter6 = SpAdapter(this)
        spAdapter6.setDatasLk(flowcons)
        binding.spFlowcon.adapter = spAdapter6
        binding.spFlowcon.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                serialHelper.close()
                serialHelper.flowCon = position
                binding.btnOpen.isEnabled = true
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun showInputDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.tips_please_enter_custom_baudrate)
        val inputField = EditText(this)
        val filter: InputFilter = object : InputFilter {
            override fun filter(
                source: CharSequence, start: Int, end: Int,
                dest: Spanned, dstart: Int, dend: Int
            ): CharSequence {
                for (i in start until end) {
                    if (!Character.isDigit(source[i])) {
                        return ""
                    }
                }
                return ""
            }
        }
        inputField.filters = arrayOf(filter)
        builder.setView(inputField)
        builder.setPositiveButton("OK") { dialogInterface, i ->
            val userInput: String = inputField.getText().toString().trim { it <= ' ' }
            try {
                val value = userInput.toInt()
                if (value in 0..4000000) {
                    binding.tvCustomBaudrate.visibility = View.VISIBLE
                    binding.tvCustomBaudrate.text = getString(R.string.title_custom_buardate, userInput)
                    serialHelper.close()
                    serialHelper.baudRate = value
                    binding.btnOpen.isEnabled = true
                }
            } catch (e: NumberFormatException) {
            }
        }
        builder.setNegativeButton("Cancel") { dialogInterface, i -> dialogInterface.cancel() }
        val dialog = builder.create()
        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = getMenuInflater()
        inflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_clean) {
            logListAdapter.clean()
        }
        return super.onOptionsItemSelected(item)
    }
}