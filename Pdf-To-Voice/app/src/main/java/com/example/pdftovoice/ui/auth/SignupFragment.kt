package com.example.pdftovoice.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.pdftovoice.R
import com.example.pdftovoice.databinding.FragmentSignupBinding

class SignupFragment : Fragment() {
    
    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    
    private val authViewModel: AuthViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        observeViewModel()
    }
    
    private fun setupClickListeners() {
        binding.btnSignUp.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            
            authViewModel.register(email, username, password, confirmPassword)
        }
        
        binding.tvSignIn.setOnClickListener {
            findNavController().popBackStack()
        }
    }
    
    private fun observeViewModel() {
        authViewModel.registerResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = { _ ->
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.registration_successful),
                        Toast.LENGTH_SHORT
                    ).show()
                    // Navigate back to login
                    findNavController().popBackStack()
                },
                onFailure = { exception ->
                    Toast.makeText(
                        requireContext(),
                        exception.message ?: getString(R.string.error_occurred),
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
        
        authViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSignUp.isEnabled = !isLoading
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
