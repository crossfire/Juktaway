package net.slashOmega.juktaway.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.row_user_list.view.*
import net.slashOmega.juktaway.ProfileActivity
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.UserListActivity
import net.slashOmega.juktaway.util.ImageUtil
import twitter4j.UserList

/**
 * Created on 2018/10/29.
 */
class UserListAdapter(c: Context, id: Int): ArrayAdapterBase<UserList>(c, id) {
    override val View.mView: (Int, ViewGroup?) -> Unit
        @SuppressLint("SetTextI18n")
        get() = { pos, _ -> getItem(pos)?.let { userList ->
            ImageUtil.displayRoundedImage(userList.user.biggerProfileImageURL, icon)
            list_name.text = userList.name
            screen_name.text = userList.user.screenName
            description.text = userList.description
            member_count.text = (userList.memberCount.toString()) + mContext?.getString(R.string.label_members)
            icon.setOnClickListener {
                mContext?.startActivity(Intent(context, ProfileActivity::class.java).apply {
                    putExtra("screenName", userList.user.screenName)
                })
            }
            setOnClickListener {
                mContext?.startActivity(Intent(context, UserListActivity::class.java).apply {
                    putExtra("userList", userList)
                })
            }
        }}
}