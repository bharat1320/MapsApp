package com.project.mapsapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.project.mapsapp.ui.login.LoginFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        addFragment(LoginFragment())
    }

    private fun addFragment(fragment : Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_fragment_view, fragment)
            .addToBackStack(fragment::class.simpleName)
            .commit()
    }

}