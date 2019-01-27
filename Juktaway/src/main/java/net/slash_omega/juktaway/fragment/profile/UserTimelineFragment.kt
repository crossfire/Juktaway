package net.slash_omega.juktaway.fragment.profile

import android.view.View
import de.greenrobot.event.EventBus
import jp.nephy.penicillin.endpoints.timeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.adapter.StatusAdapter
import net.slash_omega.juktaway.event.action.StatusActionEvent
import net.slash_omega.juktaway.event.model.StreamingDestroyStatusEvent
import net.slash_omega.juktaway.listener.StatusClickListener
import net.slash_omega.juktaway.listener.StatusLongClickListener
import net.slash_omega.juktaway.settings.BasicSettings
import net.slash_omega.juktaway.twitter.currentClient
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout


/**
 * Created on 2018/11/18.
 */
internal class UserTimelineFragment: ProfileListFragmentBase() {
    override val mAdapter by lazy { StatusAdapter(activity!!) }
    override val layout = R.layout.pull_to_refresh_list
    private var mMaxId = 0L
    private var mReload = false
    private lateinit var mPullToRefreshLayout: PullToRefreshLayout

    override fun showList() {
        GlobalScope.launch(Dispatchers.Main) {
            val timeline = runCatching {
                currentClient.timeline.run {
                    if (mMaxId > 0) user(user.id, maxId = mMaxId, count = BasicSettings.pageCount, options = *arrayOf("tweet_mode" to "extended"))
                    else user(user.id, count = BasicSettings.pageCount, options = *arrayOf("tweet_mode" to "extended"))
                }.await()
            }.getOrNull()

            timeline?.takeIf { it.isNotEmpty() }?.run {
                if (mReload) {
                    mAdapter.clear()
                    lastOrNull { mMaxId == 0L || mMaxId > it.id }?.let { mMaxId = it.id }
                    mAdapter.addAllFromStatusesSuspend(this)
                    mReload = false
                    mPullToRefreshLayout.setRefreshComplete()
                } else {
                    lastOrNull { mMaxId == 0L || mMaxId > it.id }?.let { mMaxId = it.id }
                    mAdapter.extensionAddAllFromStatuses(this)
                    mAutoLoader = true
                    mListView.visibility = View.VISIBLE
                }
            }
            finishLoading()
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

    @Suppress("UNUSED_PARAMETER")
    fun onEventMainThread(event: StatusActionEvent) {
        mAdapter.notifyDataSetChanged()
    }

    fun onEventMainThread(event: StreamingDestroyStatusEvent) {
        GlobalScope.launch(Dispatchers.Main) { mAdapter.removeStatus(event.statusId!!) }
    }

    fun onRefreshStarted() {
        mReload = true
        mMaxId = 0
        showList()
    }

    override fun View.init() {
        mListView.onItemClickListener = StatusClickListener(activity!!)
        mListView.onItemLongClickListener = StatusLongClickListener(activity!!)
        mPullToRefreshLayout = findViewById(R.id.ptr_layout)
    }
}