package com.project.mapsapp.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.project.mapsapp.databinding.FragmentLoginBinding


class LoginFragment : Fragment() {
    lateinit var binding: FragmentLoginBinding
    val validationCount : MutableLiveData<Int> = MutableLiveData()

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
        validationCount.postValue(0)

        observer()

        listeners()

        setData()

    }

    private fun observer() {
        validationCount.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), "$it", Toast.LENGTH_SHORT).show()
            binding.loginButton.isEnabled = (it == 2)
        }
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
            if(regexEmail.matches(it.toString())) {
                binding.loginEmailLayout.isErrorEnabled = false
                validationCount.postValue(validationCount.value?.plus(1))
            } else {
                validationCount.postValue(validationCount.value?.plus(1))
                binding.loginEmailLayout.error = "Email is not valid"
            }
        }

        binding.loginPassword.doAfterTextChanged {
            if(regexPassword.matches(it.toString())) {
                binding.loginPasswordLayout.isErrorEnabled = false
                validationCount.postValue(validationCount.value?.plus(1))
            } else {
                binding.loginPasswordLayout.error = "Email is not valid"
            }
        }
    }

    private fun checkValidation() {
        var matched = 0

        if(regexPassword.matches(binding.loginPassword.text.toString())) {
            matched++
            binding.loginPasswordLayout.isErrorEnabled = false
        } else {
            binding.loginPasswordLayout.error = "Password should contain one lowercase, one uppercase, numbers and letters"
        }
        binding.loginButton.isEnabled = ( matched == 2 )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_EMAIL, binding.loginEmail.text.toString())
        outState.putString(KEY_PASSWORD, binding.loginPassword.text.toString())
        super.onSaveInstanceState(outState)
    }
}