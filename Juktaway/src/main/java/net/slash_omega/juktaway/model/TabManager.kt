package net.slash_omega.juktaway.model

import android.content.Context
import android.content.SharedPreferences
import jp.nephy.penicillin.models.TwitterList
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import net.slash_omega.juktaway.R
import net.slash_omega.juktaway.app
import net.slash_omega.juktaway.twitter.currentIdentifier
import java.util.ArrayList

const val HOME_TAB_ID = 0
const val MENTION_TAB_ID = 1
const val FAVORITE_TAB_ID = 2
const val DM_TAB_ID = 3
const val SEARCH_TAB_ID = 4
const val LIST_TAB_ID = 5
const val USER_TAB_ID = 6

object TabManager {
    private const val OLD_TIMELINE_TAB_ID = -1L
    private const val OLD_INTERACTIONS_TAB_ID = -2L
    private const val OLD_DIRECT_MESSAGES_TAB_ID = -3L
    private const val OLD_FAVORITES_TAB_ID = -4L
    private const val OLD_SEARCH_TAB_ID = -5L

    private const val TABS = "mTabs-"
    private var mTabs = mutableListOf<Tab>()
    private val keyName: String
        get() = TABS + currentIdentifier.userId.toString()
    private val preference: SharedPreferences
        get() = app.getSharedPreferences("settings", Context.MODE_PRIVATE)

    @UseExperimental(ImplicitReflectionSerializer::class)
    fun loadTabs(): MutableList<Tab> {
        mTabs.clear()
        mTabs.addAll(
                preference.getString("$keyName/v3", "")?.takeIf { it.isNotBlank() }?.let { json ->
                    println(json)
                    Json.parseList<Tab>(json)
                } ?: preference.getString("$keyName/v2", "")?.takeIf { it.isNotBlank() }?.let { oldJson ->
                    Json.parse<OldTabData>(oldJson).let { data ->
                        preference.edit().putString(keyName, "").apply()
                        data.tabs.removeAll { it.id == OLD_DIRECT_MESSAGES_TAB_ID }
                        translateTab(data.tabs)
                    }
                } ?: generalTabs
        )
        return mTabs
    }

    fun reinitialize(tabs: List<Tab>) {
        mTabs.clear()
        mTabs.addAll(tabs.filterNot { it.type == DM_TAB_ID })
        saveTabs()
    }

    fun addTab(tab: Tab) {
        mTabs.add(tab)
        saveTabs()
    }

    fun addSearchTab(searchWord: String) = addTab(Tab(SEARCH_TAB_ID, 0, searchWord, -1))

    @UseExperimental(ImplicitReflectionSerializer::class)
    private fun saveTabs() {
        preference.edit().putString("$keyName/v3", Json.stringify(mTabs)).apply()
    }

    private fun translateTab(list: List<OldTab>) = list.map {
        when {
            it.id == TabManager.OLD_TIMELINE_TAB_ID -> homeTab
            it.id == TabManager.OLD_INTERACTIONS_TAB_ID -> mentionTab
            it.id == TabManager.OLD_DIRECT_MESSAGES_TAB_ID -> favoriteTab
            it.id == TabManager.OLD_FAVORITES_TAB_ID -> dmTab
            it.id <= TabManager.OLD_SEARCH_TAB_ID ->Tab(SEARCH_TAB_ID, 0, it.name, -1)
            else -> Tab(LIST_TAB_ID, it.id, it.name, -1)
        }
    }

    private val generalTabs
        get() = listOf(homeTab, mentionTab, favoriteTab)

    fun isUserListRegistered(id: Long) = mTabs.any { it.type == LIST_TAB_ID && it.id == id }
}

@Serializable
private data class OldTabData(val tabs: ArrayList<OldTab>)

@Serializable
class OldTab(var id: Long, @Optional var name: String = "")

@Serializable
data class Tab(val type: Int, val id: Long, val word: String, val autoReload: Int) {
    override fun equals(other: Any?) = other is Tab && type == other.type && when (type) {
        SEARCH_TAB_ID -> word == other.word
        LIST_TAB_ID, USER_TAB_ID -> id == other.id
        else -> true
    }

    override fun hashCode() = type + 29 * word.hashCode() + id.hashCode()
}

val Tab.icon: Int
    get() = when (type) {
        HOME_TAB_ID -> R.string.fontello_home
        MENTION_TAB_ID -> R.string.fontello_at
        DM_TAB_ID -> R.string.fontello_mail
        FAVORITE_TAB_ID -> R.string.fontello_star
        SEARCH_TAB_ID -> R.string.fontello_search
        LIST_TAB_ID -> R.string.fontello_list
        else -> 0
    }

val Tab.displayString: String
    get() = when (type) {
        HOME_TAB_ID ->app.getString(R.string.title_main)
        MENTION_TAB_ID ->app.getString(R.string.title_interactions)
        DM_TAB_ID ->app.getString(R.string.title_direct_messages)
        FAVORITE_TAB_ID ->app.getString(R.string.title_favorites)
        SEARCH_TAB_ID ->app.getString(R.string.title_search) + ": " + word
        LIST_TAB_ID -> word
        USER_TAB_ID -> app.getString(R.string.title_user) + ": " + word
        else -> ""
    }

val homeTab: Tab
    get() = Tab(HOME_TAB_ID, 0, "", -1)

val mentionTab: Tab
    get() = Tab(MENTION_TAB_ID, 0, "", -1)

val favoriteTab: Tab
    get() = Tab(FAVORITE_TAB_ID, 0, "", -1)

val dmTab: Tab
    get() = Tab(DM_TAB_ID, 0, "", -1)

fun TwitterList.toTab() = Tab(LIST_TAB_ID, id, if (user.id == currentIdentifier.userId) name else fullName, -1)
