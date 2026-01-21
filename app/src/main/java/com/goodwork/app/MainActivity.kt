package com.goodwork.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.goodwork.app.adapter.ContactAdapter
import com.goodwork.app.database.DatabaseHelper
import com.goodwork.app.manager.ContactManager
import com.goodwork.app.service.SmsSender
import com.goodwork.app.service.UnlockMonitorService
import com.goodwork.app.utils.Logger

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val CONTACT_PICKER_REQUEST_CODE = 1002
        private const val EDIT_CONTACT_REQUEST_CODE = 1003
    }

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var contactManager: ContactManager
    private lateinit var smsSender: SmsSender
    private lateinit var logger: Logger
    private lateinit var contactAdapter: ContactAdapter

    private lateinit var tvServiceStatus: TextView
    private lateinit var switchService: Switch
    private lateinit var btnTestSms: Button
    private lateinit var btnViewLogs: Button
    private lateinit var btnAddContact: Button
    private lateinit var rvContacts: RecyclerView
    private lateinit var tvSmsTemplate: TextView
    private lateinit var btnEditTemplate: Button

    private val requiredPermissions = arrayOf(
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CONTACTS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeComponents()
        setupViews()
        checkAndRequestPermissions()
    }

    private fun initializeComponents() {
        databaseHelper = DatabaseHelper(this)
        contactManager = ContactManager(this)
        smsSender = SmsSender(this, databaseHelper)
        logger = Logger(this)
        contactAdapter = ContactAdapter(
            contacts = mutableListOf(),
            onEditClick = { contact -> showEditContactDialog(contact) },
            onDeleteClick = { contact -> confirmDeleteContact(contact) }
        )
    }

    private fun setupViews() {
        tvServiceStatus = findViewById(R.id.tvServiceStatus)
        switchService = findViewById(R.id.switchService)
        btnTestSms = findViewById(R.id.btnTestSms)
        btnViewLogs = findViewById(R.id.btnViewLogs)
        btnAddContact = findViewById(R.id.btnAddContact)
        rvContacts = findViewById(R.id.rvContacts)
        tvSmsTemplate = findViewById(R.id.tvSmsTemplate)
        btnEditTemplate = findViewById(R.id.btnEditTemplate)

        // 设置RecyclerView
        rvContacts.layoutManager = LinearLayoutManager(this)
        rvContacts.adapter = contactAdapter

        // 加载联系人列表
        loadContacts()

        // 加载短信模板
        loadSmsTemplate()

        // 更新服务状态
        updateServiceStatus()

        // 设置开关监听
        switchService.setOnCheckedChangeListener { _, isChecked ->
            toggleService(isChecked)
        }

        // 测试短信按钮
        btnTestSms.setOnClickListener {
            showTestSmsDialog()
        }

        // 查看日志按钮
        btnViewLogs.setOnClickListener {
            showLogsDialog()
        }

        // 添加联系人按钮
        btnAddContact.setOnClickListener {
            showAddContactDialog()
        }

        // 编辑模板按钮
        btnEditTemplate.setOnClickListener {
            showEditTemplateDialog()
        }
    }

    private fun loadContacts() {
        val contacts = contactManager.getAllContacts()
        contactAdapter.updateContacts(contacts)
        
        if (contacts.isEmpty()) {
            Toast.makeText(this, R.string.no_contacts, Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSmsTemplate() {
        val template = databaseHelper.getSetting(DatabaseHelper.KEY_SMS_TEMPLATE)
            ?: getString(R.string.default_sms_template)
        tvSmsTemplate.text = template
    }

    private fun updateServiceStatus() {
        val isEnabled = databaseHelper.getBooleanSetting(DatabaseHelper.KEY_SERVICE_ENABLED)
        switchService.isChecked = isEnabled
        
        if (isEnabled) {
            tvServiceStatus.text = getString(R.string.service_enabled)
            tvServiceStatus.setTextColor(getColor(R.color.primary))
        } else {
            tvServiceStatus.text = getString(R.string.service_disabled)
            tvServiceStatus.setTextColor(getColor(R.color.on_secondary))
        }
    }

    private fun toggleService(enable: Boolean) {
        if (enable) {
            // 启用服务
            if (hasAllPermissions()) {
                databaseHelper.setBooleanSetting(DatabaseHelper.KEY_SERVICE_ENABLED, true)
                startUnlockMonitorService()
                Toast.makeText(this, R.string.service_enabled, Toast.LENGTH_SHORT).show()
                logger.logServiceStarted()
            } else {
                switchService.isChecked = false
                checkAndRequestPermissions()
            }
        } else {
            // 禁用服务
            databaseHelper.setBooleanSetting(DatabaseHelper.KEY_SERVICE_ENABLED, false)
            stopUnlockMonitorService()
            Toast.makeText(this, R.string.service_disabled, Toast.LENGTH_SHORT).show()
            logger.logServiceStopped()
        }
        
        updateServiceStatus()
    }

    private fun startUnlockMonitorService() {
        val intent = Intent(this, UnlockMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopUnlockMonitorService() {
        val intent = Intent(this, UnlockMonitorService::class.java)
        stopService(intent)
    }

    private fun showTestSmsDialog() {
        if (!hasAllPermissions()) {
            checkAndRequestPermissions()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("测试短信")
            .setMessage("确定要向所有联系人发送测试短信吗？")
            .setPositiveButton("确定") { _, _ ->
                sendTestSms()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun sendTestSms() {
        smsSender.sendAlertToAllContacts(
            onSuccess = { count ->
                Toast.makeText(this, "测试短信已发送给 $count 位联系人", Toast.LENGTH_SHORT).show()
                logger.logEvent("TEST_SMS", "Test SMS sent to $count contacts")
            },
            onFailure = { error ->
                Toast.makeText(this, "发送失败: ${error.message}", Toast.LENGTH_SHORT).show()
                logger.logEvent("TEST_SMS_FAILED", "Failed: ${error.message}")
            }
        )
    }

    private fun showLogsDialog() {
        val logs = logger.getLogs()
        val logText = if (logs.isEmpty()) {
            "暂无日志记录"
        } else {
            logs.takeLast(50).joinToString("\n")
        }

        AlertDialog.Builder(this)
            .setTitle("系统日志")
            .setMessage(logText)
            .setPositiveButton("清空日志") { _, _ ->
                logger.clearLogs()
                Toast.makeText(this, "日志已清空", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showAddContactDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_contact, null)
        val etName = dialogView.findViewById<android.widget.EditText>(R.id.etContactName)
        val etPhone = dialogView.findViewById<android.widget.EditText>(R.id.etContactPhone)
        val btnSelectContact = dialogView.findViewById<Button>(R.id.btnSelectContact)

        btnSelectContact.setOnClickListener {
            // TODO: 实现从通讯录选择联系人
            Toast.makeText(this, "请手动输入联系人信息", Toast.LENGTH_SHORT).show()
        }

        AlertDialog.Builder(this)
            .setTitle("添加联系人")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val name = etName.text.toString().trim()
                val phone = etPhone.text.toString().trim()

                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    contactManager.addContact(name, phone)
                    loadContacts()
                    Toast.makeText(this, "联系人已添加", Toast.LENGTH_SHORT).show()
                    logger.logEvent("CONTACT_ADDED", "Added: $name ($phone)")
                } else {
                    Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showEditContactDialog(contact: com.goodwork.app.database.Contact) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_contact, null)
        val etName = dialogView.findViewById<android.widget.EditText>(R.id.etContactName)
        val etPhone = dialogView.findViewById<android.widget.EditText>(R.id.etContactPhone)

        etName.setText(contact.name)
        etPhone.setText(contact.phone)

        AlertDialog.Builder(this)
            .setTitle("编辑联系人")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val name = etName.text.toString().trim()
                val phone = etPhone.text.toString().trim()

                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    contactManager.updateContact(contact.id, name, phone)
                    loadContacts()
                    Toast.makeText(this, "联系人已更新", Toast.LENGTH_SHORT).show()
                    logger.logEvent("CONTACT_UPDATED", "Updated: $name ($phone)")
                } else {
                    Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun confirmDeleteContact(contact: com.goodwork.app.database.Contact) {
        AlertDialog.Builder(this)
            .setTitle("删除联系人")
            .setMessage("确定要删除 ${contact.name} 吗？")
            .setPositiveButton("删除") { _, _ ->
                contactManager.deleteContact(contact.id)
                loadContacts()
                Toast.makeText(this, "联系人已删除", Toast.LENGTH_SHORT).show()
                logger.logEvent("CONTACT_DELETED", "Deleted: ${contact.name}")
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showEditTemplateDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_template, null)
        val etTemplate = dialogView.findViewById<android.widget.EditText>(R.id.etSmsTemplate)

        etTemplate.setText(tvSmsTemplate.text)

        AlertDialog.Builder(this)
            .setTitle("编辑短信模板")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val template = etTemplate.text.toString().trim()

                if (template.isNotEmpty()) {
                    databaseHelper.setSetting(DatabaseHelper.KEY_SMS_TEMPLATE, template)
                    loadSmsTemplate()
                    Toast.makeText(this, "模板已保存", Toast.LENGTH_SHORT).show()
                    logger.logEvent("TEMPLATE_UPDATED", "New template saved")
                } else {
                    Toast.makeText(this, "模板不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun checkAndRequestPermissions() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun hasAllPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            
            if (allGranted) {
                Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.permission_required, Toast.LENGTH_SHORT).show()
                switchService.isChecked = false
            }
        }
    }
}
