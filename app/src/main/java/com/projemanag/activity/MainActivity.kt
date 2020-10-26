package com.projemanag.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.iid.FirebaseInstanceId
import com.projemanag.R
import com.projemanag.activities.MyProfileActivity
import com.projemanag.adapters.BoardItemsAdapter
import com.projemanag.firebase.FireStore
import com.projemanag.model.Board
import com.projemanag.utils.Constants
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.nav_header_main.*

class MainActivity : BaseActivity(),NavigationView.OnNavigationItemSelectedListener {

    companion object {
        const val MY_PROFILE_REQUEST_CODE = 11
        const val CREATE_BOARD_REQUEST_CODE = 12
    }

    private lateinit var mUsername: String
    private lateinit var mSharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupActionBar()
        nav_view.setNavigationItemSelectedListener(this)

        mSharedPrefs = this.getSharedPreferences(Constants.PROJEMANA_PREFERENCE, Context.MODE_PRIVATE)
        val tokenUpdated = mSharedPrefs.getBoolean(Constants.FCM_TOKEN_UPDATED, false)
        if (tokenUpdated){
            showProgressDialog(resources.getString(R.string.please_wait))
            FireStore().loadUserData(this, true)
        }else{
            FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener(this@MainActivity){instanceIdResult ->
                updateFCMToken(instanceIdResult.token)
            }
        }

        FireStore().loadUserData(this, true)

        fab_create_board.setOnClickListener {
            val intent = Intent(this, CreateBoardActivity::class.java)
            intent.putExtra(Constants.NAME, mUsername)
            startActivityForResult(intent, CREATE_BOARD_REQUEST_CODE)
        }
    }

    fun tokenUpdateSuccess(){
        hideProgressDialog()
        val editor: SharedPreferences.Editor = mSharedPrefs.edit()
        editor.putBoolean(Constants.FCM_TOKEN_UPDATED, true)
        editor.apply()
        showProgressDialog(resources.getString(R.string.please_wait))
        FireStore().loadUserData(this, true)
    }

    private fun updateFCMToken(token: String){
        val userHashmap = HashMap<String, Any>()
        userHashmap[Constants.FCM_TOKEN] = token
        showProgressDialog(resources.getString(R.string.please_wait))
        FireStore().updateUserProfileData(this, userHashmap)
    }

    fun updateNavigationUserDetail(user: com.projemanag.model.User, readBoardList: Boolean) {
        hideProgressDialog()
        mUsername = user.name

        Glide
            .with(this)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(iv_user_image);

        tv_username.text = user.name
        if (readBoardList) {
            showProgressDialog(resources.getString(R.string.please_wait))
            FireStore().getBoardList(this)
        }
    }

    fun populateBoardtoUI(boardlist: ArrayList<Board>) {
        hideProgressDialog()
        if (boardlist.size > 0) {
            rv_boards_list.visibility = View.VISIBLE
            tv_no_boards_available.visibility = View.GONE

            rv_boards_list.layoutManager = LinearLayoutManager(this)
            rv_boards_list.setHasFixedSize(true)

            val adapter = BoardItemsAdapter(this, boardlist)
            rv_boards_list.adapter = adapter

            adapter.setOnClickListener(object : BoardItemsAdapter.OnClickListener{
                override fun onClick(position: Int, model: Board) {
                    val intent = Intent(this@MainActivity, TaskListActivity::class.java)
                    intent.putExtra(Constants.DOCUMENT_ID, model.documentId)
                    startActivity(intent)
                }
            })
        }else{
            rv_boards_list.visibility = View.GONE
            tv_no_boards_available.visibility = View.VISIBLE
        }
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar_main_activity)
        toolbar_main_activity.setNavigationIcon(R.drawable.ic_action_navigation_menu)
        toolbar_main_activity.setNavigationOnClickListener {
            toggleDrawer()
        }
    }

    private fun toggleDrawer(){
        if (drawer_layout.isDrawerOpen(GravityCompat.START)){
            drawer_layout.closeDrawer(GravityCompat.START)
        }else{
            drawer_layout.openDrawer(GravityCompat.START)
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)){
            drawer_layout.closeDrawer(GravityCompat.START)
        }else{
            doubleBacktoExit()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.nav_my_profile -> {
                startActivityForResult(Intent(this, MyProfileActivity::class.java), MY_PROFILE_REQUEST_CODE)
            }
            R.id.nav_sign_out -> {
                FirebaseAuth.getInstance().signOut()

                mSharedPrefs.edit().clear().apply()

                val intent = Intent(this, IntroActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == MY_PROFILE_REQUEST_CODE) {
            FireStore().loadUserData(this)
        }else if (resultCode == Activity.RESULT_OK && requestCode == CREATE_BOARD_REQUEST_CODE){
            FireStore().getBoardList(this)
        }else{
            Log.e("Cancelled Main Activity", "Cancelled Result")
        }
    }
}
