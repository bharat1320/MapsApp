package com.project.mapsapp.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.project.mapsapp.R
import com.project.mapsapp.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {
    lateinit var binding: FragmentLoginBinding

    var savedEmail : String? = null
    var savedPassword : String? = null

    companion object {
        val KEY_EMAIL = "email"
        val KEY_PASSWORD = "password"

        val regexPassword = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$")
        val regexEmail = Regex("^([a-zA-Z0-9_\\-\\.]+)@([a-zA-Z0-9\\-\\.]+)\\.([a-zA-Z]{2,5})$")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            savedInstanceState.getString(KEY_EMAIL)?.let {
                savedEmail = it
            }
            savedInstanceState.getString(KEY_PASSWORD)?.let {
                savedPassword = it
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.loginButton.isEnabled = false

        listeners()

        setData()

    }

    private fun setData() {
        savedEmail?.let { email ->
            binding.loginEmail.setText(email)
            savedPassword?.let { password ->
                binding.loginPassword.setText(password)
                checkValidation()
            }
        }
    }

    private fun listeners() {
        binding.loginEmail.doAfterTextChanged {
            binding.loginEmailLayout.isErrorEnabled = true
            checkValidation()
        }

        binding.loginPassword.doAfterTextChanged {
            binding.loginPasswordLayout.isErrorEnabled = true
            checkValidation()
        }

        binding.loginButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_mapFragment)
        }
    }

    private fun checkValidation() {
        var matched = 0
        val email = binding.loginEmail.text.toString()
        val password = binding.loginPassword.text.toString()

        if(regexEmail.matches(email)) {
            matched++
            binding.loginEmailLayout.isErrorEnabled = false
        } else {
            if(email.isNotBlank()) {
                binding.loginEmailLayout.error = "Email is not valid"
            }
        }
        if(regexPassword.matches(password)) {
            matched++
            binding.loginPasswordLayout.isErrorEnabled = false
        } else {
            if(password.isNotBlank()) {
                binding.loginPasswordLayout.error =
                    "Password should contain one lowercase, one uppercase, numbers and letters"
            }
        }
        binding.loginButton.isEnabled = ( matched == 2 )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_EMAIL, binding.loginEmail.text.toString())
        outState.putString(KEY_PASSWORD, binding.loginPassword.text.toString())
        super.onSaveInstanceState(outState)
    }
}