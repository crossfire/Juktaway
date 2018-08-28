package info.justaway

import android.app.NotificationManager
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.Window
import android.view.WindowManager
import android.widget.ListView
import de.greenrobot.event.EventBus
import info.justaway.adapter.TwitterAdapter
import info.justaway.event.AlertDialogEvent
import info.justaway.event.action.StatusActionEvent
import info.justaway.event.model.StreamingDestroyStatusEvent
import info.justaway.listener.StatusClickListener
import info.justaway.listener.StatusLongClickListener
import info.justaway.model.Row
import info.justaway.model.TwitterManager
import info.justaway.util.MessageUtil
import twitter4j.Status
import java.lang.ref.WeakReference

/**
 * Created on 2018/08/29.
 */
class StatusActivity: FragmentActivity() {
    companion object {


        private class LoadTask(activity: StatusActivity) : AsyncTask<Long, Void, Status>() {
            val ref = WeakReference(activity)

            override fun doInBackground(vararg params: Long?): twitter4j.Status? {
                return params[0]?.let {
                    try {
                        TwitterManager.getTwitter().showStatus(it)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }

            }

            override fun onPostExecute(status: twitter4j.Status?) {
                ref.get()?.run {
                    dismissProgressDialog()
                    status?.let {
                        mAdapter.add(Row.newStatus(it))
                        mAdapter.notifyDataSetChanged()
                        val inReplyToStatusId = it.inReplyToStatusId
                        if (inReplyToStatusId > 0) {
                            LoadTask(this).execute(inReplyToStatusId)
                        }
                    } ?: MessageUtil.showToast(R.string.toast_load_data_failure)
                }
            }
        }
    }

    private var mProgressDialog: ProgressDialog? = null
    private lateinit var mAdapter: TwitterAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.requestFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)

        if (intent.getBooleanExtra("notification", false)) {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
        }
        val statusId: Long
        if (Intent.ACTION_VIEW == intent.action) {
            val uri = intent.data
            if (uri == null || uri.path == null) {
                return
            }
            when {
                uri.path.contains("photo") -> {
                    startActivity(Intent(this@StatusActivity, ScaleImageActivity::class.java).apply {
                        putExtra("url", uri.toString())
                    })
                    finish()
                    return
                }
                uri.path.contains("video") -> {
                    startActivity(Intent(this@StatusActivity, VideoActivity::class.java).apply {
                        putExtra("statusUrl", uri.toString())
                    })
                    finish()
                    return
                }
                else -> statusId = java.lang.Long.parseLong(uri.lastPathSegment)
            }
        } else {
            statusId = intent.getLongExtra("id", -1L)
        }

        setContentView(R.layout.activity_status)

        //TODO
        val listView = findViewById<ListView>(R.id.list)

        // コンテキストメニューを使える様にする為の指定、但しデフォルトではロングタップで開く
        registerForContextMenu(listView)

        // Status(ツイート)をViewに描写するアダプター
        mAdapter = TwitterAdapter(this, R.layout.row_tweet)
        with (listView) {
            adapter = mAdapter
            onItemClickListener = StatusClickListener(this@StatusActivity)
            onItemLongClickListener = StatusLongClickListener(this@StatusActivity)
        }
        if (statusId > 0) {
            showProgressDialog(getString(R.string.progress_loading))
            LoadTask(this).execute(statusId)
        } else {
            (intent.getSerializableExtra("status") as Status?)?.let {
                mAdapter.add(Row.newStatus(it))
                val inReplyToStatusId = it.inReplyToStatusId
                if (inReplyToStatusId > 0) {
                    showProgressDialog(getString(R.string.progress_loading))
                    LoadTask(this).execute(inReplyToStatusId)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        EventBus.getDefault().unregister(this)
        super.onPause()
    }

    private fun showProgressDialog(message: String) {
        mProgressDialog = ProgressDialog(this)
        mProgressDialog?.setMessage(message)
        mProgressDialog?.show()
    }

    private fun dismissProgressDialog() {
        mProgressDialog?.dismiss()
    }

    fun onEventMainThread(event: AlertDialogEvent) {
        event.dialogFragment.show(supportFragmentManager, "dialog")
    }

    fun onEventMainThread(event: StatusActionEvent) {
        mAdapter.notifyDataSetChanged()
    }

    fun onEventMainThread(event: StreamingDestroyStatusEvent) {
        mAdapter.removeStatus(event.statusId!!)
    }

}