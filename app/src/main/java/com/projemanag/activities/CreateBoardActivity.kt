package com.projemanag.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.projemanag.R
import com.projemanag.firebase.FireStore
import com.projemanag.model.Board
import com.projemanag.utils.Constants
import kotlinx.android.synthetic.main.activity_create_board.*
import java.io.IOException

class CreateBoardActivity : BaseActivity() {

    private var mSelectedImageFileUri: Uri? = null
    private lateinit var mUsername: String
    private var mBoardImageURL: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_board)
        setupActionBar()

        if (intent.hasExtra(Constants.NAME)){
            mUsername = intent.getStringExtra(Constants.NAME)
        }

        iv_board_image.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                Constants.showImageChooser(this)
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    Constants.READ_STORAGE_PERMISSION_CODE
                )
            }
        }

        btn_create.setOnClickListener {
            if (mSelectedImageFileUri != null){
                uploadBoardImage()
            }else{
                showProgressDialog(resources.getString(R.string.please_wait))
                createBoard()
            }
        }
    }

    fun boardCreatedSuccessfully() {
        hideProgressDialog()
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun createBoard() {
        val assignedUserArrayList: ArrayList<String> = ArrayList()
        assignedUserArrayList.add(getCurrentUserID())

        var board = Board(
            et_board_name.text.toString(),
            mBoardImageURL,
            mUsername,
            assignedUserArrayList
        )

        FireStore().createBoard(this, board)
    }

    private fun uploadBoardImage() {
        showProgressDialog(resources.getString(R.string.please_wait))

        val sRef: StorageReference =
            FirebaseStorage.getInstance().reference.child(
                "BOARD_IMAGE"
                        + System.currentTimeMillis() + "."
                        + Constants.getFileExtension(this, mSelectedImageFileUri)

            )

        sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener {
                taskSnapshot ->
            Log.i(
                "Board Image URL", taskSnapshot.metadata!!.reference!!.downloadUrl.toString()
            )
            taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener {
                    uri ->
                Log.i("Board Image URL", uri.toString())
                mBoardImageURL = uri.toString()
                hideProgressDialog()
                createBoard()
            }
        }.addOnFailureListener {
                exception ->
            Toast.makeText(this@CreateBoardActivity, exception.message, Toast.LENGTH_SHORT).show()
            hideProgressDialog()
        }
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar_create_board_activity)
        val actionBar = supportActionBar!!
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
        actionBar.title = resources.getString(R.string.create_board_title)
        toolbar_create_board_activity.setNavigationOnClickListener { onBackPressed() }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.READ_STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Constants.showImageChooser(this)
            }
        } else {
            Toast.makeText(
                this,
                "you just denied permission to access this feature please allow it from setting ",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK
            && requestCode == Constants.PICK_IMAGE_REQUEST_CODE
            && data!!.data != null
        ) {
            mSelectedImageFileUri = data.data

            try {
                Glide
                    .with(this)
                    .load(mSelectedImageFileUri)
                    .centerCrop()
                    .placeholder(R.drawable.ic_board_place_holder)
                    .into(iv_board_image);
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}