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
    var latestEmail : String = ""
    var latestPassword : String = ""

    companion object {
        val KEY_EMAIL = "KEY_EMAIL"
        val KEY_PASSWORD = "KEY_PASSWORD"

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
            latestEmail = it.toString()
            checkValidation()
        }

        binding.loginPassword.doAfterTextChanged {
            latestPassword = it.toString()
            checkValidation()
        }

        binding.loginButton.setOnClickListener {
            val bundle = Bundle()
            bundle.putString(KEY_EMAIL, binding.loginEmail.text.toString())
            findNavController().navigate(R.id.action_loginFragment_to_mapFragment,bundle)
        }
    }

    private fun checkValidation() {
        var matched = 0

        if(regexEmail.matches(latestEmail)) {
            matched++
            binding.loginEmailLayout.isErrorEnabled = false
        } else {
            if(latestEmail.isNotBlank()) {
                binding.loginEmailLayout.error = "Email is not valid"
            }
        }
        if(regexPassword.matches(latestPassword)) {
            matched++
            binding.loginPasswordLayout.isErrorEnabled = false
        } else {
            if(latestPassword.isNotBlank()) {
                binding.loginPasswordLayout.error =
                    "Password should contain one lowercase, one uppercase, numbers and letters"
            }
        }
        binding.loginButton.isEnabled = ( matched == 2 )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_EMAIL, latestEmail)
        outState.putString(KEY_PASSWORD, latestPassword)
        super.onSaveInstanceState(outState)
    }
}