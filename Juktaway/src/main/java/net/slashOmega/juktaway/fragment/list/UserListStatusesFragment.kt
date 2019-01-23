package net.slashOmega.juktaway.fragment.list

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ListView
import android.widget.ProgressBar
import de.greenrobot.event.EventBus
import jp.nephy.penicillin.endpoints.lists
import kotlinx.android.synthetic.main.list_guruguru.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.R
import net.slashOmega.juktaway.adapter.StatusAdapter
import net.slashOmega.juktaway.event.model.StreamingDestroyStatusEvent
import net.slashOmega.juktaway.event.action.StatusActionEvent
import net.slashOmega.juktaway.listener.StatusClickListener
import net.slashOmega.juktaway.listener.StatusLongClickListener
import net.slashOmega.juktaway.settings.BasicSettings
import net.slashOmega.juktaway.twitter.currentClient

class UserListStatusesFragment : Fragment() {
    private lateinit var mAdapter: StatusAdapter
    private lateinit var mListView: ListView
    private var mListId: Long = 0
    private lateinit var mFooter: ProgressBar
    private var mAutoLoader = false
    private var mMaxId = 0L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(R.layout.list_guruguru, container, false)?.apply {
        arguments?.getLong("listId")?.let { mListId = it }
        mAdapter = StatusAdapter(activity!!)

        // リストビューの設定
        mListView = list_view.apply {
            visibility = View.GONE
            adapter = mAdapter
            onItemClickListener = StatusClickListener(activity!!)
            onItemLongClickListener = StatusLongClickListener(activity!!)
            setOnScrollListener(object : AbsListView.OnScrollListener {

                override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {}

                override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                    // 最後までスクロールされたかどうかの判定
                    if (totalItemCount == firstVisibleItem + visibleItemCount) { additionalReading() }
                }
            })
        }

        mFooter = guruguru

        applyUserList()
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
    fun onEventMainThread(event: StatusActionEvent) { mAdapter.notifyDataSetChanged() }

    fun onEventMainThread(event: StreamingDestroyStatusEvent) { GlobalScope.launch(Dispatchers.Main) { mAdapter.removeStatus(event.statusId!!) } }

    private fun additionalReading() {
        if (!mAutoLoader) return
        mFooter.visibility = View.VISIBLE
        mAutoLoader = false
        applyUserList()
    }

    private fun applyUserList() {
        GlobalScope.launch(Dispatchers.Main) {
            val statuses = runCatching {
                (if (mMaxId > 0) currentClient.lists.timeline(mListId, maxId = mMaxId - 1, count = BasicSettings.pageCount)
                else currentClient.lists.timeline(mListId)).await()
            }.getOrNull()
            mFooter.visibility = View.GONE

            if (statuses.isNullOrEmpty()) return@launch
            for (status in statuses) {
                if (mMaxId == 0L || mMaxId > status.id) {
                    mMaxId = status.id
                }
            }
            mAdapter.addAllFromStatuses(statuses)
            mAutoLoader = true
            mListView.visibility = View.VISIBLE
        }
    }
}
