package com.projemanag.firebase

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.projemanag.activities.MyProfileActivity
import com.projemanag.activities.*
import com.projemanag.model.Board
import com.projemanag.model.User
import com.projemanag.utils.Constants

class FireStore {

    private val mFireStore = FirebaseFirestore.getInstance()

    fun registerUser(activity: SignUpActivity, userInfo: User) {
        mFireStore.collection(Constants.USERS).document(getCurrentUserId())
            .set(userInfo, SetOptions.merge()).addOnSuccessListener {
                activity.userRegisteredSucces()
            }.addOnFailureListener {
                    _ -> Log.e(activity.javaClass.simpleName, "Error Writing Document")
            }
    }

    fun getCurrentUserId(): String{

        var currentUser = FirebaseAuth.getInstance().currentUser
        var currentUserID = ""
        if (currentUser != null){
            currentUserID = currentUser.uid
        }
        return currentUserID
    }

    fun getBoardDetail(activity: TaskListActivity, documentId: String) {
        mFireStore.collection(Constants.BOARDS).document(documentId)
            .get().addOnSuccessListener {
                    document ->
                Log.i(activity.javaClass.simpleName, document.toString())
                val board = document.toObject(Board::class.java)!!
                board.documentId = document.id
                activity.boardDetail(board)

            }.addOnFailureListener { e ->

                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while Getting Board Detail", e)
            }
    }

    fun loadUserData(activity: Activity, readBoardList: Boolean = false) {
        mFireStore.collection(Constants.USERS).document(getCurrentUserId())
            .get().addOnSuccessListener { document ->
                val loggedInUser = document.toObject(User::class.java)!!

                when(activity){
                    is SingInActivity ->{
                        activity.SignInSuccess(loggedInUser)
                    }

                    is  MainActivity ->{
                        activity.updateNavigationUserDetail(loggedInUser, readBoardList)
                    }

                    is MyProfileActivity ->{
                        activity.setUserDataInUI(loggedInUser)
                    }
                }

            }.addOnFailureListener {
                e->
                when(activity){
                    is SingInActivity ->{
                        activity.hideProgressDialog()
                    }

                    is  MainActivity ->{
                        activity.hideProgressDialog()
                    }
                }
                Log.w("Sign In", "Sign In with Email Failure", e)
            }
    }

    fun createBoard(activity: CreateBoardActivity, board: Board) {
        mFireStore.collection(Constants.BOARDS)
            .document().set(board, SetOptions.merge())
            .addOnSuccessListener {
                Log.e(activity.javaClass.simpleName, "Board Created Successfully.")
                Toast.makeText(activity, "Board Created Successfully.", Toast.LENGTH_SHORT).show()
                activity.boardCreatedSuccessfully()
            }.addOnFailureListener {
                exception ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while Creating Board.", exception)
            }
    }

    fun getBoardList(activity: MainActivity) {
        mFireStore.collection(Constants.BOARDS).whereArrayContains(Constants.ASSIGNED_TO, getCurrentUserId())
            .get().addOnSuccessListener {
                document ->
                Log.i(activity.javaClass.simpleName, document.documents.toString())
                val boardList: ArrayList<Board> = ArrayList()

                for (i in document.documents) {
                    val board = i.toObject(Board::class.java)!!
                    board.documentId = i.id
                    boardList.add(board)
                }

                activity.populateBoardtoUI(boardList)
            }.addOnFailureListener { e ->

                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while Creating Board", e)
            }
    }

    fun updateUserProfileData(activity: Activity, userHashMap: HashMap<String, Any>) {
        mFireStore.collection(Constants.USERS).document(getCurrentUserId()).update(userHashMap)
            .addOnSuccessListener {
                Log.i(activity.javaClass.simpleName , "Profile Data Update! ")
                Toast.makeText(activity, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show()
                when(activity){
                    is MainActivity ->{
                        activity.tokenUpdateSuccess()
                    }
                    is MyProfileActivity ->{
                        activity.profileUpdateSuccess()
                    }
                }
            }.addOnFailureListener {
                e ->
                when(activity){
                    is MainActivity ->{
                        activity.hideProgressDialog()
                    }
                    is MyProfileActivity ->{
                        activity.hideProgressDialog()
                    }
                }
                Log.e(activity.javaClass.simpleName, "Error while creating a board", e)
                Toast.makeText(activity, "Error while updating profile", Toast.LENGTH_SHORT).show()
            }
    }

    fun addUpdateTaskList(activity: Activity, board: Board) {
        val taskListHashMap = HashMap<String, Any>()
        taskListHashMap[Constants.TASK_LIST] = board.taskList

        mFireStore.collection(Constants.BOARDS).document(board.documentId).update(taskListHashMap)
            .addOnSuccessListener {
                Log.e(activity.javaClass.simpleName, "TaskList Updated Successfully")
                if (activity is TaskListActivity)
                    activity.addUpdateTaskListSuccess()
                else if (activity is CardDetailActivity)
                    activity.addUpdateTaskListSuccess()
            }.addOnFailureListener {
                exception ->
                if (activity is TaskListActivity)
                    activity.hideProgressDialog()
                else if (activity is CardDetailActivity)
                    activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while Creating Board", exception)
            }
    }

    fun getAssignedMembersListDetails(activity: Activity, assignedTo: ArrayList<String>) {
        mFireStore.collection(Constants.USERS)
            .whereIn(Constants.ID, assignedTo).get().addOnSuccessListener {
                document ->
                Log.e(activity.javaClass.simpleName, document.documents.toString())

                val userList: ArrayList<User> = ArrayList()
                for (i in document.documents) {
                    val user = i.toObject(User::class.java)!!
                    userList.add(user)
                }
                if (activity is MembersActivity)
                    activity.setupMemberList(userList)
                else if (activity is TaskListActivity)
                    activity.boardMemberDetailList(userList)
            }.addOnFailureListener {
                e ->
                if (activity is MembersActivity)
                    activity.hideProgressDialog()
                else if (activity is TaskListActivity)
                    activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while access Members Detail ", e)
            }
    }

    fun getMemberDetail(activity: MembersActivity, email: String) {
        mFireStore.collection(Constants.USERS)
            .whereEqualTo(Constants.EMAIL, email).get().addOnSuccessListener {
                document ->
                if (document.documents.size > 0) {
                    val user = document.documents[0].toObject(User::class.java)!!
                    activity.membersDetails(user)
                }else {
                    activity.hideProgressDialog()
                    activity.showErrorSnackBack("No Members Founds")
                }
            }.addOnFailureListener {
                e -> activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while getting Users Detail", e)
            }
    }

    fun assignedMemberToBoard(activity: MembersActivity, board: Board, user: User) {
        val assignedToHashmap = HashMap<String, Any>()
        assignedToHashmap[Constants.ASSIGNED_TO] = board.assignedTo

        mFireStore.collection(Constants.BOARDS).document(board.documentId)
            .update(assignedToHashmap).addOnSuccessListener {
                activity.memberAssignedSuccess(user)
            }.addOnFailureListener {
                e -> activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while getting Member " ,e)
            }
    }
}