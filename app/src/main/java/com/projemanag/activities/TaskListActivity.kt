package com.projemanag.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.projemanag.R
import com.projemanag.adapters.TaskListItemAdapter
import com.projemanag.firebase.FireStore
import com.projemanag.model.Board
import com.projemanag.model.Card
import com.projemanag.model.Task
import com.projemanag.model.User
import com.projemanag.utils.Constants
import kotlinx.android.synthetic.main.activity_task_list.*

class TaskListActivity : BaseActivity() {

    private lateinit var mBoardDetails: Board
    private lateinit var mBoardDocumentID: String
    lateinit var mAssignedMemberDetailList: ArrayList<User>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_list)


        if (intent.hasExtra(Constants.DOCUMENT_ID)){
            mBoardDocumentID = intent.getStringExtra(Constants.DOCUMENT_ID)
        }

        showProgressDialog(resources.getString(R.string.please_wait))
        FireStore().getBoardDetail(this, mBoardDocumentID)
    }

    fun cardDetails(taskListPosition: Int, cardPosition: Int){
        val intent = Intent(this, CardDetailActivity::class.java)
        intent.putExtra(Constants.BOARD_DETAIL, mBoardDetails)
        intent.putExtra(Constants.TASK_LIST_ITEM_POSITION, taskListPosition)
        intent.putExtra(Constants.CARD_LIST_ITEM_POSITION, cardPosition)
        intent.putExtra(Constants.BOARD_MEMBER_LIST, mAssignedMemberDetailList)
        startActivityForResult(intent, CARD_DETAIL_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == MEMBERS_REQUEST_CODE || requestCode == CARD_DETAIL_REQUEST_CODE){
            showProgressDialog(resources.getString(R.string.please_wait))
            FireStore().getBoardDetail(this, mBoardDocumentID)
        }else{
            Log.e("Cancelled", "Cancelled")
        }
    }

    fun boardDetail(board: Board) {
        mBoardDetails = board

        hideProgressDialog()
        setupActionBar()


        showProgressDialog(resources.getString(R.string.please_wait))
        FireStore().getAssignedMembersListDetails(this, mBoardDetails.assignedTo)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_members, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_members -> {
                val intent = Intent(this, MembersActivity::class.java)
                intent.putExtra(Constants.BOARD_DETAIL, mBoardDetails)
                startActivityForResult(intent, MEMBERS_REQUEST_CODE)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar_task_list_activity)
        val actionBar = supportActionBar!!
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
        actionBar.title = mBoardDetails.name
        toolbar_task_list_activity.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    fun addUpdateTaskListSuccess() {
        hideProgressDialog()

        showProgressDialog(resources.getString(R.string.please_wait))
        FireStore().getBoardDetail(this, mBoardDetails.documentId)
    }

    fun createTaskList(taskListName: String) {
        val task = Task(taskListName, FireStore().getCurrentUserId())
        mBoardDetails.taskList.add(0,task)
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size -1)

        showProgressDialog(resources.getString(R.string.please_wait))
        FireStore().addUpdateTaskList(this, mBoardDetails)
    }

    fun updateTaskList(position: Int, listName: String, model: Task){
        val task = Task(listName, model.createdBy)
        mBoardDetails.taskList[position] = task
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size -1)

        showProgressDialog(resources.getString(R.string.please_wait))
        FireStore().addUpdateTaskList(this, mBoardDetails)
    }

    fun deleteTaskList(position: Int) {
        mBoardDetails.taskList.removeAt(position)
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size -1)

        showProgressDialog(resources.getString(R.string.please_wait))
        FireStore().addUpdateTaskList(this, mBoardDetails)
    }

    fun addCardToTaskList(position: Int, cardName: String) {
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size -1)

        val cardAssignedUserList: ArrayList<String> = ArrayList()
        cardAssignedUserList.add(FireStore().getCurrentUserId())

        val card = Card(cardName, FireStore().getCurrentUserId(), cardAssignedUserList)
        val cardsList = mBoardDetails.taskList[position].cards
        cardsList.add(card)

        val task = Task(
            mBoardDetails.taskList[position].title,
            mBoardDetails.taskList[position].createdBy,
            cardsList
        )

        mBoardDetails.taskList[position] = task

        showProgressDialog(resources.getString(R.string.please_wait))
        FireStore().addUpdateTaskList(this, mBoardDetails)
    }

    fun boardMemberDetailList(list: ArrayList<User>) {
        mAssignedMemberDetailList = list
        hideProgressDialog()

        val addTaskList = Task(resources.getString(R.string.add_list))
        mBoardDetails.taskList.add(addTaskList)

        rv_task_list.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rv_task_list.setHasFixedSize(true)

        val adapter = TaskListItemAdapter(this, mBoardDetails.taskList)
        rv_task_list.adapter = adapter

    }

    fun updateCardsInTaskList(taskListPosition: Int, cards: ArrayList<Card>){
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size -1)
        mBoardDetails.taskList[taskListPosition].cards = cards
        showProgressDialog(resources.getString(R.string.please_wait))
        FireStore().addUpdateTaskList(this, mBoardDetails)
    }

    companion object{
        const val MEMBERS_REQUEST_CODE: Int = 13
        const val CARD_DETAIL_REQUEST_CODE: Int = 14
    }
}