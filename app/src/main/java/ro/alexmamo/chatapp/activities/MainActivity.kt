package ro.alexmamo.chatapp.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import ro.alexmamo.chatapp.R
import ro.alexmamo.chatapp.models.User

class MainActivity : AppCompatActivity() {
    private var firebaseAuth: FirebaseAuth? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private var googleApiClient: GoogleApiClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        firebaseAuth = FirebaseAuth.getInstance()
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
        }

        googleApiClient = GoogleApiClient.Builder(this).addApi(Auth.GOOGLE_SIGN_IN_API).build()
    }

    private fun signOut() {
        if (firebaseAuth != null) {
            firebaseAuth!!.signOut()
        }

        if (googleApiClient!!.isConnected) {
            Auth.GoogleSignInApi.signOut(googleApiClient)
        }
    }

    override fun onStart() {
        super.onStart()
        googleApiClient!!.connect()
        firebaseAuth!!.addAuthStateListener(this.authStateListener!!)

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            val fromUid = firebaseUser.uid
            val rootRef = FirebaseFirestore.getInstance()
            val uidRef = rootRef.collection("users").document(fromUid)
            uidRef.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    if (document.exists()) {
                        val fromUser = document.toObject(User::class.java)
                        val userRoomsRef = rootRef.collection("rooms").document(fromUid).collection("userRooms")
                        userRoomsRef.get().addOnCompleteListener{ t ->
                            if (t.isSuccessful) {
                                val listOfToUserNames = ArrayList<String>()
                                val listOfToUsers = ArrayList<User>()
                                val listOfRooms = ArrayList<String>()
                                for (d in t.result) {
                                    val toUser = d.toObject(User::class.java)
                                    listOfToUserNames.add(toUser.userName)
                                    listOfToUsers.add(toUser)
                                    listOfRooms.add(d.id)
                                }

                                val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listOfToUserNames)
                                list_viw.adapter = arrayAdapter
                                list_viw.onItemClickListener = AdapterView.OnItemClickListener{ _, _, position, _ ->
                                    val intent = Intent(this, ChatActivity::class.java)
                                    intent.putExtra("fromUser", fromUser)
                                    intent.putExtra("toUser", listOfToUsers[position])
                                    intent.putExtra("roomId", listOfRooms[position])
                                    startActivity(intent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()

        if (googleApiClient!!.isConnected) {
            googleApiClient!!.disconnect()
        }

        firebaseAuth!!.removeAuthStateListener(this.authStateListener!!)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.add_friend -> {
                val intent = Intent(this, ContactsActivity::class.java)
                startActivity(intent)
                true
            }

            R.id.sign_out_button -> {
                signOut()
                true
            }

            else -> super.onOptionsItemSelected(menuItem)
        }
    }

    companion object {
        //private const val TAG = "MainActivityTag"
    }
}