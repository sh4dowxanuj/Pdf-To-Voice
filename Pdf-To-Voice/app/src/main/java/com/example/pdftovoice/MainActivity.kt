package com.example.pdftovoice

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.navigation.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.pdftovoice.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // Set up the toolbar as action bar
            setSupportActionBar(findViewById(androidx.appcompat.R.id.action_bar))
            
            val navController = findNavController(R.id.nav_host_fragment)
            setupActionBarWithNavController(navController)
            
            Log.d("MainActivity", "Activity created successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onCreate: ${e.message}", e)
            // Don't rethrow, just log and continue with basic setup
            try {
                binding = ActivityMainBinding.inflate(layoutInflater)
                setContentView(binding.root)
            } catch (fallbackException: Exception) {
                Log.e("MainActivity", "Fallback setup also failed", fallbackException)
                throw fallbackException
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
