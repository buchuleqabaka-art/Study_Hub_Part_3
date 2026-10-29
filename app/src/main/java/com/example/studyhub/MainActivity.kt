package com.example.studyhub

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.studyhub.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.bottomNav.setOnItemSelectedListener { item ->
            val f: Fragment? = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_groups -> GroupsFragment()
                R.id.nav_chats -> ChatsFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> null
            }
            if (f != null) {
                supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                supportFragmentManager.beginTransaction().replace(R.id.container, f).commit()
            }
            f != null
        }
        if (savedInstanceState == null) b.bottomNav.selectedItemId = R.id.nav_home

        // centre "+" button: quick actions
        b.fabAdd.setOnClickListener {
            AlertDialog.Builder(this)
                .setItems(arrayOf("New Task", "Schedule Study Session", "Create Group", "Join Group")) { _, i ->
                    when (i) {
                        0 -> Dialogs.newTask(this)
                        1 -> Dialogs.newSession(this)
                        2 -> Dialogs.createGroup(this)
                        3 -> Dialogs.joinGroup(this)
                    }
                }.show()
        }
    }

    // open a screen on top of the current tab (with back support)
    fun open(f: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, f)
            .addToBackStack(null)
            .commit()
    }

    fun selectTab(id: Int) { b.bottomNav.selectedItemId = id }
}
