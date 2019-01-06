package net.slashOmega.juktaway.fragment.main.tab

import android.util.Log
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.slashOmega.juktaway.model.*
import net.slashOmega.juktaway.settings.BasicSettings
import net.slashOmega.juktaway.twitter.currentClient

class FavoritesFragment: BaseFragment() {
    override var tabId = TabManager.FAVORITES_TAB_ID

    override fun taskExecute() {
        Log.d("favorite", "maxId = $mMaxId mReloading $mReloading")
        GlobalScope.launch(Dispatchers.Main) {
            val statuses = runCatching {
                currentClient.favorites.run {
                    if (mMaxId > 0 && !mReloading) list(maxId = mMaxId, count = BasicSettings.pageCount)
                    else list(count = BasicSettings.pageCount)
                }.await()
            }.getOrNull()

            when {
                statuses.isNullOrEmpty() -> {
                    mReloading = false
                    mPullToRefreshLayout.setRefreshComplete()
                    mListView.visibility = View.VISIBLE
                }
                mReloading -> {
                    clear()
                    for (status in statuses) {
                        FavRetweetManager.setFav(status.id)
                    }
                    mMaxId = statuses.last().id
                    mAdapter?.extensionAddAllFromStatusesSuspend(statuses)
                    mReloading = false
                    mPullToRefreshLayout.setRefreshComplete()
                }
                else -> {
                    for (status in statuses) {
                        FavRetweetManager.setFav(status.id)
                    }
                    mMaxId = statuses.last().id
                    mAdapter?.extensionAddAllFromStatusesSuspend(statuses)
                    mAutoLoader = true
                    mListView.visibility = View.VISIBLE
                }
            }

            finishLoad()
        }
    }
}