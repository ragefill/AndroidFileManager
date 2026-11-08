package com.ragefill.filemanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.ragefill.filemanager.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fileAdapter: FileAdapter
    private var currentPath: File = Environment.getExternalStorageDirectory()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            loadFiles(currentPath)
        } else {
            Toast.makeText(this, "Storage permission is required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Start Python
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        setupFileBrowser()
        setupTerminal()
        checkAndRequestPermissions()

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_files -> {
                    binding.filesContainer.visibility = View.VISIBLE
                    binding.terminalContainer.visibility = View.GONE
                    true
                }
                R.id.nav_terminal -> {
                    binding.filesContainer.visibility = View.GONE
                    binding.terminalContainer.visibility = View.VISIBLE
                    true
                }
                else -> false
            }
        }
    }

    private fun setupFileBrowser() {
        fileAdapter = FileAdapter { file ->
            if (file.isDirectory) {
                currentPath = file
                loadFiles(currentPath)
            } else {
                // Simple text viewer for now
                if (file.extension in listOf("txt", "md", "py", "json", "xml", "log", "kt", "java")) {
                    showTextFile(file)
                } else {
                    Toast.makeText(this, "Preview not supported for this type", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.recyclerFiles.layoutManager = LinearLayoutManager(this)
        binding.recyclerFiles.adapter = fileAdapter

        binding.btnUp.setOnClickListener {
            currentPath.parentFile?.let {
                currentPath = it
                loadFiles(currentPath)
            }
        }

        binding.btnRefresh.setOnClickListener {
            loadFiles(currentPath)
        }
    }

    private fun setupTerminal() {
        binding.btnRun.setOnClickListener {
            val code = binding.etCode.text.toString().trim()
            if (code.isEmpty()) return@setOnClickListener

            try {
                val py = Python.getInstance()
                val module = py.getModule("builtins")
                // Simple exec via a helper
                val result = runPython(code)
                binding.tvOutput.append("> $code\n$result\n\n")
                binding.etCode.setText("")
            } catch (e: Exception) {
                binding.tvOutput.append("> $code\nError: ${e.message}\n\n")
            }
            // Scroll to bottom
            binding.scrollOutput.post {
                binding.scrollOutput.fullScroll(View.FOCUS_DOWN)
            }
        }

        binding.btnClear.setOnClickListener {
            binding.tvOutput.text = ""
        }
    }

    private fun runPython(code: String): String {
        val py = Python.getInstance()
        // Use a simple approach: exec in a restricted way and capture output
        // For production you would redirect sys.stdout
        val locals = py.getBuiltins().callAttr("dict")
        try {
            py.getBuiltins().callAttr("exec", code, locals)
            // Try to get last expression result if possible; otherwise just success
            return "OK (executed)"
        } catch (e: Exception) {
            return "Error: ${e.message}"
        }
    }

    private fun loadFiles(dir: File) {
        binding.tvCurrentPath.text = dir.absolutePath
        val files = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
        fileAdapter.submitList(files)
    }

    private fun showTextFile(file: File) {
        try {
            val content = file.readText()
            AlertDialog.Builder(this)
                .setTitle(file.name)
                .setMessage(content.take(5000) + if (content.length > 5000) "\n\n... (truncated)" else "")
                .setPositiveButton("Close", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot read file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This app needs All files access to manage your files. You will be taken to settings.")
                    .setPositiveButton("Open settings") { _, _ ->
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            intent.data = Uri.parse("package:$packageName")
                            startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            startActivity(intent)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                loadFiles(currentPath)
            }
        } else {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val needed = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needed.isNotEmpty()) {
                requestPermissionLauncher.launch(needed.toTypedArray())
            } else {
                loadFiles(currentPath)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            loadFiles(currentPath)
        }
    }
}
