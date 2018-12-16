package net.slashOmega.juktaway.task

import android.content.Context

import net.slashOmega.juktaway.model.TwitterManager
import twitter4j.MediaEntity
import twitter4j.Status
import twitter4j.TwitterException

class PhotoLoader(context: Context, private val mStatusId: Long, private val mIndex: Int) : AbstractAsyncTaskLoader<String>(context) {

    override fun loadInBackground(): String? {
        return try {
            val status = TwitterManager.twitter.showStatus(mStatusId)
            val mediaEntities = status.mediaEntities
            if (mediaEntities.size < mIndex) {
                null
            } else mediaEntities[mIndex - 1].mediaURL
        } catch (e: TwitterException) {
            e.printStackTrace()
            null
        }

    }
}