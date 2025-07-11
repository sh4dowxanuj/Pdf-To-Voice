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
import com.example.pdftovoice.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {
    
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    
    private val authViewModel: AuthViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        observeViewModel()
    }
    
    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            authViewModel.login(email, password)
        }
        
        binding.tvSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }
    }
    
    private fun observeViewModel() {
        authViewModel.loginResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = { _ ->
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.login_successful),
                        Toast.LENGTH_SHORT
                    ).show()
                    // Navigate to main app or dashboard
                    findNavController().navigate(R.id.action_loginFragment_to_mainActivity)
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
            binding.btnLogin.isEnabled = !isLoading
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
