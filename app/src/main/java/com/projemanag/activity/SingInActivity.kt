package com.projemanag.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.projemanag.R
import com.projemanag.firebase.FireStore
import com.projemanag.model.User
import kotlinx.android.synthetic.main.activity_sing_in.*
import kotlinx.android.synthetic.main.activity_sing_in.et_email
import kotlinx.android.synthetic.main.activity_sing_in.et_password

class SingInActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sing_in)

        auth = FirebaseAuth.getInstance()

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setupActionBar()

        btn_sign_in.setOnClickListener {
            SignInUser()
        }

    }

    fun SignInSuccess(user: User) {
        hideProgressDialog()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar_sign_in_activity)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_black_color_back_24dp)
        toolbar_sign_in_activity.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun SignInUser(){
        val email: String = et_email.text.toString().trim{it <= ' '}
        val password: String = et_password.text.toString().trim{it <= ' '}

        if (validateForm(email, password)){
            showProgressDialog(resources.getString(R.string.please_wait))
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    hideProgressDialog()
                    if (task.isSuccessful) {

                        FireStore().loadUserData(this)

                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("SignIn", "signInWithEmail:failure", task.exception)
                        Toast.makeText(baseContext, "Authentication failed.",
                            Toast.LENGTH_SHORT).show()
                        // ...
                    }

                    // ...
                }
        }

    }

    private fun validateForm(email: String, password: String): Boolean{
        return when {
            TextUtils.isEmpty(email) ->{
                showErrorSnackBack("Please enter a Email Address")
                false
            }
            TextUtils.isEmpty(password) ->{
                showErrorSnackBack("Please enter a Password")
                false
            }else -> {
                true
            }
        }
    }

}